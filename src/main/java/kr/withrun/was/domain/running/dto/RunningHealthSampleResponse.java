package kr.withrun.was.domain.running.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.withrun.was.domain.running.entity.RunningHealthSample;

import java.time.LocalDateTime;

@Schema(description = "단일 건강 샘플 응답")
public record RunningHealthSampleResponse(
        @Schema(description = "샘플 수집 시각", example = "2026-03-18T06:31:30")
        LocalDateTime sampledAt,

        @Schema(description = "심박수(bpm)", example = "152", nullable = true)
        Short heartRate,

        @Schema(description = "누적 소모 칼로리(kcal)", example = "84.5", nullable = true)
        Double caloriesKcal
) {
    public static RunningHealthSampleResponse from(RunningHealthSample runningHealthSample) {
        return new RunningHealthSampleResponse(
                runningHealthSample.getSampledAt(),
                runningHealthSample.getHeartRate(),
                runningHealthSample.getCaloriesKcal()
        );
    }
}
