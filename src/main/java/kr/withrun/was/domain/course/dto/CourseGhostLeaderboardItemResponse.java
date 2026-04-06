package kr.withrun.was.domain.course.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "코스 고스트 리더보드 항목")
public record CourseGhostLeaderboardItemResponse(
        @Schema(description = "리더보드 행 ID", example = "101")
        Long leaderboardId,
        @Schema(description = "사용자 ID", example = "7")
        Long userId,
        @Schema(description = "사용자 닉네임", example = "runner-a")
        String nickname,
        @Schema(description = "기록을 만든 러닝 세션 ID", example = "1001")
        Long runningSessionId,
        @Schema(description = "고스트 점수", example = "1250")
        Integer point,
        @Schema(description = "리더보드 순위", example = "1")
        Long rank,
        @Schema(description = "리더보드 생성 시각", example = "2026-03-21T10:30:00")
        LocalDateTime createdAt
) {
}
