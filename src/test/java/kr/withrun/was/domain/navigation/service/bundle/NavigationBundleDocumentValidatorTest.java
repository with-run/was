package kr.withrun.was.domain.navigation.service.bundle;

import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleDocument;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleManeuver;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleManeuverSampleAction;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleManeuverType;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleMetadata;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleSegment;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleShapePoint;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleTurnStrength;
import kr.withrun.was.domain.navigation.exception.NavigationBundleGenerationException;
import kr.withrun.was.domain.navigation.type.NavigationBundleFailureCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("네비게이션 번들 문서 검증기")
class NavigationBundleDocumentValidatorTest {

    private final NavigationBundleDocumentValidator validator = new NavigationBundleDocumentValidator();

    @DisplayName("shape, segment, maneuver 관계가 정상이면 통과한다")
    @Test
    void acceptsValidPolylineBundleDocument() {
        assertThatCode(() -> validator.validate(validDocument())).doesNotThrowAnyException();
    }

    @DisplayName("shape point 가 2개 미만이면 실패한다")
    @Test
    void rejectsDocumentWithTooFewShapePoints() {
        NavigationBundleDocument document = new NavigationBundleDocument(
                List.of(shapePoint(37.5665, 126.9780, 12.0)),
                List.of(),
                List.of(arrivalManeuver(0, 0d)),
                0d,
                metadata(1, 0)
        );

        assertBundleValidationFailure(document);
    }

    @DisplayName("segment 개수가 shape size - 1 과 다르면 실패한다")
    @Test
    void rejectsDocumentWhenSegmentCountDoesNotMatchShapeCount() {
        NavigationBundleDocument document = new NavigationBundleDocument(
                shape(),
                List.of(segment(0, 1, 104d, 0d, 104d)),
                validManeuvers(),
                209d,
                metadata(3, 1)
        );

        assertBundleValidationFailure(document);
    }

    @DisplayName("segment index 가 연속적이지 않으면 실패한다")
    @Test
    void rejectsDocumentWithNonContiguousSegments() {
        NavigationBundleDocument document = new NavigationBundleDocument(
                shape(),
                List.of(
                        segment(0, 1, 104d, 0d, 104d),
                        segment(2, 3, 105d, 104d, 209d)
                ),
                validManeuvers(),
                209d,
                metadata(3, 2)
        );

        assertBundleValidationFailure(document);
    }

    @DisplayName("누적 거리가 감소하면 실패한다")
    @Test
    void rejectsDocumentWithNonMonotonicCumulativeDistances() {
        NavigationBundleDocument document = new NavigationBundleDocument(
                shape(),
                List.of(
                        segment(0, 1, 104d, 0d, 104d),
                        segment(1, 2, 105d, 103d, 102d)
                ),
                validManeuvers(),
                209d,
                metadata(3, 2)
        );

        assertBundleValidationFailure(document);
    }

    @DisplayName("total distance 와 마지막 segment 종료 거리가 어긋나면 실패한다")
    @Test
    void rejectsDocumentWhenTotalDistanceDoesNotMatchLastSegment() {
        NavigationBundleDocument document = new NavigationBundleDocument(
                shape(),
                validSegments(),
                validManeuvers(),
                250d,
                metadata(3, 2)
        );

        assertBundleValidationFailure(document);
    }

    @DisplayName("maneuver 가 누적 거리 기준으로 정렬되지 않으면 실패한다")
    @Test
    void rejectsDocumentWhenManeuversAreNotSortedByDistance() {
        NavigationBundleDocument document = new NavigationBundleDocument(
                shape(),
                validSegments(),
                List.of(arrivalManeuver(2, 209d), turnManeuver(1, 104d)),
                209d,
                metadata(3, 2)
        );

        assertBundleValidationFailure(document);
    }

    @DisplayName("마지막 maneuver 가 도착 이벤트가 아니면 실패한다")
    @Test
    void rejectsDocumentWhenLastManeuverIsNotArrivalAtFinalShapeIndex() {
        NavigationBundleDocument document = new NavigationBundleDocument(
                shape(),
                validSegments(),
                List.of(
                        turnManeuver(1, 104d),
                        new NavigationBundleManeuver(
                                NavigationBundleManeuverType.RIGHT,
                                NavigationBundleTurnStrength.NORMAL,
                                NavigationBundleManeuverSampleAction.RIGHT,
                                "Turn right again",
                                1,
                                120d,
                                0,
                                1,
                                110,
                                180,
                                70
                        )
                ),
                209d,
                metadata(3, 2)
        );

        assertBundleValidationFailure(document);
    }

    private static NavigationBundleDocument validDocument() {
        return new NavigationBundleDocument(
                shape(),
                validSegments(),
                validManeuvers(),
                209d,
                metadata(3, 2)
        );
    }

    private static List<NavigationBundleShapePoint> shape() {
        return List.of(
                shapePoint(37.5665, 126.9780, 12.0),
                shapePoint(37.5670, 126.9790, 13.0),
                shapePoint(37.5675, 126.9800, 14.0)
        );
    }

    private static List<NavigationBundleSegment> validSegments() {
        return List.of(
                segment(0, 1, 104d, 0d, 104d),
                segment(1, 2, 105d, 104d, 209d)
        );
    }

    private static List<NavigationBundleManeuver> validManeuvers() {
        return List.of(
                turnManeuver(1, 104d),
                arrivalManeuver(2, 209d)
        );
    }

    private static NavigationBundleShapePoint shapePoint(double lat, double lon, double elevationM) {
        return new NavigationBundleShapePoint(lat, lon, elevationM);
    }

    private static NavigationBundleSegment segment(
            int startShapeIndex,
            int endShapeIndex,
            double distanceMeters,
            double startCumulativeDistanceMeters,
            double endCumulativeDistanceMeters
    ) {
        return new NavigationBundleSegment(
                startShapeIndex,
                endShapeIndex,
                distanceMeters,
                152,
                startCumulativeDistanceMeters,
                endCumulativeDistanceMeters
        );
    }

    private static NavigationBundleManeuver turnManeuver(int shapeIndex, double cumulativeDistanceMeters) {
        return new NavigationBundleManeuver(
                NavigationBundleManeuverType.RIGHT,
                NavigationBundleTurnStrength.NORMAL,
                NavigationBundleManeuverSampleAction.RIGHT,
                "Turn right",
                shapeIndex,
                cumulativeDistanceMeters,
                0,
                2,
                147,
                232,
                85
        );
    }

    private static NavigationBundleManeuver arrivalManeuver(int shapeIndex, double cumulativeDistanceMeters) {
        return new NavigationBundleManeuver(
                NavigationBundleManeuverType.ARRIVAL,
                null,
                NavigationBundleManeuverSampleAction.ARRIVAL,
                "Arrive at destination",
                shapeIndex,
                cumulativeDistanceMeters,
                null,
                null,
                null,
                null,
                null
        );
    }

    private static NavigationBundleMetadata metadata(int shapePointCount, int segmentCount) {
        return new NavigationBundleMetadata(
                Instant.parse("2026-03-26T01:02:03Z"),
                "polyline-navigation-bundle",
                "v1",
                "ko-KR",
                "course.coordinates",
                shapePointCount,
                segmentCount
        );
    }

    private void assertBundleValidationFailure(NavigationBundleDocument document) {
        assertThatThrownBy(() -> validator.validate(document))
                .isInstanceOf(NavigationBundleGenerationException.class)
                .extracting("failureCode")
                .isEqualTo(NavigationBundleFailureCode.BUNDLE_VALIDATION_FAILED);
    }
}
