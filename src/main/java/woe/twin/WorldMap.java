package woe.twin;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

interface WorldMap {

  int zoomMax = 18;

  static String entityIdOf(Region region) {
    return String.format("%d:%1.13f:%1.13f:%1.13f:%1.13f", region.zoom,
        region.topLeft.lat, region.topLeft.lng, region.botRight.lat, region.botRight.lng);
  }

  static Region regionForEntityId(String entityId) {
    final String[] fields = entityId.split(":");
    final int zoom = Integer.parseInt(fields[0]);
    final double topLeftLat = Double.parseDouble(fields[1]);
    final double topLeftLng = Double.parseDouble(fields[2]);
    final double botRightLat = Double.parseDouble(fields[3]);
    final double botRightLng = Double.parseDouble(fields[4]);
    return new WorldMap.Region(zoom, topLeft(topLeftLat, topLeftLng), botRight(botRightLat, botRightLng));
  }

  static LatLng topLeft(double lat, double lng) {
    return new LatLng(lat, lng);
  }

  static LatLng botRight(double lat, double lng) {
    return new LatLng(lat, lng);
  }

  static Region region(int zoom, LatLng topLeft, LatLng botRight) {
    return new Region(zoom, topLeft, botRight);
  }

  static Region regionForZoom0() {
    return region(0, topLeft(90, -180), botRight(-90, 180));
  }

  /* level 0 - 1 region  180 / 360
   * level 1 - 2 regions 180 / 1 x 180 / 2, 180 lat x 180 lng, on either side of lng 0 meridian
   * level 2 - 9 regions 180 / 3 x 180 / 3,  60 lat x  60 lng
   * level 3 - 9 regions  60 / 3 x  60 / 3,  20 lat x  20 lng
   * level 4 - 4 regions  20 / 2 x  20 / 2,  10 lat x  10 lng
   * level 5 - 4 regions  10 / 2 x  10 / 2,   5 lat x   5 lng, subdivide by 4 down to zoom 18
   */
  static List<Region> subRegionsFor(Region region) {
    switch (region.zoom) {
      case 0:
        return subRegionsForZoom0();
      case 1:
      case 2:
        return subRegionsForZoomX(region, 3);
      default:
        return subRegionsForZoomX(region, 2);
    }
  }

  private static List<Region> subRegionsForZoom0() {
    final List<Region> regions = new ArrayList<>();
    regions.add(region(1, topLeft(90, -180), botRight(-90, 0)));
    regions.add(region(1, topLeft(90, 0), botRight(-90, 180)));
    return regions;
  }

  private static List<Region> subRegionsForZoomX(Region region, int splits) {
    final double length = (region.topLeft.lat - region.botRight.lat) / splits;
    List<Region> regions = new ArrayList<>();
    if (region.zoom >= zoomMax) {
      return regions;
    }
    IntStream.range(0, splits).forEach(latIndex -> IntStream.range(0, splits).forEach(lngIndex -> {
      final LatLng topLeft = topLeft(region.topLeft.lat - latIndex * length, region.topLeft.lng + lngIndex * length);
      final LatLng botRight = botRight(region.topLeft.lat - (latIndex + 1) * length, region.topLeft.lng + (lngIndex + 1) * length);
      regions.add(region(region.zoom + 1, topLeft, botRight));
    }));
    return regions;
  }

  static Region regionAtLatLng(int zoom, LatLng latLng) {
    return regionAtLatLng(zoom, latLng, regionForZoom0());
  }

  private static Region regionAtLatLng(int zoom, LatLng latLng, Region region) {
    if (zoom == region.zoom) {
      return region;
    }
    final List<Region> subRegions = subRegionsFor(region);
    final Optional<Region> subRegionOpt = subRegions.stream().filter(r -> r.contains(latLng)).findFirst();
    return subRegionOpt.map(subRegion -> regionAtLatLng(zoom, latLng, subRegion)).orElse(null);
  }

  static LatLng latLng(double lat, double lng) {
    return new LatLng(lat, lng);
  }

  static LatLng atCenter(Region region) {
    return latLng(region.topLeft.lat - (region.topLeft.lat - region.botRight.lat) / 2,
        region.topLeft.lng + (region.botRight.lng - region.topLeft.lng) / 2);
  }

