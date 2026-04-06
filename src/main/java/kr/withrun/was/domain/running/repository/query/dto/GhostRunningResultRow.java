package kr.withrun.was.domain.running.repository.query.dto;

import kr.withrun.was.domain.running.type.GhostResultStatus;

import java.time.LocalDateTime;

public record GhostRunningResultRow(
        Long ghostRunningResultId,
        Long runningSessionId,
        Long ghostTargetRunningSessionId,
        Long targetUserId,
        GhostResultStatus resultStatus,
        Integer point,
        Integer timeGapSec,
        Integer distanceGapM,
        LocalDateTime createdAt
) {
}
