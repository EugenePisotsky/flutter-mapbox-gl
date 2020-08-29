part of mapbox_gl_platform_interface;

class FloatingLabel {
  FloatingLabel(this._id, this.options, [this._data]);

  /// A unique identifier for this symbol.
  ///
  /// The identifier is an arbitrary unique string.
  final String _id;

  String get id => _id;

  final Map _data;
  Map get data => _data;

  FloatingLabelOptions options;
}

class FloatingLabelOptions {
  const FloatingLabelOptions({
    this.image, this.icon, this.geometry, this.id, this.size
  });

  final BitmapDescriptor image;
  final String icon;
  final LatLng geometry;
  final String id;
  final Offset size;

  static const FloatingLabelOptions defaultOptions = FloatingLabelOptions();

  FloatingLabelOptions copyWith(FloatingLabelOptions changes) {
    if (changes == null) {
      return this;
    }
    return FloatingLabelOptions(
      id: changes.id ?? id,
      size: changes.size ?? size,
      image: changes.image ?? image,
      icon: changes.icon ?? icon,
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

    addIfPresent('id', id);
    addIfPresent('icon', icon);
    addIfPresent('image', image?._toJson());
    addIfPresent('width', size.dx);
    addIfPresent('height', size.dy);
    addIfPresent('geometry', geometry?.toJson());
    return json;
  }
}