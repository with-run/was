package kr.withrun.was.domain.running.repository.query.dto;

import kr.withrun.was.domain.running.type.GhostResultStatus;
import kr.withrun.was.domain.running.type.RunningMode;
import kr.withrun.was.domain.running.type.RunningSessionCompleteState;

import java.time.LocalDateTime;

public record PastRunningSessionHistoryRow(
        Long runningSessionId,
        LocalDateTime startedAt,
        Integer distanceM,
        Integer durationSec,
        Integer caloriesKcal,
        String snapshotImageUrl,
        Integer elevationGainM,
        RunningSessionCompleteState completeState,
        RunningMode runningMode,
        GhostResultStatus ghostResultStatus
) {
}
