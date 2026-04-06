package kr.withrun.was.domain.course.repository.query.dto;

import java.time.LocalDateTime;

public record CourseGhostLeaderboardRankRow(
        Long leaderboardId,
        Long userId,
        String nickname,
        Long runningSessionId,
        Integer point,
        Long rank,
        LocalDateTime createdAt
) {
}
