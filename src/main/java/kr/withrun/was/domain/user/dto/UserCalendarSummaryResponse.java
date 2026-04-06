package kr.withrun.was.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "사용자 누적 러닝 요약 응답")
public record UserCalendarSummaryResponse(
        @Schema(description = "누적 총 러닝 횟수", example = "128")
        Integer lifetimeRunCount,

        @Schema(description = "누적 총 러닝 거리(m)", example = "742130")
        Integer lifetimeDistanceM,

        @Schema(description = "누적 코스 러닝 횟수", example = "57")
        Integer lifetimeCourseRunCount,

        @Schema(description = "누적 자유 러닝 횟수", example = "44")
        Integer lifetimeFreeRunCount,

        @Schema(description = "누적 고스트 러닝 횟수", example = "27")
        Integer lifetimeGhostRunCount,

        @Schema(description = "오늘까지 연속 러닝 중인 일수", example = "6")
        Integer currentStreakDays,

        @Schema(description = "지금까지 가장 길었던 연속 러닝 일수", example = "14")
        Integer longestStreakDays,

        @Schema(description = "요약 집계 기준 날짜", example = "2026-03-17")
        LocalDate calculatedDate
) {
}
