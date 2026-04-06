package kr.withrun.was.domain.navigation.service.bundle.polyline;

import kr.withrun.was.domain.course.util.HaversineDistanceCalculator;
import kr.withrun.was.domain.course.vo.Coordinates;
import kr.withrun.was.domain.course.vo.GeoPoint;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleSegment;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleShapePoint;
import kr.withrun.was.domain.navigation.exception.NavigationBundleGenerationException;
import kr.withrun.was.domain.navigation.type.NavigationBundleFailureCode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PolylineShapePreprocessor {

    public PreprocessedShape preprocess(Coordinates coordinates) {
        List<GeoPoint> sourcePoints = coordinates == null ? List.of() : coordinates.values();
        if (sourcePoints.size() < 2) {
            throw invalidCoordinates("At least two course coordinates are required");
        }

        List<NavigationBundleShapePoint> shape = new ArrayList<>(sourcePoints.size());
        List<NavigationBundleSegment> segments = new ArrayList<>(sourcePoints.size() - 1);

        double cumulativeDistanceMeters = 0d;
        GeoPoint previousPoint = null;
        for (int index = 0; index < sourcePoints.size(); index++) {
            GeoPoint currentPoint = sourcePoints.get(index);
            double latitude = requireCoordinate(currentPoint == null ? null : currentPoint.latitude(), "latitude");
            double longitude = requireCoordinate(currentPoint == null ? null : currentPoint.longitude(), "longitude");

            shape.add(new NavigationBundleShapePoint(latitude, longitude, currentPoint.elevationM()));
            if (previousPoint != null) {
                double distanceMeters = HaversineDistanceCalculator.calculatePreciseDistanceMeters(
                        previousPoint.latitude(),
                        previousPoint.longitude(),
                        latitude,
                        longitude
                );
                double segmentStartDistanceMeters = cumulativeDistanceMeters;
                cumulativeDistanceMeters += distanceMeters;
                segments.add(new NavigationBundleSegment(
                        index - 1,
                        index,
                        distanceMeters,
                        calculateInitialBearingDegrees(previousPoint.latitude(), previousPoint.longitude(), latitude, longitude),
                        segmentStartDistanceMeters,
                        cumulativeDistanceMeters
                ));
            }

            previousPoint = currentPoint;
        }

        return new PreprocessedShape(shape, segments, cumulativeDistanceMeters);
    }

    private static double requireCoordinate(Double coordinate, String axis) {
        if (coordinate == null) {
            throw invalidCoordinates("Course coordinate " + axis + " must not be null");
        }
        return coordinate;
    }

    private static Integer calculateInitialBearingDegrees(
            double startLatitude,
            double startLongitude,
            double endLatitude,
            double endLongitude
    ) {
        if (Double.compare(startLatitude, endLatitude) == 0 && Double.compare(startLongitude, endLongitude) == 0) {
            return 0;
        }

        double startLatitudeRadians = Math.toRadians(startLatitude);
        double endLatitudeRadians = Math.toRadians(endLatitude);
        double longitudeDeltaRadians = Math.toRadians(endLongitude - startLongitude);

        double y = Math.sin(longitudeDeltaRadians) * Math.cos(endLatitudeRadians);
        double x = Math.cos(startLatitudeRadians) * Math.sin(endLatitudeRadians)
                - Math.sin(startLatitudeRadians) * Math.cos(endLatitudeRadians) * Math.cos(longitudeDeltaRadians);
        double bearingDegrees = Math.toDegrees(Math.atan2(y, x));

        return (int) Math.round((bearingDegrees + 360d) % 360d);
    }

    private static NavigationBundleGenerationException invalidCoordinates(String message) {
        return new NavigationBundleGenerationException(NavigationBundleFailureCode.INVALID_COURSE_COORDINATES, message);
    }

    public record PreprocessedShape(
            List<NavigationBundleShapePoint> shape,
            List<NavigationBundleSegment> segments,
            double totalDistanceMeters
    ) {

        public PreprocessedShape {
            shape = shape == null ? List.of() : List.copyOf(shape);
            segments = segments == null ? List.of() : List.copyOf(segments);
        }
    }
}
