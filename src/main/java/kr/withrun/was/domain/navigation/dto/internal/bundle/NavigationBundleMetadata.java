package kr.withrun.was.domain.navigation.dto.internal.bundle;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;

/**
 * 저장된 navigation bundle 의 메타데이터를 표현합니다.
 */
public record NavigationBundleMetadata(
        @JsonIgnore double distanceMeters,
        Instant generatedAt,
        @JsonIgnore String valhallaVersion,
        String generator,
        String algorithmVersion,
        String language,
        String source,
        int shapePointCount,
        int segmentCount
) {

    public NavigationBundleMetadata {
        generator = generator == null ? "polyline-navigation-bundle" : generator;
        algorithmVersion = algorithmVersion == null ? "v1" : algorithmVersion;
        language = language == null ? "ko-KR" : language;
        source = source == null ? "course.coordinates" : source;
    }

    public NavigationBundleMetadata(
            Instant generatedAt,
            String generator,
            String algorithmVersion,
            String language,
            String source,
            int shapePointCount,
            int segmentCount
    ) {
        this(0d, generatedAt, null, generator, algorithmVersion, language, source, shapePointCount, segmentCount);
    }

    public NavigationBundleMetadata(
            double distanceMeters,
            Instant generatedAt,
            String valhallaVersion,
            String language
    ) {
        this(
                distanceMeters,
                generatedAt,
                valhallaVersion,
                "polyline-navigation-bundle",
                "v1",
                language,
                "course.coordinates",
                0,
                0
        );
    }
}
