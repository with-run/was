package kr.withrun.was.domain.running.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.withrun.was.domain.running.repository.query.dto.GhostRunningResultRow;
import kr.withrun.was.domain.running.type.GhostResultStatus;

import java.time.LocalDateTime;

@Schema(description = "고스트 러닝 결과 조회 응답")
public record GhostRunningResultResponse(
        @Schema(description = "고스트 러닝 결과 ID", example = "77")
        Long ghostRunningResultId,

        @Schema(description = "결과가 저장된 러닝 세션 ID", example = "901")
        Long runningSessionId,

        @Schema(description = "경쟁 대상 러닝 세션 ID", example = "345")
        Long ghostTargetRunningSessionId,

        @Schema(description = "경쟁 대상 사용자 ID", example = "202", nullable = true)
        Long targetUserId,

        @Schema(description = "고스트 러닝 결과 상태")
        GhostResultStatus resultStatus,

        @Schema(description = "고스트 러닝 결과 점수", example = "2140")
        Integer point,

        @Schema(description = "시간 차이(초)", example = "14")
        Integer timeGapSec,

        @Schema(description = "거리 차이(미터)", example = "32")
        Integer distanceGapM,

        @Schema(description = "결과 생성 시각", example = "2026-03-08T08:15:00")
        LocalDateTime createdAt
) {

    public static GhostRunningResultResponse from(GhostRunningResultRow ghostRunningResult) {
        return new GhostRunningResultResponse(
                ghostRunningResult.ghostRunningResultId(),
                ghostRunningResult.runningSessionId(),
                ghostRunningResult.ghostTargetRunningSessionId(),
                ghostRunningResult.targetUserId(),
                ghostRunningResult.resultStatus(),
                ghostRunningResult.point(),
                ghostRunningResult.timeGapSec(),
                ghostRunningResult.distanceGapM(),
                ghostRunningResult.createdAt()
        );
    }

}
