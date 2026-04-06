package kr.withrun.was.domain.course.util;

import kr.withrun.was.domain.course.vo.Coordinates;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("하버사인 거리 계산기")
class HaversineDistanceCalculatorTest {

    @DisplayName("유효한 좌표 쌍 중 최소 거리를 반환한다")
    @Test
    void returnsMinimumDistanceAcrossValidCoordinatePairs() {
        Coordinates coordinates = new Coordinates(
                List.of(37.5665, 37.5668, 35.1796),
                List.of(126.9780, 126.9790, 129.0756),
                List.of(10.0, 10.5, 14.0)
        );

        OptionalInt distance = HaversineDistanceCalculator.calculateMinimumDistanceMeters(
                coordinates,
                37.5665,
                126.9780
        );

        assertThat(distance).hasValue(0);
    }

    @DisplayName("계산한 거리를 가장 가까운 미터 단위로 반올림한다")
    @Test
    void roundsCalculatedDistanceToNearestMeter() {
        Coordinates coordinates = new Coordinates(
                List.of(0.0),
                List.of(0.000006),
                List.of(0.0)
        );

        OptionalInt distance = HaversineDistanceCalculator.calculateMinimumDistanceMeters(
                coordinates,
                0.0,
                0.0
        );

        assertThat(distance).hasValue(1);
    }

    @DisplayName("좌표가 null이면 빈 값을 반환한다")
    @Test
    void rejectsNullCoordinates() {
        OptionalInt distance = HaversineDistanceCalculator.calculateMinimumDistanceMeters(
                null,
                37.5665,
                126.9780
        );

        assertThat(distance).isEmpty();
    }

    @DisplayName("위도 목록이 없으면 빈 값을 반환한다")
    @Test
    void rejectsMissingLatitudeList() {
        Coordinates coordinates = new Coordinates(null, List.of(126.9780), List.of(10.0));

        OptionalInt distance = HaversineDistanceCalculator.calculateMinimumDistanceMeters(
                coordinates,
                37.5665,
                126.9780
        );

        assertThat(distance).isEmpty();
    }

    @DisplayName("경도 목록이 없으면 빈 값을 반환한다")
    @Test
    void rejectsMissingLongitudeList() {
        Coordinates coordinates = new Coordinates(List.of(37.5665), null, List.of(10.0));

        OptionalInt distance = HaversineDistanceCalculator.calculateMinimumDistanceMeters(
                coordinates,
                37.5665,
                126.9780
        );

        assertThat(distance).isEmpty();
    }

    @DisplayName("좌표 목록 길이가 다르면 빈 값을 반환한다")
    @Test
    void rejectsMismatchedCoordinateListLengths() {
        Coordinates coordinates = new Coordinates(
                List.of(37.5665, 35.1796),
                List.of(126.9780),
                List.of(10.0, 11.0)
        );

        OptionalInt distance = HaversineDistanceCalculator.calculateMinimumDistanceMeters(
                coordinates,
                37.5665,
                126.9780
        );

        assertThat(distance).isEmpty();
    }

    @DisplayName("좌표 목록이 비어 있으면 빈 값을 반환한다")
    @Test
    void rejectsEmptyCoordinateLists() {
        Coordinates coordinates = new Coordinates(List.of(), List.of(), List.of());

        OptionalInt distance = HaversineDistanceCalculator.calculateMinimumDistanceMeters(
                coordinates,
                37.5665,
                126.9780
        );

        assertThat(distance).isEmpty();
    }

    @DisplayName("유효한 좌표 쌍이 없으면 빈 값을 반환한다")
    @Test
    void rejectsWhenNoValidCoordinatePairsExist() {
        Coordinates coordinates = new Coordinates(
                Arrays.asList(null, null),
                Arrays.asList(126.9780, null),
                Arrays.asList(10.0, 11.0)
        );

        OptionalInt distance = HaversineDistanceCalculator.calculateMinimumDistanceMeters(
                coordinates,
                37.5665,
                126.9780
        );

        assertThat(distance).isEmpty();
    }
}
