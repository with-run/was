package kr.withrun.was.domain.reward.service;

import kr.withrun.was.domain.reward.entity.RewardPointBalance;
import kr.withrun.was.domain.reward.entity.RewardPointHistory;
import kr.withrun.was.domain.reward.repository.RewardPointBalanceRepository;
import kr.withrun.was.domain.reward.repository.RewardPointHistoryRepository;
import kr.withrun.was.domain.reward.type.RewardPointReason;
import kr.withrun.was.domain.running.entity.GhostRunningResult;
import kr.withrun.was.domain.running.entity.RunningSession;
import kr.withrun.was.domain.running.type.GhostResultStatus;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.entity.UserCalendar;
import kr.withrun.was.domain.user.repository.UserCalendarRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class RewardPointGrantService {

    private static final int DAILY_RUNNING_THRESHOLD_M = 1_000;
    private static final int REWARD_POINT = 1;
    private static final List<WeeklyMilestone> WEEKLY_MILESTONES = List.of(
            new WeeklyMilestone(3, RewardPointReason.WEEKLY_STREAK_3_DAYS),
            new WeeklyMilestone(5, RewardPointReason.WEEKLY_STREAK_5_DAYS),
            new WeeklyMilestone(7, RewardPointReason.WEEKLY_STREAK_7_DAYS)
    );

    private final RewardPointBalanceRepository rewardPointBalanceRepository;
    private final RewardPointHistoryRepository rewardPointHistoryRepository;
    private final UserCalendarRepository userCalendarRepository;

    public RewardPointGrantService(
            RewardPointBalanceRepository rewardPointBalanceRepository,
            RewardPointHistoryRepository rewardPointHistoryRepository,
            UserCalendarRepository userCalendarRepository
    ) {
        this.rewardPointBalanceRepository = rewardPointBalanceRepository;
        this.rewardPointHistoryRepository = rewardPointHistoryRepository;
        this.userCalendarRepository = userCalendarRepository;
    }

    @Transactional
    public void grantCompletedRunningRewards(
            RunningSession runningSession,
            UserCalendar userCalendar,
            GhostRunningResult ghostRunningResult
    ) {
        User user = runningSession.getUser();
        LocalDate calendarDate = userCalendar.getCalendarDate();

        if (userCalendar.getTotalDistanceM() >= DAILY_RUNNING_THRESHOLD_M) {
            grantEarnedPointIfAbsent(
                    user,
                    RewardPointReason.DAILY_RUNNING,
                    buildDailyRunningKey(user.getId(), calendarDate)
            );
        }

        grantWeeklyStreakRewards(user, calendarDate);

        if (ghostRunningResult != null && ghostRunningResult.getResultStatus() == GhostResultStatus.WIN) {
            grantEarnedPointIfAbsent(
                    user,
                    RewardPointReason.GHOST_WIN,
                    buildGhostWinKey(user.getId(), runningSession.getId())
            );
        }
    }

    private void grantWeeklyStreakRewards(User user, LocalDate calendarDate) {
        LocalDate weekStart = calendarDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        LocalDate weekEnd = weekStart.plusDays(6);
        int runningDaysInWeek = userCalendarRepository.findDailySummaries(user.getId(), weekStart, weekEnd).size();

        for (WeeklyMilestone weeklyMilestone : WEEKLY_MILESTONES) {
            if (runningDaysInWeek >= weeklyMilestone.dayCount()) {
                grantEarnedPointIfAbsent(
                        user,
                        weeklyMilestone.reason(),
                        buildWeeklyStreakKey(user.getId(), weekStart, weeklyMilestone.dayCount())
                );
            }
        }
    }

    private void grantEarnedPointIfAbsent(User user, RewardPointReason reason, String idempotencyKey) {
        if (rewardPointHistoryRepository.existsByUserIdAndIdempotencyKeyAndDeletedAtIsNull(user.getId(), idempotencyKey)) {
            return;
        }

        ensureBalanceExists(user);

        try {
            rewardPointHistoryRepository.saveAndFlush(RewardPointHistory.create(user, REWARD_POINT, reason, idempotencyKey));
        } catch (DataIntegrityViolationException exception) {
            if (rewardPointHistoryRepository.existsByUserIdAndIdempotencyKeyAndDeletedAtIsNull(user.getId(), idempotencyKey)) {
                return;
            }
            throw exception;
        }

        rewardPointBalanceRepository.increaseBalance(user.getId(), REWARD_POINT);
    }

    private void ensureBalanceExists(User user) {
        if (rewardPointBalanceRepository.findByIdAndDeletedAtIsNull(user.getId()).isPresent()) {
            return;
        }

        try {
            rewardPointBalanceRepository.saveAndFlush(RewardPointBalance.create(user));
        } catch (DataIntegrityViolationException exception) {
            if (rewardPointBalanceRepository.findByIdAndDeletedAtIsNull(user.getId()).isPresent()) {
                return;
            }
            throw exception;
        }
    }

    private String buildDailyRunningKey(Long userId, LocalDate calendarDate) {
        return "reward:daily-running:%d:%s".formatted(userId, calendarDate);
    }

    private String buildWeeklyStreakKey(Long userId, LocalDate weekStart, int milestoneDays) {
        return "reward:weekly-streak:%d:%s:%d".formatted(userId, weekStart, milestoneDays);
    }

    private String buildGhostWinKey(Long userId, Long runningSessionId) {
        return "reward:ghost-win:%d:%d".formatted(userId, runningSessionId);
    }

    private record WeeklyMilestone(int dayCount, RewardPointReason reason) {
    }
}
