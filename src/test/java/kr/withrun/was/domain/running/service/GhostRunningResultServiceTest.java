package kr.withrun.was.domain.running.service;

import kr.withrun.was.domain.running.dto.GhostRunningResultResponse;
import kr.withrun.was.domain.running.repository.GhostRunningResultRepository;
import kr.withrun.was.domain.running.repository.query.dto.GhostRunningResultRow;
import kr.withrun.was.domain.running.type.GhostResultStatus;
import kr.withrun.was.global.common.type.Difficulty;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("고스트 러닝 결과 서비스")
class GhostRunningResultServiceTest {

    @Mock
    private GhostRunningResultRepository ghostRunningResultRepository;

    @DisplayName("고스트 러닝 결과 조회 시 결과 상세를 응답한다")
    @Test
    void returnsGhostRunningResultWhenResultExists() {
        GhostRunningResultService ghostRunningResultService = new GhostRunningResultService(ghostRunningResultRepository);
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 8, 8, 15);
        GhostRunningResultRow ghostRunningResult = new GhostRunningResultRow(
                77L,
                901L,
                345L,
                202L,
                GhostResultStatus.WIN,
                2140,
                14,
                32,
                createdAt
        );
        when(ghostRunningResultRepository.findByRunningSessionId(901L))
                .thenReturn(Optional.of(ghostRunningResult));

        GhostRunningResultResponse response = ghostRunningResultService.findGhostRunningResult(901L);

        assertThat(response).isEqualTo(new GhostRunningResultResponse(
                77L,
                901L,
                345L,
                202L,
                GhostResultStatus.WIN,
                2140,
                14,
                32,
                createdAt
        ));
    }

    @DisplayName("고스트 러닝 결과가 없으면 GHOST_RUNNING_RESULT_NOT_FOUND 예외를 던진다")
    @Test
    void throwsGhostRunningResultNotFoundWhenResultDoesNotExist() {
        GhostRunningResultService ghostRunningResultService = new GhostRunningResultService(ghostRunningResultRepository);
        when(ghostRunningResultRepository.findByRunningSessionId(901L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> ghostRunningResultService.findGhostRunningResult(901L))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.GHOST_RUNNING_RESULT_NOT_FOUND);
    }

    @DisplayName("시간이 빠를수록 점수가 높다")
    @Test
    void returnsHigherPointForFasterDuration() {
        GhostRunningResultService ghostRunningResultService = new GhostRunningResultService(ghostRunningResultRepository);

        Integer fastPoint = ghostRunningResultService.calculateGhostRunningPoint(1_800, Difficulty.MEDIUM);
        Integer slowPoint = ghostRunningResultService.calculateGhostRunningPoint(3_600, Difficulty.MEDIUM);

        assertThat(fastPoint).isGreaterThan(slowPoint);
    }

    @DisplayName("같은 시간이라면 난이도가 높을수록 추가 점수가 부여된다")
    @Test
    void addsMorePointsForHigherDifficultyAtSameDuration() {
        GhostRunningResultService ghostRunningResultService = new GhostRunningResultService(ghostRunningResultRepository);

        Integer easyPoint = ghostRunningResultService.calculateGhostRunningPoint(2_400, Difficulty.EASY);
        Integer mediumPoint = ghostRunningResultService.calculateGhostRunningPoint(2_400, Difficulty.MEDIUM);
        Integer hardPoint = ghostRunningResultService.calculateGhostRunningPoint(2_400, Difficulty.HARD);

        assertThat(easyPoint).isLessThan(mediumPoint);
        assertThat(mediumPoint).isLessThan(hardPoint);
    }

    @DisplayName("120초 미만 기록도 실제 시간 그대로 계산해 더 빠르면 더 높은 점수를 준다")
    @Test
    void calculatesPointUsingActualDurationBelowMinimumThreshold() {
        GhostRunningResultService ghostRunningResultService = new GhostRunningResultService(ghostRunningResultRepository);

        Integer fasterPoint = ghostRunningResultService.calculateGhostRunningPoint(80, Difficulty.HARD);
        Integer slowerPoint = ghostRunningResultService.calculateGhostRunningPoint(120, Difficulty.HARD);

        assertThat(fasterPoint).isGreaterThan(slowerPoint);
    }

    @DisplayName("점수 상한 클램프 없이 3000점을 초과할 수 있다")
    @Test
    void allowsPointsAbove3000WithoutUpperClamp() {
        GhostRunningResultService ghostRunningResultService = new GhostRunningResultService(ghostRunningResultRepository);

        Integer highPoint = ghostRunningResultService.calculateGhostRunningPoint(120, Difficulty.HARD);

        assertThat(highPoint).isGreaterThan(3_000);
    }

    @DisplayName("점수 하한 클램프 없이 100점 미만으로 내려갈 수 있다")
    @Test
    void allowsPointsBelow100WithoutLowerClamp() {
        GhostRunningResultService ghostRunningResultService = new GhostRunningResultService(ghostRunningResultRepository);

        Integer lowPoint = ghostRunningResultService.calculateGhostRunningPoint(30_000, Difficulty.EASY);

        assertThat(lowPoint).isLessThan(100);
    }

    @DisplayName("점수 계산 입력이 유효하지 않으면 INVALID_INPUT_VALUE 예외를 던진다")
    @Test
    void throwsInvalidInputValueWhenPointCalculationInputIsInvalid() {
        GhostRunningResultService ghostRunningResultService = new GhostRunningResultService(ghostRunningResultRepository);

        assertThatThrownBy(() -> ghostRunningResultService.calculateGhostRunningPoint(0, Difficulty.EASY))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.INVALID_INPUT_VALUE);

        assertThatThrownBy(() -> ghostRunningResultService.calculateGhostRunningPoint(null, Difficulty.EASY))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.INVALID_INPUT_VALUE);

        assertThatThrownBy(() -> ghostRunningResultService.calculateGhostRunningPoint(600, null))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.INVALID_INPUT_VALUE);
    }

}