  static List<Region> regionsIn(Region area) {
    final Region start = regionAtLatLng(area.zoom, area.topLeft);
    List<Region> regions = new ArrayList<>(regionsInRow(area, start));
    (new ArrayList<>(regions)).forEach(region -> regions.addAll(regionsInCol(area, region)));
    return regions;
  }

  private static List<Region> regionsInRow(Region area, Region start) {
    final List<Region> regions = new ArrayList<>();
    regions.add(start);
    Region next = start.cloneRight();
    while (area.overlaps(next)) {
      regions.add(next);
      next = next.cloneRight();
    }
    return regions;
  }

  private static List<Region> regionsInCol(Region area, Region start) {
    final List<Region> regions = new ArrayList<>();
    Region next = start.cloneBelow();
    while (area.overlaps(next)) {
      regions.add(next);
      next = next.cloneBelow();
    }
    return regions;
  }

  class LatLng implements CborSerializable {
    public final double lat;
    public final double lng;

    @JsonCreator
    public LatLng(@JsonProperty("lat") double lat, @JsonProperty("lng") double lng) {
      this.lat = lat;
      this.lng = lng;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      final LatLng latLng = (LatLng) o;
      return Double.compare(latLng.lat, lat) == 0 &&
          Double.compare(latLng.lng, lng) == 0;
    }

    @Override
    public int hashCode() {
      return Objects.hash(lat, lng);
    }

    @Override
    public String toString() {
      return String.format("%s[lat %f, lng %f]", getClass().getSimpleName(), lat, lng);
    }
  }

  class Region implements CborSerializable {
    public final int zoom;
    public final LatLng topLeft;
    public final LatLng botRight;

    @JsonCreator
    Region(@JsonProperty("zoom") int zoom, @JsonProperty("topLeft") LatLng topLeft, @JsonProperty("botRight") LatLng botRight) {
      if (zoom < 0 || zoom > 18) {
        throw new IllegalArgumentException("Zoom must be in >= 0 and <= 18.");
      }
      if (topLeft.lat <= botRight.lat) {
        throw new IllegalArgumentException("Top left latitude must be greater than bottom right latitude.");
      }
      if (topLeft.lng >= botRight.lng) {
        throw new IllegalArgumentException("Top left longitude must be less than bottom right longitude.");
      }
      this.zoom = zoom;
      this.topLeft = topLeft;
      this.botRight = botRight;
    }

    boolean overlaps(Region region) {
      return !isThisAbove(region) && !isThisBelow(region) && !isThisLeft(region) && !isThisRight(region);
    }

    private boolean isThisAbove(Region region) {
      return botRight.lat >= region.topLeft.lat;
    }

    private boolean isThisBelow(Region region) {
      return topLeft.lat <= region.botRight.lat;
    }

    private boolean isThisLeft(Region region) {
      return botRight.lng <= region.topLeft.lng;
    }

    private boolean isThisRight(Region region) {
      return topLeft.lng >= region.botRight.lng;
    }

    boolean contains(Region region) {
      return contains(region.topLeft) && contains(region.botRight);
    }

    boolean contains(LatLng latLng) {
      return topLeft.lat >= latLng.lat && botRight.lat <= latLng.lat
          && topLeft.lng <= latLng.lng && botRight.lng >= latLng.lng;
    }

    boolean isDevice() {
      return zoom == zoomMax; // devices are represented at finest zoom in level.
    }

    Region cloneRight() {
      final double lngDelta = botRight.lng - topLeft.lng;
      return new Region(zoom, topLeft(topLeft.lat, topLeft.lng + lngDelta), botRight(botRight.lat, botRight.lng + lngDelta));
    }

    Region cloneBelow() {
      final double latDelta = botRight.lat - topLeft.lat;
      return new Region(zoom, topLeft(topLeft.lat + latDelta, topLeft.lng), botRight(botRight.lat + latDelta, botRight.lng));
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Region region = (Region) o;
      return zoom == region.zoom &&
          topLeft.equals(region.topLeft) &&
          botRight.equals(region.botRight);
    }

    @Override
    public int hashCode() {
      return Objects.hash(zoom, topLeft, botRight);
    }

    @Override
    public String toString() {
      return String.format("%s[zoom %d, topLeft %s, botRight %s]", getClass().getSimpleName(), zoom, topLeft, botRight);
    }
  }
}
