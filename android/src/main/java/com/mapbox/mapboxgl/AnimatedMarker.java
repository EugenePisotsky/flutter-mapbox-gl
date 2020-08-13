package com.mapbox.mapboxgl;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.util.Objects;
import java.util.UUID;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconOffset;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconRotate;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconSize;

public class AnimatedMarker {

    private MapboxMap mapInstance;
    private Point animatingMarker;

    private GeoJsonSource pointSource;
    public String identifier;
    private String imageName;

    public Animator currentAnimator;
    private ValueAnimator bearingValueAnimator;

    private Handler handler;
    private Runnable runnable;

    private Handler rHandler;
    private Runnable rRunnable;

    public float prevBearing;

    private Style style;

    public AnimatedMarker(MapboxMap mapView) {
        mapInstance = mapView;
        identifier = UUID.randomUUID().toString();
        style = Objects.requireNonNull(mapInstance.getStyle());
    }

    public void create() {
        style.addSource(pointSource = new GeoJsonSource(identifier));
        style.addLayer(new SymbolLayer(identifier + "-layer", identifier).withProperties(
                iconImage(imageName),
                iconSize(1f),
                iconOffset(new Float[] {0f, 0f}),
                iconIgnorePlacement(true),
                iconAllowOverlap(true)
        ));
    }

    public void updateIconImage(String name) {
        imageName = name;

        final Layer animatedMarker = Objects.requireNonNull(mapInstance.getStyle()).getLayerAs(identifier + "-layer");

        if (animatedMarker != null) {
            animatedMarker.setProperties(
                    iconImage(imageName)
            );
        }
    }

    public void updateCoordinates(Point coordinates, long duration) {
        updateCoordinates(coordinates, duration, new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
            }
        });
    }

    public void updateCoordinates(Point coordinates, long duration, AnimatorListenerAdapter callback) {
        if (currentAnimator != null) {
            currentAnimator.removeAllListeners();
            currentAnimator.cancel();
        }

        if (animatingMarker == null) {
            animatingMarker = coordinates;
            pointSource.setGeoJson(animatingMarker);
        } else {
            handler = new Handler();
            runnable = new Runnable() {
                @Override
                public void run() {
                    currentAnimator = createLatLngAnimator(animatingMarker, coordinates, duration);
                    currentAnimator.addListener(callback);
                    currentAnimator.start();
                }
            };
            handler.post(runnable);
        }
    }

    public void updateRotation(Float rotation, long duration) {
        rHandler = new Handler();
        rRunnable = new Runnable() {
            @Override
            public void run() {
                final Layer animatedMarker = style.getLayerAs(identifier + "-layer");

                if (animatedMarker == null) {
                    return;
                }

                if (bearingValueAnimator != null) {
                    bearingValueAnimator.removeAllUpdateListeners();
                    bearingValueAnimator.cancel();
                }

                bearingValueAnimator = ValueAnimator.ofFloat(1);
                bearingValueAnimator.setDuration(duration);
                bearingValueAnimator.setInterpolator(new LinearInterpolator());
                bearingValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        try {
                            final float b = (rotation - prevBearing) * animation.getAnimatedFraction() + prevBearing;

                            animatedMarker.setProperties(
                                    iconRotate(b)
                                    // iconRotate((float) animation.getAnimatedValue())
                                    // iconRotate((currentStartRotation + animation.getAnimatedFraction() * nextRotation) % 360)
                            );

                            prevBearing = b;
                        } catch (Exception ex) {
                            //I don't care atm..
                        }
                    }
                });
                bearingValueAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                    }
                });
                bearingValueAnimator.start();
            }
        };
        rHandler.post(rRunnable);
    }

    public Point currentLocation() {
        return animatingMarker;
    }

    public void destroy() {
        if (currentAnimator != null) {
            currentAnimator.removeAllListeners();
            currentAnimator.cancel();
        }

        style.removeLayer(identifier + "-layer");
        style.removeSource(identifier);
    }

    private static class PointEvaluator implements TypeEvaluator<Point> {
        @Override
        public Point evaluate(float fraction, Point startValue, Point endValue) {
            return Point.fromLngLat(
                    startValue.longitude() + ((endValue.longitude() - startValue.longitude()) * fraction),
                    startValue.latitude() + ((endValue.latitude() - startValue.latitude()) * fraction)
            );
        }
    }

    private Animator createLatLngAnimator(Point currentPosition, Point targetPosition, long duration) {
        ValueAnimator latLngAnimator = ValueAnimator.ofFloat(0, 1);
        latLngAnimator.setDuration(duration);
        latLngAnimator.setInterpolator(new LinearInterpolator());
        /*latLngAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                // animate();
            }
        });*/

        latLngAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float fraction = (float) animation.getAnimatedValue();

                double lat = ((targetPosition.latitude() - currentPosition.latitude()) * fraction) + currentPosition.latitude();
                double lng = ((targetPosition.longitude() - currentPosition.longitude()) * fraction) + currentPosition.longitude();
                Point point = Point.fromLngLat(lng, lat);

                pointSource.setGeoJson(point);
                animatingMarker = point;
            }
        });

        return latLngAnimator;
    }

}
