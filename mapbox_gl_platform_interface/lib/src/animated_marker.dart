part of mapbox_gl_platform_interface;

class AnimatedMarker {
  AnimatedMarker(this._id, this.options, [this._data]);

  /// A unique identifier for this symbol.
  ///
  /// The identifier is an arbitrary unique string.
  final String _id;

  String get id => _id;

  final Map _data;
  Map get data => _data;

  AnimatedMarkerOptions options;
}

class AnimatedGeometry {
  final LatLng geometry;
  final Duration duration;

  AnimatedGeometry({ this.geometry, this.duration });

  dynamic toJson() {
    return <double>[geometry.latitude, geometry.longitude, duration.inMilliseconds.toDouble()];
  }
}

class AnimatedRotation {
  final double rotation;
  final Duration duration;

  AnimatedRotation({ this.rotation, this.duration });

  dynamic toJson() {
    return <double>[rotation, duration.inMilliseconds.toDouble()];
  }
}

class AnimatedMarkerOptions {
  const AnimatedMarkerOptions({
    this.iconImage,
    this.geometry,
    this.rotation
  });

  final String iconImage;
  final AnimatedRotation rotation;
  final AnimatedGeometry geometry;

  static const AnimatedMarkerOptions defaultOptions = AnimatedMarkerOptions();

  AnimatedMarkerOptions copyWith(AnimatedMarkerOptions changes) {
    if (changes == null) {
      return this;
    }
    return AnimatedMarkerOptions(
      iconImage: changes.iconImage ?? iconImage,
      rotation: changes.rotation ?? rotation,
      geometry: changes.geometry ?? geometry,
    );
  }

  dynamic toJson() {
    final Map<String, dynamic> json = <String, dynamic>{};

    void addIfPresent(String fieldName, dynamic value) {
      if (value != null) {
        json[fieldName] = value;
      }
    }

    addIfPresent('iconImage', iconImage);
    addIfPresent('rotation', rotation?.toJson());
    addIfPresent('geometry', geometry?.toJson());
    return json;
  }
}