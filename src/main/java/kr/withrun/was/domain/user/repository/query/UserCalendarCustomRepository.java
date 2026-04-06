package kr.withrun.was.domain.user.repository.query;

import kr.withrun.was.domain.user.entity.UserCalendar;
import kr.withrun.was.domain.user.repository.query.dto.UserCalendarDailySummaryRow;
import kr.withrun.was.domain.user.repository.query.dto.UserCalendarSummaryRow;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserCalendarCustomRepository {

    UserCalendarSummaryRow findSummaryRow(Long userId);

    Optional<UserCalendar> findByUserIdAndCalendarDateAndDeletedAtIsNull(Long userId, LocalDate calendarDate);

    List<UserCalendarDailySummaryRow> findDailySummaries(Long userId, LocalDate startDate, LocalDate endDate);

    List<LocalDate> findRunningDates(Long userId);
}
