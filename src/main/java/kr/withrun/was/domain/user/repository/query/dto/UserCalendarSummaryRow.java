package kr.withrun.was.domain.user.repository.query.dto;

public record UserCalendarSummaryRow(
        Integer lifetimeRunCount,
        Integer lifetimeDistanceM,
        Integer lifetimeCourseRunCount,
        Integer lifetimeFreeRunCount,
        Integer lifetimeGhostRunCount
) {
}
