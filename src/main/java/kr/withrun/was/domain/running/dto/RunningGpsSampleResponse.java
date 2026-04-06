package kr.withrun.was.domain.running.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.withrun.was.domain.running.entity.RunningGpsSample;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Schema(description = "단일 GPS 샘플 응답")
public record RunningGpsSampleResponse(
        @Schema(description = "샘플 수집 시각", example = "2026-03-18T06:31:30")
        LocalDateTime sampledAt,

        @Schema(description = "첫 GPS 샘플 기준 경과 시간(초)", example = "10")
        Integer time,

        @Schema(description = "위도", example = "37.5665")
        Double latitude,

        @Schema(description = "경도", example = "126.9780")
        Double longitude,

        @Schema(description = "고도(m)", example = "21.5")
        Double altitudeM,

        @Schema(description = "위치 정확도(m)", example = "5.2")
        Double accuracyM,

        @Schema(description = "방위각(도)", example = "182.0")
        Double bearingDeg,

        @Schema(description = "순간 속도(m/s)", example = "3.45", nullable = true)
        Double speedMps,

        @Schema(description = "순간 페이스(초/km)", example = "289", nullable = true)
        Integer paceSecPerKm,

        @Schema(description = "누적 거리(m)", example = "1250", nullable = true)
        Integer distanceM,

        @Schema(description = "케이던스(spm)", example = "174", nullable = true)
        Short cadenceSpm
) {
    public static List<RunningGpsSampleResponse> from(List<RunningGpsSample> runningGpsSamples) {
        if (runningGpsSamples == null || runningGpsSamples.isEmpty()) {
            return List.of();
        }

        List<RunningGpsSample> orderedSamples = runningGpsSamples.stream()
                .sorted(Comparator.comparing(RunningGpsSample::getSampledAt))
                .toList();
        LocalDateTime firstSampledAt = orderedSamples.getFirst().getSampledAt();

        return orderedSamples.stream()
                .map(runningGpsSample -> from(runningGpsSample, firstSampledAt))
                .toList();
    }

    private static RunningGpsSampleResponse from(RunningGpsSample runningGpsSample, LocalDateTime firstSampledAt) {
        return new RunningGpsSampleResponse(
                runningGpsSample.getSampledAt(),
                Math.toIntExact(ChronoUnit.SECONDS.between(firstSampledAt, runningGpsSample.getSampledAt())),
                runningGpsSample.getLatitude(),
                runningGpsSample.getLongitude(),
                runningGpsSample.getAltitudeM(),
                runningGpsSample.getAccuracyM(),
                runningGpsSample.getBearingDeg(),
                runningGpsSample.getSpeedMps(),
                runningGpsSample.getPaceSecPerKm(),
                runningGpsSample.getDistanceM(),
                runningGpsSample.getCadenceSpm()
        );
    }
}
