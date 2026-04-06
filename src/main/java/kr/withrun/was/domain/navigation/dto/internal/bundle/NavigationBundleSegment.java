package kr.withrun.was.domain.navigation.dto.internal.bundle;

/**
 * 인접 shape point 사이의 세그먼트 메타데이터입니다.
 */
public record NavigationBundleSegment(
        int startShapeIndex,
        int endShapeIndex,
        double distanceMeters,
        Integer bearingDegrees,
        double startCumulativeDistanceMeters,
        double endCumulativeDistanceMeters
) {
}
