package kr.withrun.was.domain.course.vo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.ArrayList;
import java.util.List;

public record Coordinates(List<GeoPoint> values) {

    public Coordinates {
        values = values == null ? List.of() : List.copyOf(values);
    }

    // test코드용 생성자
    public Coordinates(
            List<Double> latitudes,
            List<Double> longitudes,
            List<Double> elevations
    ) {
        this(toGeoPoints(latitudes, longitudes, elevations));
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Coordinates fromJson(List<GeoPoint> values) {
        return new Coordinates(values);
    }

    @JsonValue
    public List<GeoPoint> toJson() {
        return values;
    }

    // test코드용 유틸함수
    private static List<GeoPoint> toGeoPoints(
            List<Double> latitudes,
            List<Double> longitudes,
            List<Double> elevations
    ) {
        if (latitudes == null || longitudes == null || elevations == null) {
            return List.of();
        }
        if (latitudes.size() != longitudes.size() || latitudes.size() != elevations.size()) {
            return List.of();
        }

        int size = latitudes.size();
        if (size == 0) {
            return List.of();
        }

        ArrayList<GeoPoint> points = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            points.add(new GeoPoint(
                    latitudes.get(index),
                    longitudes.get(index),
                    elevations.get(index)
            ));
        }
        return List.copyOf(points);
    }
}
