package kr.withrun.was.domain.user.repository.query.dto;

import java.time.LocalDate;

public record UserCalendarDailySummaryRow(
        LocalDate calendarDate,
        Integer dailyDistanceM,
        Integer dailyDurationSec,
        Integer dailyCaloriesKcal,
        Integer dailyCourseRunCount,
        Integer dailyFreeRunCount,
        Integer dailyGhostRunCount
) {
}
