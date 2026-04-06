package kr.withrun.was.domain.navigation.dto.internal.bundle;

import java.util.List;

/**
 * S3 에 저장되는 latest navigation bundle 문서 구조를 표현하는 내부 DTO 입니다.
 */
public record NavigationBundleDocument(
        String schemaVersion,
        List<NavigationBundleShapePoint> shape,
        List<NavigationBundleSegment> segments,
        List<NavigationBundleManeuver> maneuvers,
        double totalDistanceMeters,
        NavigationBundleMetadata metadata
) {

    public static final String CURRENT_SCHEMA_VERSION = "navigation-bundle/v2";

    public NavigationBundleDocument {
        schemaVersion = schemaVersion == null ? CURRENT_SCHEMA_VERSION : schemaVersion;
        shape = shape == null ? List.of() : List.copyOf(shape);
        segments = segments == null ? List.of() : List.copyOf(segments);
        maneuvers = maneuvers == null ? List.of() : List.copyOf(maneuvers);
    }

    public NavigationBundleDocument(
            List<NavigationBundleShapePoint> shape,
            List<NavigationBundleSegment> segments,
            List<NavigationBundleManeuver> maneuvers,
            double totalDistanceMeters,
            NavigationBundleMetadata metadata
    ) {
        this(CURRENT_SCHEMA_VERSION, shape, segments, maneuvers, totalDistanceMeters, metadata);
    }
}
