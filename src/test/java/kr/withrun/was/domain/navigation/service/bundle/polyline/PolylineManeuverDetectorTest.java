package kr.withrun.was.domain.navigation.service.bundle.polyline;

import kr.withrun.was.domain.course.vo.Coordinates;
import kr.withrun.was.domain.course.vo.GeoPoint;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleManeuver;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleManeuverSampleAction;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleManeuverType;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleTurnStrength;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("폴리라인 maneuver 감지기")
class PolylineManeuverDetectorTest {

    private final PolylineShapePreprocessor preprocessor = new PolylineShapePreprocessor();
    private final PolylineManeuverDetector detector = new PolylineManeuverDetector(
            new PolylineNavigationProperties(),
            new PolylineInstructionFormatter()
    );

    @DisplayName("직선 경로는 도착 maneuver 하나만 만든다")
    @Test
    void returnsOnlyArrivalForStraightRoute() {
        List<NavigationBundleManeuver> maneuvers = detect(route(
                point(0.0, 0.0),
                point(0.0, 0.001),
                point(0.0, 0.002),
                point(0.0, 0.003)
        ));

        assertThat(maneuvers).hasSize(1);
        assertThat(maneuvers.getFirst().maneuverType()).isEqualTo(NavigationBundleManeuverType.ARRIVAL);
        assertThat(maneuvers.getFirst().sampleAction()).isEqualTo(NavigationBundleManeuverSampleAction.ARRIVAL);
    }

    @DisplayName("완만한 좌우회전을 구분한다")
    @Test
    void classifiesSlightLeftAndRightTurns() {
        assertThat(detect(route(
                point(0.0, 0.0),
                point(0.0, 0.001),
                point(-0.0005, 0.0015),
                point(-0.001, 0.002)
        ))).extracting(
                NavigationBundleManeuver::maneuverType,
                NavigationBundleManeuver::strength,
                NavigationBundleManeuver::sampleAction
        ).containsSequence(
                org.assertj.core.groups.Tuple.tuple(
                        NavigationBundleManeuverType.SLIGHT_RIGHT,
                        NavigationBundleTurnStrength.SLIGHT,
                        NavigationBundleManeuverSampleAction.RIGHT
                )
        );

        assertThat(detect(route(
                point(0.0, 0.0),
                point(0.0, 0.001),
                point(0.0005, 0.0015),
                point(0.001, 0.002)
        ))).extracting(
                NavigationBundleManeuver::maneuverType,
                NavigationBundleManeuver::strength,
                NavigationBundleManeuver::sampleAction
        ).containsSequence(
                org.assertj.core.groups.Tuple.tuple(
                        NavigationBundleManeuverType.SLIGHT_LEFT,
                        NavigationBundleTurnStrength.SLIGHT,
                        NavigationBundleManeuverSampleAction.LEFT
                )
        );
    }

    @DisplayName("일반 좌우회전을 구분한다")
    @Test
    void classifiesNormalLeftAndRightTurns() {
        assertThat(detect(route(
                point(0.0, 0.0),
                point(0.0, 0.001),
                point(-0.001, 0.001),
                point(-0.002, 0.001)
        ))).extracting(
                NavigationBundleManeuver::maneuverType,
                NavigationBundleManeuver::strength,
                NavigationBundleManeuver::instruction
        ).containsSequence(
                org.assertj.core.groups.Tuple.tuple(
                        NavigationBundleManeuverType.RIGHT,
                        NavigationBundleTurnStrength.NORMAL,
                        "우회전"
                )
        );

        assertThat(detect(route(
                point(0.0, 0.0),
                point(0.0, 0.001),
                point(0.001, 0.001),
                point(0.002, 0.001)
        ))).extracting(
                NavigationBundleManeuver::maneuverType,
                NavigationBundleManeuver::strength,
                NavigationBundleManeuver::instruction
        ).containsSequence(
                org.assertj.core.groups.Tuple.tuple(
                        NavigationBundleManeuverType.LEFT,
                        NavigationBundleTurnStrength.NORMAL,
                        "좌회전"
                )
        );
    }

