package kr.withrun.was.domain.running.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import kr.withrun.was.domain.running.type.GhostResultStatus;
import kr.withrun.was.domain.running.type.RunningMode;
import kr.withrun.was.domain.running.type.RunningSessionCompleteState;
import kr.withrun.was.global.common.type.TimeSlot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.RecordComponent;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("과거 러닝 세션 조회 DTO")
class PastRunningSessionsDtoTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @DisplayName("요청 레코드의 구성 요소를 정확히 노출한다")
    @Test
    void exposesExactRequestComponents() {
        assertThat(componentNamesOf(PastRunningSessionsRequest.class))
                .containsExactly("year", "month", "day", "pageSize", "cursor");
    }

    @DisplayName("유효한 전체 조회 요청은 허용한다")
    @Test
    void acceptsValidRequestWithoutDateFilters() {
        Set<ConstraintViolation<PastRunningSessionsRequest>> violations = validate(
                new PastRunningSessionsRequest(null, null, null, 10, null)
        );

        assertThat(violations).isEmpty();
    }

    @DisplayName("pageSize가 없으면 기본값 10을 적용한다")
    @Test
    void defaultsMissingPageSizeToTen() {
        PastRunningSessionsRequest request = new PastRunningSessionsRequest(2026, 3, 16, null, null);

        assertThat(request.pageSize()).isEqualTo(10);
    }

    @DisplayName("연도는 양수여야 한다")
    @ParameterizedTest(name = "year={0}")
    @ValueSource(ints = {0, -1})
    void rejectsNonPositiveYear(int year) {
        Set<ConstraintViolation<PastRunningSessionsRequest>> violations = validate(
                new PastRunningSessionsRequest(year, 3, 16, 10, null)
        );

        assertOnlyViolationOn(violations, "year");
    }

    @DisplayName("월이 달력 범위를 벗어나면 DTO 검증이 실패한다")
    @ParameterizedTest(name = "month={0}")
    @ValueSource(ints = {0, 13})
    void rejectsMonthOutsideCalendarRange(int month) {
        Set<ConstraintViolation<PastRunningSessionsRequest>> violations = validate(
                new PastRunningSessionsRequest(2026, month, 16, 10, null)
        );

        assertOnlyViolationOn(violations, "calendarDateValid");
    }

    @DisplayName("일이 달력 범위를 벗어나면 DTO 검증이 실패한다")
    @ParameterizedTest(name = "day={0}")
    @ValueSource(ints = {0, 32})
    void rejectsDayOutsideCalendarRange(int day) {
        Set<ConstraintViolation<PastRunningSessionsRequest>> violations = validate(
                new PastRunningSessionsRequest(2026, 3, day, 10, null)
        );

        assertOnlyViolationOn(violations, "calendarDateValid");
    }

    @DisplayName("pageSize는 양수여야 한다")
    @ParameterizedTest(name = "pageSize={0}")
    @ValueSource(ints = {0, -1})
    void rejectsNonPositivePageSize(int pageSize) {
        Set<ConstraintViolation<PastRunningSessionsRequest>> violations = validate(
                new PastRunningSessionsRequest(null, null, null, pageSize, null)
        );

        assertOnlyViolationOn(violations, "pageSize");
    }

    @DisplayName("연도 없이 월만 있으면 DTO 검증이 실패한다")
    @Test
    void rejectsMonthWithoutYearInsideDto() {
        Set<ConstraintViolation<PastRunningSessionsRequest>> violations = validate(
                new PastRunningSessionsRequest(null, 3, null, 10, null)
        );

        assertOnlyViolationOn(violations, "dateFilterCombinationValid");
    }

    @DisplayName("존재할 수 없는 날짜면 DTO 검증이 실패한다")
    @Test
    void rejectsImpossibleCalendarDateInsideDto() {
        Set<ConstraintViolation<PastRunningSessionsRequest>> violations = validate(
                new PastRunningSessionsRequest(2026, 2, 30, 10, null)
        );

        assertOnlyViolationOn(violations, "calendarDateValid");
    }

    @DisplayName("최대 허용 연도 값 자체는 DTO 검증을 통과한다")
    @Test
    void acceptsMaxYearAtDtoValidationLayer() {
        Set<ConstraintViolation<PastRunningSessionsRequest>> violations = validate(
                new PastRunningSessionsRequest(999999999, null, null, 10, null)
        );

        assertThat(violations).isEmpty();
    }

    @DisplayName("응답 아이템 레코드의 구성 요소를 정확히 노출한다")
    @Test
    void exposesExactItemResponseComponents() {
        assertThat(componentNamesOf(PastRunningSessionItemResponse.class))
                .containsExactly(
                        "runningSessionId",
                        "startedAt",
                        "distanceM",
                        "durationSec",
                        "caloriesKcal",
                        "snapshotImageUrl",
                        "elevationGainM",
                        "completeState",
                        "runningMode",
                        "ghostResultStatus",
                        "timeSlot"
                );
    }

    @DisplayName("과거 러닝 세션 응답은 목록, hasMore, nextCursor를 그대로 담는다")
    @Test
    void storesPagedResponseFields() {
        PastRunningSessionItemResponse item = new PastRunningSessionItemResponse(
                120L,
                LocalDateTime.of(2026, 3, 15, 19, 30),
                7200,
                2100,
                430,
                "https://cdn.withrun.app/snapshots/120.png",
                42,
                RunningSessionCompleteState.SUCCESS,
                RunningMode.GHOST,
                GhostResultStatus.WIN,
                TimeSlot.EVENING
        );
        PastRunningSessionsResponse response = new PastRunningSessionsResponse(List.of(item), true, "next-cursor");

        assertThat(response.items()).containsExactly(item);
        assertThat(response.hasMore()).isTrue();
        assertThat(response.nextCursor()).isEqualTo("next-cursor");
    }

    private Set<ConstraintViolation<PastRunningSessionsRequest>> validate(PastRunningSessionsRequest request) {
        return validator.validate(request);
    }

    private void assertOnlyViolationOn(Set<? extends ConstraintViolation<?>> violations, String propertyName) {
        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains(propertyName);
    }

    private String[] componentNamesOf(Class<?> type) {
        RecordComponent[] components = type.getRecordComponents();
        assertThat(components).isNotNull();
        return java.util.Arrays.stream(components)
                .map(RecordComponent::getName)
                .toArray(String[]::new);
    }
}
