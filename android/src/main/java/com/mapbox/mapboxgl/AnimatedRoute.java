package com.mapbox.mapboxgl;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.graphics.Color;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.turf.TurfMeasurement;
import com.mapbox.turf.TurfMisc;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineCap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineJoin;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth;

public class AnimatedRoute {

    private static final String LINE_SOURCE_ID = "line-source-id";
    private static final String TARGET_LINE_SOURCE_ID = "target-line-source-id";
    private static final String TARGET_LINE_LAYER_ID = "target-line-layer-id";

    private MapboxMap mapInstance;
    private Style style;
    private AnimatedMarker animatedMarker;

    private GeoJsonSource lineSource;
    private LineString route;
    private List<Point> routeCoordinateList;

    private GeoJsonSource targetLineSource;
    private LineString targetRoute;
    private List<Point> targetRouteCoordinateList;

    private float bearingPrev = 0;
    boolean isRotating = false;

    private int routeIndex;

    public AnimatedRoute(MapboxMap mapView, AnimatedMarker marker) {
        mapInstance = mapView;
        style = Objects.requireNonNull(mapInstance.getStyle());
        animatedMarker = marker;

        createSources();
        createTargetLine();
    }

    private void createSources() {
        style.addSource(lineSource = new GeoJsonSource(LINE_SOURCE_ID));
        style.addSource(targetLineSource = new GeoJsonSource(TARGET_LINE_SOURCE_ID));
    }

    private void createTargetLine() {
        style.addLayerBelow(new LineLayer(TARGET_LINE_LAYER_ID, TARGET_LINE_SOURCE_ID).withProperties(
                lineColor(Color.parseColor("#000000")),
                lineCap(Property.LINE_CAP_ROUND),
                lineJoin(Property.LINE_JOIN_ROUND),
                lineWidth(2.5f)), "road-label");
    }

    public void update(AnimatedRouteConfiguration conf) {
        route = LineString.fromPolyline(conf.line, 6);
        targetRoute = LineString.fromPolyline(conf.targetLine, 6);

        routeCoordinateList = route.coordinates();
        final LineString slicedSource = animatedMarker.currentLocation().equals(routeCoordinateList.get(routeCoordinateList.size() - 1)) ?
                route :
                TurfMisc.lineSlice(animatedMarker.currentLocation(), routeCoordinateList.get(routeCoordinateList.size() - 1), route);

        route = slicedSource;
        routeCoordinateList = slicedSource.coordinates();

        if (!animatedMarker.currentLocation().equals(routeCoordinateList.get(0))) {
            routeCoordinateList.add(0, animatedMarker.currentLocation());
        }

        targetRouteCoordinateList = targetRoute.coordinates();
        routeIndex = 0;

        animate();

        lineSource.setGeoJson(route);
        targetLineSource.setGeoJson(targetRoute);
    }

    private void animate() {
        if (routeCoordinateList.get(0).equals(routeCoordinateList.get(routeCoordinateList.size() - 1))) {
            final List<Point> list = new ArrayList<Point>();
            list.add(routeCoordinateList.get(0));
            list.add(routeCoordinateList.get(routeCoordinateList.size() - 1));

            routeCoordinateList = list;
        }

        // Check if we are at the end of the points list
        if ((routeCoordinateList.size() - 1 > routeIndex)) {
            final Point currentPosition = animatedMarker.currentLocation();
            final Point targetPosition = routeCoordinateList.get(routeIndex + 1);

            final long size = (long) TurfMeasurement.distance(routeCoordinateList.get(0), routeCoordinateList.get(routeCoordinateList.size() - 1), "meters");

            if (size > 1000) {
                animatedMarker.updateCoordinates(routeCoordinateList.get(routeCoordinateList.size() - 1), 300);
                return;
            }

            /*targetLineSource.setGeoJson(
                    Feature.fromGeometry(
                            TurfMisc.lineSlice(targetPosition, targetRouteCoordinateList.get(targetRouteCoordinateList.size() - 1), targetRoute)
                    )
            );*/

            long speed;

            if (size > 300) {
                speed = 10;
            } else if (size > 150) {
                speed = 20;
            } else if (size > 100) {
                speed = 35;
            } else if (size > 55) {
                speed = 105;
            } else if (size > 30) {
                speed = 135;
            } else {
                speed = 160;
            }

            long meters = (long) TurfMeasurement.distance(routeCoordinateList.get(routeIndex), targetPosition, "meters");
            long duration = speed * meters;

            // float bearing = (float) TurfMeasurement.bearing(currentPosition, targetPosition) % 360;
            // float bearing = (float) TurfMeasurement.bearing(currentPosition, targetPosition);
            /*if (bearing < 0) {
                bearing += 360;
            }*/

            float bearing = (float) getBearing(currentPosition, targetPosition);

            animatedMarker.updateCoordinates(targetPosition, duration, new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);

                    animate();
                }
            });

            animatedMarker.updateRotation(bearing, 200);

            routeIndex++;
        }
    }

    private double getBearing(Point currentPosition, Point targetPosition) {
        float bearing = (float) TurfMeasurement.bearing(currentPosition, targetPosition) % 360;
        if (bearing < 0) {
            bearing += 360;
        }

        float currentValue = animatedMarker.prevBearing;

        float nextBearing = bearing;

        if (Math.abs(bearing - animatedMarker.prevBearing) > 150) {
            if (animatedMarker.prevBearing >= 180 && bearing < 180) {
                currentValue = -(360 - animatedMarker.prevBearing);
            }

            if (animatedMarker.prevBearing <= 180 && bearing > 180) {
                nextBearing = -(360 - bearing);
            }
        }

        final float pBearing = currentValue;
        final float nBearing = nextBearing;

        animatedMarker.prevBearing = pBearing;

        return nBearing;
    }

    public void destroy() {
        style.removeLayer(TARGET_LINE_LAYER_ID);
        style.removeSource(LINE_SOURCE_ID);
        style.removeSource(TARGET_LINE_SOURCE_ID);
    }

}
