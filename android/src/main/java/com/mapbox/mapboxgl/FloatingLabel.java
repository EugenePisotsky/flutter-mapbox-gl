package com.mapbox.mapboxgl;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.view.Display;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAnchor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconSize;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconTranslate;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.visibility;

public class FloatingLabel {

    private Style style;

    private float anchorX = 1f;
    private float anchorY = 1f;

    private float translateX;
    private float translateY;

    private float prevTranslateX;
    private float prevTranslateY;

    private String ID;
    private String SOURCE_ID;
    private String LAYER_ID;
    private String IMAGE_ID;

    private LatLng location;
    private Activity activity;
    private MapView mapboxMapView;
    private MapboxMap mapboxMap;
    private float density;
    private float width;
    private float height;
    private String icon;

    private GeoJsonSource pointSource;
    private GeoJsonSource labelSource;

    private LatLng prevCameraPosition;

    private MapConfiguration mapConfiguration;

    public FloatingLabel(
            MapView mapView,
            MapboxMap mapboxMap,
            String id,
            Bitmap image,
            LatLng location,
            float width,
            float height,
            String icon,
            float density,
            Activity activity,
            MapConfiguration mapConfiguration
    ) {
        this.mapboxMapView = mapView;
        this.width = width;
        this.height = height;
        this.location = location;
        this.activity = activity;
        this.mapboxMap = mapboxMap;
        this.density = density;
        this.icon = icon;
        this.mapConfiguration = mapConfiguration;

        ID = id;
        SOURCE_ID = id + "-source";
        LAYER_ID = id + "-layer";
        IMAGE_ID = id + "-image";

        style = mapboxMap.getStyle();

        pointSource = new GeoJsonSource(
                "point-" + SOURCE_ID,
                Feature.fromGeometry(Point.fromLngLat(location.getLongitude(), location.getLatitude()))
        );

        labelSource = new GeoJsonSource(
                SOURCE_ID,
                Feature.fromGeometry(Point.fromLngLat(location.getLongitude(), location.getLatitude()))
        );

        style.addSource(pointSource);
        style.addSource(labelSource);
        style.addImage(IMAGE_ID, image);

        translateX = (width - 36) / 2;
        translateY = (height / 2) + 4;

        style.addLayer(new SymbolLayer(LAYER_ID, SOURCE_ID)
                .withProperties(
                        PropertyFactory.iconImage(IMAGE_ID),
                        iconAllowOverlap(true),
                        iconAnchor(Property.ICON_ANCHOR_CENTER),
                        iconTranslate(new Float[]{translateX, translateY})
                )
        );

        style.addLayer(new SymbolLayer("point-" + LAYER_ID, "point-" + SOURCE_ID)
                .withProperties(
                        PropertyFactory.iconImage(icon),
                        iconAllowOverlap(true),
                        iconSize(1.2f),
                        iconAnchor(Property.ICON_ANCHOR_CENTER)
                )
        );
    }

    public void updatePosition() {
        activity.runOnUiThread(new Runnable() {
            public void run() {
                final LatLng target = mapboxMap.getCameraPosition().target;

                if (style.isFullyLoaded() && !target.equals(prevCameraPosition)) {
                    prevCameraPosition = target;

                    Display display = activity.getWindowManager().getDefaultDisplay();
                    android.graphics.Point size = new android.graphics.Point();
                    display.getSize(size);
                    int windowWidth = size.x;
                    int windowHeight = size.y - (int) mapConfiguration.bottomPadding;

                    final PointF pointInWindow = mapboxMap.getProjection().toScreenLocation(location);
                    final float x = (pointInWindow.x + (width * density));
                    final float y = (pointInWindow.y - (height * density));

                    final float x2 = (pointInWindow.x - (width * density));
                    final float y2 = (pointInWindow.y + (height * density)) - (int) mapConfiguration.topPadding;

                    if (x > windowWidth) {
                        translateX = -((width - 36) / 2);
                        anchorX = -1;
                    } else if (x2 < 0) {
                        translateX = (width - 36) / 2;
                        anchorX = 1;
                    }

                    if (y > windowHeight / 2) {
                        translateY = -((height / 2) - 12);
                        anchorY = -1;
                    } else if (y2 < windowHeight / 2) {
                        translateY = (height / 2) + 4;
                        anchorY = 1;
                    }

                    if (translateX != prevTranslateX || translateY != prevTranslateY) {
                        prevTranslateX = translateX;
                        prevTranslateY = translateY;

                        style.getLayerAs(LAYER_ID).setProperties(
                                iconTranslate(new Float[]{translateX, translateY})
                        );
                    }
                }
            }
        });
    }

    public void destroy() {
        style.removeLayer(LAYER_ID);
        style.removeLayer("point-" + LAYER_ID);
        style.removeImage(IMAGE_ID);
        style.removeSource(SOURCE_ID);
        style.removeSource("point-" + SOURCE_ID);
    }

    public String getLayerId() {
        return LAYER_ID;
    }

    public String getId() {
        return ID;
    }

    public void updateSourceCoordinates(LatLng location) {
        this.location = location;

        final Feature point = Feature.fromGeometry(Point.fromLngLat(location.getLongitude(), location.getLatitude()));

        pointSource.setGeoJson(point);
        labelSource.setGeoJson(point);
    }

    public void updateLabel(float width, float height, Bitmap image) {
        this.width = width;
        this.height = height;

        style.removeImage(IMAGE_ID);
        style.addImage(IMAGE_ID, image);

        updatePosition();
    }

    public void updateIcon(String icon) {
        // @todo
    }

    public void hide() {
        style.getLayer(LAYER_ID).setProperties(visibility(Property.NONE));
        style.getLayer("point-" + LAYER_ID).setProperties(visibility(Property.NONE));
    }

    public void show() {
        style.getLayer(LAYER_ID).setProperties(visibility(Property.VISIBLE));
        style.getLayer("point-" + LAYER_ID).setProperties(visibility(Property.VISIBLE));
    }

    public PointF getAnchor() {
        return new PointF(anchorX, anchorY);
    }

}