package kr.withrun.was.domain.running.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.withrun.was.domain.running.entity.RunningSessionSplit;

@Schema(description = "러닝 구간 스플릿 응답")
public record RunningSessionSplitResponse(
        @Schema(description = "구간 순번", example = "1")
        Integer splitIndex,

        @Schema(description = "구간 거리(m)", example = "1000")
        Integer splitDistanceM,

        @Schema(description = "구간 소요 시간(초)", example = "268")
        Integer splitDurationSec,

        @Schema(description = "구간 페이스(초/km)", example = "268")
        Integer splitPaceSecPerKm,

        @Schema(description = "구간 평균 심박수(bpm)", example = "158", nullable = true)
        Integer avgHeartRate,

        @Schema(description = "구간 상승 고도(m)", example = "12")
        Integer elevationGainM
) {
    public static RunningSessionSplitResponse from(RunningSessionSplit runningSessionSplit) {
        return new RunningSessionSplitResponse(
                runningSessionSplit.getSplitIndex(),
                runningSessionSplit.getSplitDistanceM(),
                runningSessionSplit.getSplitDurationSec(),
                runningSessionSplit.getSplitPaceSecPerKm(),
                runningSessionSplit.getAvgHeartRate(),
                runningSessionSplit.getElevationGainM()
        );
    }
}