    @DisplayName("급회전과 유턴을 구분한다")
    @Test
    void classifiesSharpTurnAndUTurn() {
        assertThat(detect(route(
                point(0.0, 0.0),
                point(0.0, 0.001),
                point(0.0005, 0.0005),
                point(0.001, 0.0)
        ))).extracting(
                NavigationBundleManeuver::maneuverType,
                NavigationBundleManeuver::strength,
                NavigationBundleManeuver::sampleAction
        ).containsSequence(
                org.assertj.core.groups.Tuple.tuple(
                        NavigationBundleManeuverType.SHARP_LEFT,
                        NavigationBundleTurnStrength.SHARP,
                        NavigationBundleManeuverSampleAction.LEFT
                )
        );

        assertThat(detect(route(
                point(0.0, 0.0),
                point(0.0, 0.001),
                point(0.0, 0.0005),
                point(0.0, 0.0)
        ))).extracting(
                NavigationBundleManeuver::maneuverType,
                NavigationBundleManeuver::strength,
                NavigationBundleManeuver::sampleAction
        ).containsSequence(
                org.assertj.core.groups.Tuple.tuple(
                        NavigationBundleManeuverType.UTURN,
                        NavigationBundleTurnStrength.SHARP,
                        NavigationBundleManeuverSampleAction.UTURN
                )
        );
    }

    @DisplayName("시작점 근처의 미세 지터는 무시한다")
    @Test
    void suppressesMicroSegmentJitter() {
        List<NavigationBundleManeuver> maneuvers = detect(route(
                point(0.0, 0.0),
                point(0.000018, 0.0),
                point(0.000018, 0.001),
                point(0.000018, 0.002)
        ));

        assertThat(maneuvers).extracting(NavigationBundleManeuver::maneuverType)
                .containsExactly(NavigationBundleManeuverType.ARRIVAL);
    }

    @DisplayName("같은 방향의 연속 회전은 24m 안에서 하나로 합친다")
    @Test
    void mergesNearbySameDirectionTurns() {
        List<NavigationBundleManeuver> maneuvers = detect(route(
                point(0.0, 0.0),
                point(0.0, 0.001),
                point(-0.0001, 0.001),
                point(-0.0001, 0.0009),
                point(-0.0001, 0.0008)
        ));

        assertThat(maneuvers).extracting(NavigationBundleManeuver::maneuverType)
                .containsExactly(NavigationBundleManeuverType.RIGHT, NavigationBundleManeuverType.ARRIVAL);
    }

    @DisplayName("진짜 S턴은 두 maneuver 로 유지한다")
    @Test
    void preservesTrueSTurnAsTwoManeuvers() {
        List<NavigationBundleManeuver> maneuvers = detect(route(
                point(0.0, 0.0),
                point(0.0, 0.001),
                point(-0.0001, 0.001),
                point(-0.0001, 0.0011),
                point(-0.0001, 0.0012)
        ));

        assertThat(maneuvers).extracting(NavigationBundleManeuver::maneuverType)
                .containsExactly(
                        NavigationBundleManeuverType.RIGHT,
                        NavigationBundleManeuverType.LEFT,
                        NavigationBundleManeuverType.ARRIVAL
                );
    }

    @DisplayName("도착 maneuver 는 마지막 shape 에 정확히 한 번만 추가한다")
    @Test
    void appendsArrivalExactlyOnce() {
        List<NavigationBundleManeuver> maneuvers = detect(route(
                point(0.0, 0.0),
                point(0.0, 0.001),
                point(0.0, 0.001)
        ));

        assertThat(maneuvers.stream().filter(maneuver -> maneuver.maneuverType() == NavigationBundleManeuverType.ARRIVAL)).hasSize(1);
        assertThat(maneuvers.getLast().shapeIndex()).isEqualTo(2);
    }

    private List<NavigationBundleManeuver> detect(Coordinates coordinates) {
        return detector.detect(preprocessor.preprocess(coordinates));
    }

    private static Coordinates route(GeoPoint... points) {
        return new Coordinates(List.of(points));
    }

    private static GeoPoint point(double latitude, double longitude) {
        return new GeoPoint(latitude, longitude, 0.0);
    }
}
