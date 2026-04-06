package kr.withrun.was.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 주간 러닝 요약 응답")
public record UserCalendarWeeklyResponse(
        @Schema(description = "이번 주 총 러닝 횟수(일~토)", example = "4")
        Integer weeklyRunCount,

        @Schema(description = "이번 주 총 러닝 거리(m)", example = "23100")
        Integer weeklyDistanceM,

        @Schema(description = "이번 주 총 소모 칼로리(kcal)", example = "1240")
        Integer weeklyCaloriesKcal,

        @Schema(description = "이번 주 총 운동 시간(초)", example = "5380")
        Integer weeklyDurationSec
) {
}
