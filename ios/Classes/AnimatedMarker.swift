//
//  AnimatedMarker.swift
//  mapbox_gl
//
//  Created by Eugene Pisotsky on 11.08.2020.
//

import Foundation
import Mapbox
import Turf

extension CLLocationCoordinate2D {}

public func ==(lhs: CLLocationCoordinate2D, rhs: CLLocationCoordinate2D) -> Bool {
    return (lhs.latitude == rhs.latitude && lhs.longitude == rhs.longitude)
}

class AnimatedAnnotationView: MGLAnnotationView {
    override func layoutSubviews() {
        super.layoutSubviews()
        
        scalesWithViewingDistance = false
        
        /*layer.cornerRadius = frame.width / 2
        layer.borderWidth = 2
        layer.borderColor = UIColor.white.cgColor*/
    }
}

class MGLAnimatedPointAnnotation: MGLPointAnnotation {
    var mapId: String?
    var image: UIImageView?
}

func randomString(length: Int) -> String {
  let letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
  return String((0..<length).map{ _ in letters.randomElement()! })
}

class AnimatedMarker {
    
    var mapInstance: MGLMapView
    var annotation: MGLAnimatedPointAnnotation?
    var animationHeading: UIViewPropertyAnimator?
    var animationCoord: UIViewPropertyAnimator?
    var identifier: String
    var imageName: String?
    var image: UIImageView?
    
    init(mapInstance: MGLMapView) {
        self.identifier = randomString(length: 30)
        self.mapInstance = mapInstance
    }
    
    func create(coords: CLLocationCoordinate2D) {
        annotation = MGLAnimatedPointAnnotation()
        annotation?.coordinate = coords
        annotation?.mapId = identifier
    }
    
    func addToMap() {
        mapInstance.addAnnotation(annotation!)
    }
    
    func updateIconImage(name: String) {
        if (name != self.imageName) {
            self.imageName = name
            self.image = UIImageView(image: mapInstance.style?.image(forName: name))
            self.image?.frame = CGRect(x: 0, y: 0, width: 40, height: 40)
            self.image?.contentMode = .scaleAspectFit
            self.annotation?.image = image
        }
    }
    
    func updateCoordinates(coords: CLLocationCoordinate2D, duration: Double) {
        if (coords != self.annotation?.coordinate) {
            animationCoord?.stopAnimation(false)
            animationCoord?.finishAnimation(at: .current)
            
            animationCoord = UIViewPropertyAnimator(duration: duration, curve: .linear) {
                self.annotation!.coordinate = coords
            }
            
            animationCoord?.startAnimation()
        }
    }
    
    func updateRotation(rotation: Double, duration: Double) {
        animationHeading?.stopAnimation(false)
        animationHeading?.finishAnimation(at: .current)
        
        animationHeading = UIViewPropertyAnimator(duration: duration, curve: .easeOut){
               self.image?.transform = CGAffineTransform.identity.rotated(by: CGFloat(rotation) / 180.0 * .pi)
        }
        
        animationHeading?.startAnimation()
    }
    
    func destroy() {
        mapInstance.removeAnnotation(annotation!)
    }
    
}
