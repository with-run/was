package kr.withrun.was.domain.running.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import kr.withrun.was.domain.running.entity.RunningSession;
import kr.withrun.was.domain.running.type.RunningSessionCompleteState;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "러닝 세션 상세 응답")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RunningSessionDetailResponse(
        @JsonInclude(JsonInclude.Include.ALWAYS)
        @Schema(description = "러닝 세션 종료 상태. 시작 직후 기본값은 FAIL 이고 종료 요청으로 최종 상태가 반영된다.", example = "SUCCESS")
        RunningSessionCompleteState completeState,

        @Schema(description = "러닝 세션 ID", example = "91")
        Long runningSessionId,

        @Schema(description = "러닝 세션 소유 사용자 ID", example = "1")
        Long userId,

        @Schema(description = "세션 스냅샷 이미지 URL", example = "https://cdn.withrun.app/snapshots/91.png", nullable = true)
        String snapshotImageUrl,

        @Schema(description = "러닝 시작 시각", example = "2026-03-12T06:30:00")
        LocalDateTime startedAt,

        @Schema(description = "러닝 종료 시각. 진행 중 세션이면 null 이다.", example = "2026-03-12T07:12:00", nullable = true)
        LocalDateTime endedAt,

        @Schema(description = "총 이동 거리(m)", example = "10023")
        Integer distanceM,

        @Schema(description = "총 러닝 시간(초). 진행 중 세션이면 null 일 수 있다.", example = "2520", nullable = true)
        Integer durationSec,

        @Schema(description = "평균 속도(m/s). 진행 중 세션이면 null 일 수 있다.", example = "3.98", nullable = true)
        Double avgSpeedMps,

        @Schema(description = "평균 페이스(초/km). 진행 중 세션이면 null 일 수 있다.", example = "251", nullable = true)
        Integer avgPaceSecPerKm,

        @Schema(description = "총 소모 칼로리(kcal)", example = "642")
        Integer caloriesKcal,

        @Schema(description = "총 상승 고도(m)", example = "73")
        Integer elevationGainM,

        @Schema(description = "시간순 GPS 샘플 목록")
        List<RunningGpsSampleResponse> gpsSamples,

        @Schema(description = "시간순 건강 샘플 목록")
        List<RunningHealthSampleResponse> healthSamples,

        @Schema(description = "구간별 스플릿 기록 목록")
        List<RunningSessionSplitResponse> splits,

        @Schema(description = "고스트 러닝 결과. 일반 러닝이면 null 이다.", nullable = true)
        GhostRunningResultResponse ghostRunningResult
) {
    public static RunningSessionDetailResponse of(
            RunningSession runningSession,
            List<RunningGpsSampleResponse> gpsSamples,
            List<RunningHealthSampleResponse> healthSamples,
            List<RunningSessionSplitResponse> splits
    ) {
        return of(runningSession, gpsSamples, healthSamples, splits, runningSession.getSnapshotImageUrl(), null);
    }

    public static RunningSessionDetailResponse of(
            RunningSession runningSession,
            List<RunningGpsSampleResponse> gpsSamples,
            List<RunningHealthSampleResponse> healthSamples,
            List<RunningSessionSplitResponse> splits,
            GhostRunningResultResponse ghostRunningResult
    ) {
        return of(
                runningSession,
                gpsSamples,
                healthSamples,
                splits,
                runningSession.getSnapshotImageUrl(),
                ghostRunningResult
        );
    }

    public static RunningSessionDetailResponse of(
            RunningSession runningSession,
            List<RunningGpsSampleResponse> gpsSamples,
            List<RunningHealthSampleResponse> healthSamples,
            List<RunningSessionSplitResponse> splits,
            String snapshotImageUrl,
            GhostRunningResultResponse ghostRunningResult
    ) {
        return new RunningSessionDetailResponse(
                runningSession.getCompleteState(),
                runningSession.getId(),
                runningSession.getUser().getId(),
                snapshotImageUrl,
                runningSession.getStartedAt(),
                runningSession.getEndedAt(),
                runningSession.getDistanceM(),
                runningSession.getDurationSec(),
                runningSession.getAvgSpeedMps(),
                runningSession.getAvgPaceSecPerKm(),
                runningSession.getCaloriesKcal(),
                runningSession.getElevationGainM(),
                gpsSamples,
                healthSamples,
                splits,
                ghostRunningResult
        );
    }
}
