package kr.withrun.was.domain.running.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.withrun.was.domain.running.entity.RunningSessionSplit;

@Schema(description = "저장된 러닝 세션 구간 기록 응답")
public record CreateRunningSessionSplitResponse(
        @Schema(description = "구간 기록 ID", example = "301")
        Long runningSessionSplitId,
        @Schema(description = "소속 러닝 세션 ID", example = "91")
        Long runningSessionId,
        @Schema(description = "구간 순번", example = "1")
        Integer splitIndex,
        @Schema(description = "구간 거리(m)", example = "1000")
        Integer splitDistanceM,
        @Schema(description = "구간 소요 시간(초)", example = "320")
        Integer splitDurationSec,
        @Schema(description = "구간 평균 페이스(초/km)", example = "320")
        Integer splitPaceSecPerKm,
        @Schema(description = "구간 평균 심박수(bpm)", example = "152", nullable = true)
        Integer avgHeartRate,
        @Schema(description = "구간 누적 상승 고도(m)", example = "12")
        Integer elevationGainM
) {
    public static CreateRunningSessionSplitResponse from(RunningSessionSplit runningSessionSplit) {
        return new CreateRunningSessionSplitResponse(
                runningSessionSplit.getId(),
                runningSessionSplit.getRunningSession().getId(),
                runningSessionSplit.getSplitIndex(),
                runningSessionSplit.getSplitDistanceM(),
                runningSessionSplit.getSplitDurationSec(),
                runningSessionSplit.getSplitPaceSecPerKm(),
                runningSessionSplit.getAvgHeartRate(),
                runningSessionSplit.getElevationGainM()
        );
    }
}
