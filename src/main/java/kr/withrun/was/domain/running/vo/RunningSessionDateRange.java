package kr.withrun.was.domain.running.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record RunningSessionDateRange(
        LocalDateTime startInclusive,
        LocalDateTime endExclusive
) {

    public static RunningSessionDateRange of(Integer year, Integer month, Integer day) {
        if (year == null) {
            return new RunningSessionDateRange(null, null);
        }

        if (month == null) {
            LocalDate startDate = LocalDate.of(year, 1, 1);
            return new RunningSessionDateRange(startDate.atStartOfDay(), startDate.plusYears(1).atStartOfDay());
        }
        if (day == null) {
            LocalDate startDate = LocalDate.of(year, month, 1);
            return new RunningSessionDateRange(startDate.atStartOfDay(), startDate.plusMonths(1).atStartOfDay());
        }

        LocalDate startDate = LocalDate.of(year, month, day);
        return new RunningSessionDateRange(startDate.atStartOfDay(), startDate.plusDays(1).atStartOfDay());
    }
}
