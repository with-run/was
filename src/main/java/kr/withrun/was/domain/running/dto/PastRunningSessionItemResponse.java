package kr.withrun.was.domain.running.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.withrun.was.domain.running.repository.query.dto.PastRunningSessionHistoryRow;
import kr.withrun.was.domain.running.type.GhostResultStatus;
import kr.withrun.was.domain.running.type.RunningMode;
import kr.withrun.was.domain.running.type.RunningSessionCompleteState;
import kr.withrun.was.global.common.type.TimeSlot;

import java.time.LocalDateTime;

@Schema(description = "과거 러닝 세션 목록의 단일 항목")
public record PastRunningSessionItemResponse(
        @Schema(description = "러닝 세션 ID", example = "120")
        Long runningSessionId,
        @Schema(description = "러닝 시작 시각", example = "2026-03-15T19:30:00")
        LocalDateTime startedAt,
        @Schema(description = "러닝 거리(m)", example = "7200")
        Integer distanceM,
        @Schema(description = "러닝 지속 시간(초)", example = "2100")
        Integer durationSec,
        @Schema(description = "소모 칼로리(kcal)", example = "430")
        Integer caloriesKcal,
        @Schema(description = "러닝 스냅샷 이미지 URL", example = "https://cdn.withrun.app/snapshots/120.png", nullable = true)
        String snapshotImageUrl,
        @Schema(description = "누적 상승 고도(m)", example = "42", nullable = true)
        Integer elevationGainM,
        @Schema(description = "러닝 세션 종료 상태", example = "SUCCESS")
        RunningSessionCompleteState completeState,
        @Schema(description = "러닝 모드", example = "GHOST")
        RunningMode runningMode,
        @Schema(description = "고스트 러닝 결과 상태. 고스트 모드가 아니면 null 일 수 있다.", example = "WIN", nullable = true)
        GhostResultStatus ghostResultStatus,
        @Schema(description = "러닝 시작 시각 기준 시간대 구분", example = "EVENING")
        TimeSlot timeSlot
) {

    public static PastRunningSessionItemResponse from(PastRunningSessionHistoryRow row) {
        return from(row, row.snapshotImageUrl());
    }

    public static PastRunningSessionItemResponse from(PastRunningSessionHistoryRow row, String snapshotImageUrl) {
        return new PastRunningSessionItemResponse(
                row.runningSessionId(),
                row.startedAt(),
                row.distanceM(),
                row.durationSec(),
                row.caloriesKcal(),
                snapshotImageUrl,
                row.elevationGainM(),
                row.completeState(),
                row.runningMode(),
                row.ghostResultStatus(),
                TimeSlot.from(row.startedAt().toLocalTime())
        );
    }
}
