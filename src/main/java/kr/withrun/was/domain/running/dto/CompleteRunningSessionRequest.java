package kr.withrun.was.domain.running.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import kr.withrun.was.domain.running.type.RunningSessionCompleteState;

import java.util.List;

@Schema(description = "러닝 세션 종료 및 상세 기록 저장 요청")
public record CompleteRunningSessionRequest(
        @Schema(description = "러닝 세션 종료 상태", example = "SUCCESS", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        RunningSessionCompleteState completeState,

        @Schema(description = "최종 러닝 거리(m)", example = "5320", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @PositiveOrZero
        Integer distanceM,

        @Schema(description = "최종 소모 칼로리(kcal)", example = "328", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @PositiveOrZero
        Integer caloriesKcal,

        @Schema(description = "러닝 종료 지점 위도", example = "37.5701", minimum = "-90.0", maximum = "90.0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @DecimalMin(value = "-90.0")
        @DecimalMax(value = "90.0")
        Double endLatitude,

        @Schema(description = "러닝 종료 지점 경도", example = "126.9812", minimum = "-180.0", maximum = "180.0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @DecimalMin(value = "-180.0")
        @DecimalMax(value = "180.0")
        Double endLongitude,

        @Schema(description = "평균 속도(m/s)", example = "2.92", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @PositiveOrZero
        Double avgSpeedMps,

        @Schema(description = "총 러닝 시간(초)", example = "1925", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @PositiveOrZero
        Integer durationSec,

        @Schema(description = "평균 페이스(sec/km)", example = "362", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @PositiveOrZero
        Integer avgPaceSecPerKm,

        @Schema(description = "누적 상승고도(m)", example = "46", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @PositiveOrZero
        Integer elevationGainM,

        @Schema(description = "고스트 러닝 거리 차이(m). 음수/양수 모두 허용하며 고스트 모드가 아니면 0 권장", example = "-32", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        Integer distanceGapM,

        @Schema(description = "GPS 샘플 목록", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        List<@Valid CreateRunningGpsSampleRequest> gpsSamples,

        @Schema(description = "건강 샘플 목록", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        List<@Valid CreateRunningHealthSampleRequest> healthSamples,

        @Schema(description = "스플릿 목록", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        List<@Valid CreateRunningSessionSplitRequest> splits
) {
}
