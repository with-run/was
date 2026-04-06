package kr.withrun.was.domain.running.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

@Schema(description = "단일 GPS 샘플")
public record CreateRunningGpsSampleRequest (
        @NotNull
        @Schema(description = "위도", example = "37.5665", minimum = "-90.0", maximum = "90.0", requiredMode = Schema.RequiredMode.REQUIRED)
        Double latitude,

        @NotNull
        @Schema(description = "경도", example = "126.9780", minimum = "-180.0", maximum = "180.0", requiredMode = Schema.RequiredMode.REQUIRED)
        Double longitude,

        @NotNull
        @Schema(description = "고도(m)", example = "21.5", requiredMode = Schema.RequiredMode.REQUIRED)
        Double altitudeM,

        @NotNull
        @Schema(description = "위치 정확도(m)", example = "5.2", requiredMode = Schema.RequiredMode.REQUIRED)
        Double accuracyM,

        @NotNull
        @Schema(description = "방위각(도)", example = "182.0", requiredMode = Schema.RequiredMode.REQUIRED)
        Double bearingDeg,

        @Schema(description = "순간 속도(m/s)", example = "3.45", nullable = true)
        Double speedMps,

        @Schema(description = "순간 페이스(초/km)", example = "289", nullable = true)
        Integer paceSecPerKm,

        @Schema(description = "누적 거리(m)", example = "1250", nullable = true)
        Integer distanceM,

        @Schema(description = "케이던스(spm)", example = "174", nullable = true)
        Short cadenceSpm,

        @NotNull
        @Schema(description = "샘플 수집 시각", example = "2026-03-18T06:31:30", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDateTime sampledAt
 ) {

}
