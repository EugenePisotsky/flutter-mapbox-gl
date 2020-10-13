package com.mapbox.mapboxgl;

public class MapConfiguration {

    double topPadding = 0;
    double bottomPadding = 0;
    double leftPadding = 0;
    double rightPadding = 0;

    public void setPadding(double top, double bottom, double left, double right) {
        topPadding = top;
        bottomPadding = bottom;
        leftPadding = left;
        rightPadding = right;
    }

}
