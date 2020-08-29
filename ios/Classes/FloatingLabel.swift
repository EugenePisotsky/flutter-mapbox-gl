//
//  FloatingLabel.swift
//  mapbox_gl
//
//  Created by Eugene Pisotsky on 20.10.2019.
//

import Foundation
import Mapbox

class FloatingLabel {
    var ID: String
    var SOURCE_ID: String
    var LAYER_ID: String
    var IMAGE_ID: String
    
    var mapInstance: MGLMapView
    var id: String
    var image: UIImage
    var location: CLLocationCoordinate2D
    var width: Float
    var height: Float
    var icon: String?
    var style: MGLStyle?
   
    var anchorX: Float = 1
    var anchorY: Float = 1
    
    var translateY: Double
    var translateX: Double
    
    var pointSource: MGLShapeSource
    var labelSource: MGLShapeSource
    
    var pointLayer: MGLSymbolStyleLayer
    var labelLayer: MGLSymbolStyleLayer
    
    init(
        mapInstance: MGLMapView,
        id: String,
        image: UIImage,
        location: CLLocationCoordinate2D,
        width: Float,
        height: Float,
        icon: String?
    ) {
        self.mapInstance = mapInstance
        self.id = id
        self.image = image
        self.location = location
        self.width = width
        self.height = height
        self.icon = icon
        
        style = mapInstance.style
        
        ID = id
        SOURCE_ID = id + "-source"
        LAYER_ID = id + "-layer"
        IMAGE_ID = id + "-image"
        
        let point = MGLPointFeature()
        point.identifier = id
        point.coordinate = location
        
        pointSource = MGLShapeSource(identifier: "point-" + SOURCE_ID, shape: point, options: nil)
        labelSource = MGLShapeSource(identifier: SOURCE_ID, shape: point, options: nil)
        pointLayer = MGLSymbolStyleLayer(identifier: "point-" + LAYER_ID, source: pointSource)
        labelLayer = MGLSymbolStyleLayer(identifier: LAYER_ID, source: labelSource)
        
        style?.addSource(pointSource)
        style?.addSource(labelSource)
        style?.setImage(image, forName: IMAGE_ID)
        
        translateX = Double((width - 36) / 2);
        translateY = Double((height / 2) + 4);
        
        pointLayer.iconAnchor = NSExpression(forConstantValue: "center")
        pointLayer.iconImageName = NSExpression(forConstantValue: icon)
        pointLayer.iconAllowsOverlap = NSExpression(forConstantValue: true)
        
        labelLayer.iconImageName = NSExpression(forConstantValue: IMAGE_ID)
        labelLayer.iconTranslation = NSExpression(forConstantValue: NSValue(cgVector: CGVector(dx: translateX, dy: translateY)))
        labelLayer.iconAllowsOverlap = NSExpression(forConstantValue: true)
        
        style?.addLayer(pointLayer)
        style?.addLayer(labelLayer)
    }
    
    func getId() -> String {
        return self.id
    }
    
    func getLayerId() -> String {
        return LAYER_ID
    }
    
    func destroy() {
        style?.removeLayer(pointLayer)
        style?.removeLayer(labelLayer)
        style?.removeImage(forName: IMAGE_ID)
        style?.removeSource(pointSource)
        style?.removeSource(labelSource)
    }
    
    func hide() {
        pointLayer.isVisible = false
        labelLayer.isVisible = false
    }
    
    func show() {
        pointLayer.isVisible = true
        labelLayer.isVisible = true
    }
    
    func updatePosition() {
        let screenSize: CGRect = UIScreen.main.bounds
        let windowWidth = screenSize.size.width
        let windowHeight = screenSize.size.height - CGFloat(mapInstance.contentInset.bottom)

        let centerScreenPoint: CGPoint = self.mapInstance.convert(location, toPointTo: nil)

        let x = (centerScreenPoint.x + CGFloat(width))
        let y = (centerScreenPoint.y - CGFloat(height))
        
        let x2 = (centerScreenPoint.x - CGFloat(width))
        let y2 = (centerScreenPoint.y + CGFloat(height)) - CGFloat(mapInstance.contentInset.top)

        if (x > windowWidth) {
            translateX = Double(-((width - 36) / 2))
            anchorX = -1
        } else if (x2 < 0) {
            translateX = Double((width - 36) / 2)
            anchorX = 1
        }
        
        if (y > windowHeight / 2) {
            translateY = Double(-((height / 2) - 12))
            anchorY = -1
        } else if (y2 < windowHeight / 2) {
            translateY = Double((height / 2) + 4)
            anchorY = 1
        }
        
        labelLayer.iconTranslation = NSExpression(forConstantValue: NSValue(cgVector: CGVector(dx: translateX, dy: translateY)))
    }
    
    func updateSourceCoordinates(coords: CLLocationCoordinate2D) {
        self.location = coords
        let point = MGLPointFeature()
        point.identifier = self.ID
        point.coordinate = location
        
        pointSource.shape = point
        labelSource.shape = point
    }
    
    func updateIcon(icon: String) {
        pointLayer.iconImageName = NSExpression(forConstantValue: icon)
    }
    
    func updateLabel(
        width: Float,
        height: Float,
        image: UIImage
    ) {
        self.width = width
        self.height = height
        
        style?.removeImage(forName: IMAGE_ID)
        style?.setImage(image, forName: IMAGE_ID)
        
        updatePosition()
    }
    
    func getAnchor() -> [Float] {
        return [anchorX, anchorY]
    }
    
}
