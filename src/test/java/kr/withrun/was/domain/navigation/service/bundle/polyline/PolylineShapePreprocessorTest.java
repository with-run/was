package kr.withrun.was.domain.navigation.service.bundle.polyline;

import kr.withrun.was.domain.course.vo.Coordinates;
import kr.withrun.was.domain.course.vo.GeoPoint;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleSegment;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleShapePoint;
import kr.withrun.was.domain.navigation.exception.NavigationBundleGenerationException;
import kr.withrun.was.domain.navigation.type.NavigationBundleFailureCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;

@DisplayName("폴리라인 shape 전처리기")
class PolylineShapePreprocessorTest {

    private final PolylineShapePreprocessor preprocessor = new PolylineShapePreprocessor();

    @DisplayName("원본 좌표 순서와 elevation 을 그대로 shape 에 보존한다")
    @Test
    void preservesOriginalShapeOrderAndElevation() {
        PolylineShapePreprocessor.PreprocessedShape result = preprocessor.preprocess(new Coordinates(List.of(
                point(0.0, 0.0, 5.0),
                point(0.0, 0.001, 6.0),
                point(0.0, 0.002, 7.0)
        )));

        assertThat(result.shape()).containsExactly(
                new NavigationBundleShapePoint(0.0, 0.0, 5.0),
                new NavigationBundleShapePoint(0.0, 0.001, 6.0),
                new NavigationBundleShapePoint(0.0, 0.002, 7.0)
        );
    }

    @DisplayName("중복 점도 유지하고 0m segment 를 만든다")
    @Test
    void preservesDuplicatePointsAsZeroDistanceSegments() {
        PolylineShapePreprocessor.PreprocessedShape result = preprocessor.preprocess(new Coordinates(List.of(
                point(0.0, 0.0, 5.0),
                point(0.0, 0.0, 5.5),
                point(0.0, 0.001, 6.0)
        )));

        assertThat(result.shape()).hasSize(3);
        assertThat(result.segments()).extracting(
                NavigationBundleSegment::distanceMeters,
                NavigationBundleSegment::startCumulativeDistanceMeters,
                NavigationBundleSegment::endCumulativeDistanceMeters
        ).containsExactly(
                org.assertj.core.groups.Tuple.tuple(0.0, 0.0, 0.0),
                org.assertj.core.groups.Tuple.tuple(result.segments().get(1).distanceMeters(), 0.0, result.segments().get(1).endCumulativeDistanceMeters())
        );
    }

    @DisplayName("segment 거리와 누적 거리 합계를 계산한다")
    @Test
    void computesSegmentAndCumulativeDistances() {
        PolylineShapePreprocessor.PreprocessedShape result = preprocessor.preprocess(new Coordinates(List.of(
                point(0.0, 0.0, 5.0),
                point(0.0, 0.001, 6.0),
                point(0.0, 0.002, 7.0)
        )));

        assertThat(result.segments()).hasSize(2);
        assertThat(result.segments().get(0).distanceMeters()).isCloseTo(111.2, offset(1.0));
        assertThat(result.segments().get(0).startCumulativeDistanceMeters()).isZero();
        assertThat(result.segments().get(0).endCumulativeDistanceMeters()).isCloseTo(111.2, offset(1.0));
        assertThat(result.segments().get(1).distanceMeters()).isCloseTo(111.2, offset(1.0));
        assertThat(result.segments().get(1).startCumulativeDistanceMeters()).isCloseTo(111.2, offset(1.0));
        assertThat(result.segments().get(1).endCumulativeDistanceMeters()).isCloseTo(222.4, offset(1.5));
        assertThat(result.totalDistanceMeters()).isCloseTo(222.4, offset(1.5));
    }

    @DisplayName("유효한 점이 2개 미만이면 실패한다")
    @Test
    void rejectsWhenFewerThanTwoPointsExist() {
        assertThatThrownBy(() -> preprocessor.preprocess(new Coordinates(List.of(point(0.0, 0.0, 5.0)))))
                .isInstanceOf(NavigationBundleGenerationException.class)
                .extracting("failureCode")
                .isEqualTo(NavigationBundleFailureCode.INVALID_COURSE_COORDINATES);
    }

    @DisplayName("null 위도나 경도가 있으면 실패한다")
    @Test
    void rejectsNullLatitudeOrLongitude() {
        Coordinates coordinates = new Coordinates(List.of(
                point(null, 0.0, 5.0),
                point(0.0, 0.001, 6.0)
        ));

        assertThatThrownBy(() -> preprocessor.preprocess(coordinates))
                .isInstanceOf(NavigationBundleGenerationException.class)
                .extracting("failureCode")
                .isEqualTo(NavigationBundleFailureCode.INVALID_COURSE_COORDINATES);
    }

    private static GeoPoint point(Double latitude, Double longitude, double elevationM) {
        return new GeoPoint(latitude, longitude, elevationM);
    }
}
