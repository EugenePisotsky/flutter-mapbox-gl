//
//  AnimatedRoute.swift
//  mapbox_gl
//
//  Created by Eugene Pisotsky on 12.08.2020.
//

import Foundation
import Mapbox
import Turf

class AnimatedRouteConfiguration {
    var line: String
    var targetLine: String
    
    init(line: String, targetLine: String) {
        self.line = line
        self.targetLine = targetLine
    }
}

class AnimatedRoute {
    
    var LINE_SOURCE_ID = "line-source-id"
    var TARGET_LINE_SOURCE_ID = "target-line-source-id"
    var TARGET_LINE_LAYER_ID = "target-line-layer-id"
    
    var routeIndex: Int = 0
    
    var route: MGLPolyline?
    var targetRoute: MGLPolyline?
    var lineSource: MGLShapeSource?
    var targetLineSource: MGLShapeSource?
    var routeCoordinates: [CLLocationCoordinate2D]?
    var targetRouteCoordinates: [CLLocationCoordinate2D]?
    var targetLineLayer: MGLLineStyleLayer?
    
    var mapInstance: MGLMapView
    var animatedMarker: AnimatedMarker
    
    init(mapInstance: MGLMapView, marker: AnimatedMarker) {
        self.mapInstance = mapInstance
        self.animatedMarker = marker
        
        createSources()
        createTargetLine()
    }
    
    func createSources() {
        targetLineSource = MGLShapeSource(identifier: TARGET_LINE_SOURCE_ID, shape: targetRoute)
        lineSource = MGLShapeSource(identifier: LINE_SOURCE_ID, shape: route)
        mapInstance.style?.addSource(targetLineSource!)
        mapInstance.style?.addSource(lineSource!)
    }

    func createTargetLine() {
        let layer = MGLLineStyleLayer(identifier: TARGET_LINE_LAYER_ID, source: targetLineSource!)
        
        targetLineLayer = layer
        
        layer.lineColor = NSExpression(forConstantValue: UIColor.black)
        layer.lineCap = NSExpression(forConstantValue: "round")
        layer.lineJoin = NSExpression(forConstantValue: "round")
        layer.lineWidth = NSExpression(forConstantValue: 2.5)
        
        mapInstance.style?.addLayer(layer)
    }
    
    func update(conf: AnimatedRouteConfiguration) {
        let polyline = Polyline(encodedPolyline: conf.line, precision: 1e6)
        guard let decodedCoordinates: [CLLocationCoordinate2D] = polyline.coordinates else { return }
        route = MGLPolyline(coordinates: decodedCoordinates, count: UInt(decodedCoordinates.count))
        
        let targetPolyline = Polyline(encodedPolyline: conf.targetLine, precision: 1e6)
        guard let targetDecodedCoordinates: [CLLocationCoordinate2D] = targetPolyline.coordinates else { return }
        
        routeCoordinates = decodedCoordinates
        targetRouteCoordinates = targetDecodedCoordinates
        
        let currentCoord = animatedMarker.currentLocation()
        animatedMarker.annotation?.coordinate = currentCoord!
        
        if (currentCoord != routeCoordinates?.last) {
            let slicedRoute = LineString(routeCoordinates!).sliced(from: currentCoord, to: routeCoordinates?.last)
            
            let coords: [CLLocationCoordinate2D] = slicedRoute?.coordinates ?? []
            
            route = MGLPolyline(coordinates: coords, count: UInt(coords.count))
            routeCoordinates = coords
        }
        
        routeIndex = 0
        animate()
        
        lineSource?.shape = MGLPolylineFeature(coordinates: routeCoordinates!, count: UInt(routeCoordinates!.count))
        targetLineSource?.shape = MGLPolylineFeature(coordinates: targetRouteCoordinates!, count: UInt(targetRouteCoordinates!.count))
    }
    
    func animate() {
        if (routeCoordinates?.first == routeCoordinates?.last) {
            let newList: [CLLocationCoordinate2D]? = [
                (routeCoordinates?.first)!,
                (routeCoordinates?.last)!
            ]

            routeCoordinates = newList
        }
        
        guard let list = self.routeCoordinates else { return }
        
        if (list.count <= self.routeIndex + 1) {
            return
        }

        let size = list.first!.distance(to: list.last!)
        let sizeForStep = list[self.routeIndex].distance(to: list[self.routeIndex + 1])
        var speed: Double
        
        if (size > 1000) {
            guard let lastCoord = routeCoordinates?.last else { return }
            animatedMarker.updateCoordinates(coords: lastCoord, duration: 0.3)
            
            return
        }
        
        if (size > 300) {
            speed = 0.01
        } else if (size > 150) {
            speed = 0.02
        } else if (size > 100) {
            speed = 0.035
        } else if (size > 55) {
            speed = 0.105
        } else if (size > 30) {
            speed = 0.135
        } else {
            speed = 0.160
        }
        
        speed = speed * sizeForStep
        
        if ((routeCoordinates?.count ?? 0) - 1 > routeIndex) {
            let prevCoords = list[self.routeIndex]
            let coords = list[self.routeIndex + 1]
            
            if (prevCoords != coords) {
                let bearing = getBearingBetweenTwoPoints1(point1: prevCoords, point2: coords)
                animatedMarker.updateRotation(rotation: bearing, duration: 2.0)
            }
            
            animatedMarker.updateCoordinates(coords: coords, duration: speed)
            self.routeIndex += 1
            
            animatedMarker.animationCoord?.addCompletion { position in
                if position == .end {
                    self.animate()
                }
            }
        }
    }
    
    func getBearingBetweenTwoPoints1(point1 : CLLocationCoordinate2D, point2 : CLLocationCoordinate2D) -> Double {
        let lat1 = degreesToRadians(degrees: point1.latitude)
        let lon1 = degreesToRadians(degrees: point1.longitude)

        let lat2 = degreesToRadians(degrees: point2.latitude)
        let lon2 = degreesToRadians(degrees: point2.longitude)

        let dLon = lon2 - lon1

        let y = sin(dLon) * cos(lat2)
        let x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        let radiansBearing = atan2(y, x)

        return radiansToDegrees(radians: radiansBearing)
    }

    
    func degreesToRadians(degrees: Double) -> Double { return degrees * .pi / 180.0 }
    func radiansToDegrees(radians: Double) -> Double { return radians * 180.0 / .pi }
    
    func destroy() {
        mapInstance.style?.removeLayer(targetLineLayer!)
    }
    
}
