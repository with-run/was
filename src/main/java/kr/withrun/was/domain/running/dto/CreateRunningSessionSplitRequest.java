package kr.withrun.was.domain.running.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(description = "러닝 세션 구간 기록 저장 요청")
public record CreateRunningSessionSplitRequest(
        @NotNull
        @PositiveOrZero
        @Schema(description = "1부터 시작하는 구간 순번", example = "1", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer splitIndex,

        @NotNull
        @PositiveOrZero
        @Schema(description = "구간 거리(m)", example = "1000", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer splitDistanceM,

        @NotNull
        @PositiveOrZero
        @Schema(description = "구간 소요 시간(초)", example = "320", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer splitDurationSec,

        @NotNull
        @PositiveOrZero
        @Schema(description = "구간 평균 페이스(초/km)", example = "320", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer splitPaceSecPerKm,

        @PositiveOrZero
        @Schema(description = "구간 평균 심박수(bpm)", example = "152", minimum = "1", nullable = true)
        Integer avgHeartRate,

        @NotNull
        @PositiveOrZero
        @Schema(description = "구간 누적 상승 고도(m)", example = "12", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer elevationGainM
) {
}
