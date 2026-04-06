package kr.withrun.was.domain.navigation.service.bundle.polyline;

import com.fasterxml.jackson.databind.json.JsonMapper;
import kr.withrun.was.domain.course.vo.Coordinates;
import kr.withrun.was.domain.course.vo.GeoPoint;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleDocument;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleManeuver;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleManeuverType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("폴리라인 번들 문서 팩토리")
class PolylineBundleDocumentFactoryTest {

    private final PolylineShapePreprocessor preprocessor = new PolylineShapePreprocessor();
    private final PolylineManeuverDetector detector = new PolylineManeuverDetector(
            new PolylineNavigationProperties(),
            new PolylineInstructionFormatter()
    );
    private final PolylineBundleDocumentFactory factory = new PolylineBundleDocumentFactory(
            preprocessor,
            detector,
            Clock.fixed(Instant.parse("2026-03-27T05:00:00Z"), ZoneOffset.UTC),
            new PolylineNavigationProperties()
    );

    @DisplayName("원본 shape, segment, maneuver, metadata 를 번들 문서로 조립한다")
    @Test
    void buildsBundleDocumentFromPolylinePipeline() throws Exception {
        Coordinates coordinates = new Coordinates(List.of(
                new GeoPoint(0.0, 0.0, 3.0),
                new GeoPoint(0.0, 0.001, 4.0),
                new GeoPoint(-0.001, 0.001, 5.0),
                new GeoPoint(-0.002, 0.001, 6.0)
        ));

        PolylineShapePreprocessor.PreprocessedShape preprocessedShape = preprocessor.preprocess(coordinates);
        List<NavigationBundleManeuver> detectedManeuvers = detector.detect(preprocessedShape);

        NavigationBundleDocument document = factory.build(coordinates);
        String json = JsonMapper.builder().findAndAddModules().build().writeValueAsString(document);

        assertThat(document.schemaVersion()).isEqualTo(NavigationBundleDocument.CURRENT_SCHEMA_VERSION);
        assertThat(document.shape()).containsExactlyElementsOf(preprocessedShape.shape());
        assertThat(document.segments()).containsExactlyElementsOf(preprocessedShape.segments());
        assertThat(document.maneuvers()).containsExactlyElementsOf(detectedManeuvers);
        assertThat(document.maneuvers()).isSortedAccordingTo(java.util.Comparator.comparingDouble(NavigationBundleManeuver::cumulativeDistanceMeters));
        assertThat(document.maneuvers().getLast().maneuverType()).isEqualTo(NavigationBundleManeuverType.ARRIVAL);
        assertThat(document.totalDistanceMeters()).isEqualTo(preprocessedShape.totalDistanceMeters());

        assertThat(document.metadata().generatedAt()).isEqualTo(Instant.parse("2026-03-27T05:00:00Z"));
        assertThat(document.metadata().generator()).isEqualTo("polyline-navigation-bundle");
        assertThat(document.metadata().algorithmVersion()).isEqualTo("v1");
        assertThat(document.metadata().language()).isEqualTo("ko-KR");
        assertThat(document.metadata().source()).isEqualTo("course.coordinates");
        assertThat(document.metadata().shapePointCount()).isEqualTo(4);
        assertThat(document.metadata().segmentCount()).isEqualTo(3);

        assertThat(json).contains("\"schemaVersion\":\"navigation-bundle/v2\"");
        assertThat(json).doesNotContain("edges");
        assertThat(json).doesNotContain("matchedPoints");
        assertThat(json).doesNotContain("shapeLengths");
        assertThat(json).doesNotContain("valhallaVersion");
    }
}
