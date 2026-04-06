package kr.withrun.was.domain.user.service;

import kr.withrun.was.domain.user.dto.UserCalendarMonthlyResponse;
import kr.withrun.was.domain.user.dto.UserCalendarSummaryResponse;
import kr.withrun.was.domain.user.dto.UserCalendarWeeklyResponse;
import kr.withrun.was.domain.user.repository.UserCalendarRepository;
import kr.withrun.was.domain.user.repository.UserRepository;
import kr.withrun.was.domain.user.repository.query.dto.UserCalendarDailySummaryRow;
import kr.withrun.was.domain.user.repository.query.dto.UserCalendarSummaryRow;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserCalendarService {

    private final UserRepository userRepository;
    private final UserCalendarRepository userCalendarRepository;

    public UserCalendarMonthlyResponse findMonthlyCalendar(Long userId, Integer year, Integer month) {
        getUser(userId);

        YearMonth yearMonth;
        try {
            yearMonth = YearMonth.of(year, month);
        } catch (DateTimeException exception) {
            throw new CustomException(ResponseCode.INVALID_INPUT_VALUE);
        }
        List<UserCalendarDailySummaryRow> dailySummaryRows = userCalendarRepository.findDailySummaries(
                userId,
                yearMonth.atDay(1),
                yearMonth.atEndOfMonth()
        );
        List<UserCalendarMonthlyResponse.DailySummary> dailySummaries = dailySummaryRows.stream()
                .map(UserCalendarMonthlyResponse.DailySummary::from)
                .toList();

        return UserCalendarMonthlyResponse.from(year, month, dailySummaries);
    }

    public UserCalendarSummaryResponse findSummary(Long userId) {
        getUser(userId);

        LocalDate calculatedDate = LocalDate.now();
        UserCalendarSummaryRow summaryRow = userCalendarRepository.findSummaryRow(userId);
        List<LocalDate> runningDates = userCalendarRepository.findRunningDates(userId);

        return new UserCalendarSummaryResponse(
                summaryRow.lifetimeRunCount(),
                summaryRow.lifetimeDistanceM(),
                summaryRow.lifetimeCourseRunCount(),
                summaryRow.lifetimeFreeRunCount(),
                summaryRow.lifetimeGhostRunCount(),
                calculateCurrentStreakDays(runningDates, calculatedDate),
                calculateLongestStreakDays(runningDates),
                calculatedDate
        );
    }

    public UserCalendarWeeklyResponse findWeeklySummary(Long userId) {
        getUser(userId);

        LocalDate today = LocalDate.now();
        LocalDate startDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        LocalDate endDate = startDate.plusDays(6);

        List<UserCalendarDailySummaryRow> dailySummaryRows = userCalendarRepository.findDailySummaries(
                userId,
                startDate,
                endDate
        );

        int weeklyRunCount = 0;
        int weeklyDistanceM = 0;
        int weeklyCaloriesKcal = 0;
        int weeklyDurationSec = 0;

        for (UserCalendarDailySummaryRow dailySummaryRow : dailySummaryRows) {
            weeklyRunCount += dailySummaryRow.dailyCourseRunCount()
                    + dailySummaryRow.dailyFreeRunCount()
                    + dailySummaryRow.dailyGhostRunCount();
            weeklyDistanceM += dailySummaryRow.dailyDistanceM();
            weeklyCaloriesKcal += dailySummaryRow.dailyCaloriesKcal();
            weeklyDurationSec += dailySummaryRow.dailyDurationSec();
        }

        return new UserCalendarWeeklyResponse(
                weeklyRunCount,
                weeklyDistanceM,
                weeklyCaloriesKcal,
                weeklyDurationSec
        );
    }

    private void getUser(Long userId) {
        userRepository.findNotDeletedUser(userId)
                .orElseThrow(() -> new CustomException(ResponseCode.USER_NOT_FOUND));
    }

    private int calculateCurrentStreakDays(List<LocalDate> runningDates, LocalDate calculatedDate) {
        if (runningDates.isEmpty() || !runningDates.getFirst().isEqual(calculatedDate)) {
            return 0;
        }

        int streakDays = 0;
        LocalDate expectedDate = calculatedDate;

        for (LocalDate runningDate : runningDates) {
            if (!runningDate.isEqual(expectedDate)) {
                break;
            }

            streakDays++;
            expectedDate = expectedDate.minusDays(1);
        }

        return streakDays;
    }

    private int calculateLongestStreakDays(List<LocalDate> runningDates) {
        if (runningDates.isEmpty()) {
            return 0;
        }

        int longestStreakDays = 1;
        int currentStreakDays = 1;
        LocalDate previousDate = runningDates.getFirst();

        for (int index = 1; index < runningDates.size(); index++) {
            LocalDate runningDate = runningDates.get(index);
            if (runningDate.isEqual(previousDate.minusDays(1))) {
                currentStreakDays++;
            } else {
                currentStreakDays = 1;
            }

            longestStreakDays = Math.max(longestStreakDays, currentStreakDays);
            previousDate = runningDate;
        }

        return longestStreakDays;
    }
}
