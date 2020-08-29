part of mapbox_gl_platform_interface;

class MapViewportSettings {
  double bottomPadding;
  double topPadding;

  MapViewportSettings({ this.bottomPadding, this.topPadding });

  copyWith({ double bottomPadding, double topPadding }) {
    return MapViewportSettings(
        bottomPadding: bottomPadding ?? this.bottomPadding,
        topPadding: topPadding ?? this.topPadding
    );
  }

  dynamic toJson() {
    return <String, dynamic>{
      'bottomPadding': this.bottomPadding,
      'topPadding': this.topPadding,
    };
  }
}