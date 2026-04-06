package kr.withrun.was.domain.running.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import kr.withrun.was.domain.running.type.RunningMode;
import kr.withrun.was.domain.running.type.RunningSessionCompleteState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("러닝 세션 요청 DTO")
class RunningSessionRequestDtoTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @DisplayName("세션 생성 요청은 현재 사용자 userId를 노출하지 않는다")
    @Test
    void createRequestDoesNotExposeUserIdComponent() {
        assertThat(componentNamesOf(CreateRunningSessionRequest.class))
                .containsExactly("mode", "courseId", "ghostTargetRunningSessionId", "startLatitude", "startLongitude");
    }

    @DisplayName("유효한 세션 생성 요청은 검증을 통과한다")
    @Test
    void acceptsValidCreateRequest() {
        Set<ConstraintViolation<CreateRunningSessionRequest>> violations = validator.validate(
                new CreateRunningSessionRequest(RunningMode.COURSE, 12L, null, 37.5665, 126.9780)
        );

        assertThat(violations).isEmpty();
    }

    @DisplayName("세션 종료 요청과 스플릿 요청은 현재 사용자 userId를 노출하지 않는다")
    @Test
    void completeRequestAndSplitRequestDoNotExposeUserIdComponents() {
        assertThat(componentNamesOf(CompleteRunningSessionRequest.class))
                .containsExactly(
                        "completeState",
                        "distanceM",
                        "caloriesKcal",
                        "endLatitude",
                        "endLongitude",
                        "avgSpeedMps",
                        "durationSec",
                        "avgPaceSecPerKm",
                        "elevationGainM",
                        "distanceGapM",
                        "gpsSamples",
                        "healthSamples",
                        "splits"
                );
        assertThat(componentNamesOf(CreateRunningSessionSplitRequest.class))
                .containsExactly(
                        "splitIndex",
                        "splitDistanceM",
                        "splitDurationSec",
                        "splitPaceSecPerKm",
                        "avgHeartRate",
                        "elevationGainM"
                );
    }

    @DisplayName("유효한 세션 종료 요청은 검증을 통과한다")
    @Test
    void acceptsValidCompleteRequest() {
        CompleteRunningSessionRequest request = new CompleteRunningSessionRequest(
                RunningSessionCompleteState.SUCCESS,
                5320,
                328,
                37.5701,
                126.9812,
                2.92,
                1925,
                362,
                46,
                0,
                List.of(new CreateRunningGpsSampleRequest(
                        37.5665,
                        126.9780,
                        21.5,
                        5.2,
                        182.0,
                        3.45,
                        289,
                        1250,
                        (short) 174,
                        LocalDateTime.of(2026, 3, 18, 6, 31, 30)
                )),
                List.of(new CreateRunningHealthSampleRequest(
                        LocalDateTime.of(2026, 3, 18, 6, 31, 30),
                        (short) 152,
                        84.5
                )),
                List.of(new CreateRunningSessionSplitRequest(
                        1,
                        1000,
                        320,
                        320,
                        152,
                        12
                ))
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    @DisplayName("세션 종료 요청의 distanceGapM 은 음수도 허용한다")
    @Test
    void acceptsNegativeDistanceGapM() {
        CompleteRunningSessionRequest request = new CompleteRunningSessionRequest(
                RunningSessionCompleteState.SUCCESS,
                5320,
                328,
                37.5701,
                126.9812,
                2.92,
                1925,
                362,
                46,
                -32,
                List.of(),
                List.of(),
                List.of()
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    private String[] componentNamesOf(Class<?> type) {
        RecordComponent[] components = type.getRecordComponents();
        assertThat(components).isNotNull();
        return java.util.Arrays.stream(components)
                .map(RecordComponent::getName)
                .toArray(String[]::new);
    }
}
