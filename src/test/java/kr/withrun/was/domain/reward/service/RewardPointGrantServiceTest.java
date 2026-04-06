package kr.withrun.was.domain.reward.service;

import kr.withrun.was.domain.reward.entity.RewardPointBalance;
import kr.withrun.was.domain.reward.entity.RewardPointHistory;
import kr.withrun.was.domain.reward.repository.RewardPointBalanceRepository;
import kr.withrun.was.domain.reward.repository.RewardPointHistoryRepository;
import kr.withrun.was.domain.reward.type.RewardPointReason;
import kr.withrun.was.domain.running.entity.GhostRunningResult;
import kr.withrun.was.domain.running.entity.RunningSession;
import kr.withrun.was.domain.running.type.GhostResultStatus;
import kr.withrun.was.domain.running.type.RunningMode;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.entity.UserCalendar;
import kr.withrun.was.domain.user.repository.UserCalendarRepository;
import kr.withrun.was.domain.user.repository.query.dto.UserCalendarDailySummaryRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("리워드 포인트 지급 서비스")
class RewardPointGrantServiceTest {

    @Mock
    private RewardPointBalanceRepository rewardPointBalanceRepository;

    @Mock
    private RewardPointHistoryRepository rewardPointHistoryRepository;

    @Mock
    private UserCalendarRepository userCalendarRepository;

    private RewardPointGrantService rewardPointGrantService;

    @BeforeEach
    void setUp() {
        rewardPointGrantService = new RewardPointGrantService(
                rewardPointBalanceRepository,
                rewardPointHistoryRepository,
                userCalendarRepository
        );
    }

    @DisplayName("하루 누적 1km 이상이면 일일 러닝 포인트를 지급한다")
    @Test
    void grantsDailyRunningPointWhenThresholdIsMet() {
        User user = user(1L);
        RunningSession runningSession = runningSession(101L, user);
        UserCalendar userCalendar = userCalendar(user, LocalDate.of(2026, 3, 29), 1_000);
        RewardPointBalance rewardPointBalance = RewardPointBalance.create(user);

        when(rewardPointHistoryRepository.existsByUserIdAndIdempotencyKeyAndDeletedAtIsNull(
                1L,
                "reward:daily-running:1:2026-03-29"
        )).thenReturn(false);
        when(rewardPointBalanceRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(rewardPointBalance));
        when(userCalendarRepository.findDailySummaries(1L, LocalDate.of(2026, 3, 29), LocalDate.of(2026, 4, 4)))
                .thenReturn(List.of(new UserCalendarDailySummaryRow(LocalDate.of(2026, 3, 29), 1_000, 600, 80, 0, 1, 0)));

        rewardPointGrantService.grantCompletedRunningRewards(runningSession, userCalendar, null);

        ArgumentCaptor<RewardPointHistory> historyCaptor = ArgumentCaptor.forClass(RewardPointHistory.class);
        verify(rewardPointHistoryRepository).saveAndFlush(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getReason()).isEqualTo(RewardPointReason.DAILY_RUNNING);
        assertThat(historyCaptor.getValue().getIdempotencyKey()).isEqualTo("reward:daily-running:1:2026-03-29");
        verify(rewardPointBalanceRepository).increaseBalance(1L, 1);
    }

    @DisplayName("같은 날짜의 일일 러닝 포인트는 중복 지급하지 않는다")
    @Test
    void skipsDailyRunningPointWhenHistoryAlreadyExists() {
        User user = user(2L);
        RunningSession runningSession = runningSession(102L, user);
        UserCalendar userCalendar = userCalendar(user, LocalDate.of(2026, 3, 30), 1_500);

        when(rewardPointHistoryRepository.existsByUserIdAndIdempotencyKeyAndDeletedAtIsNull(
                2L,
                "reward:daily-running:2:2026-03-30"
        )).thenReturn(true);
        when(userCalendarRepository.findDailySummaries(2L, LocalDate.of(2026, 3, 29), LocalDate.of(2026, 4, 4)))
                .thenReturn(List.of(
                        new UserCalendarDailySummaryRow(LocalDate.of(2026, 3, 29), 1_200, 600, 80, 1, 0, 0),
                        new UserCalendarDailySummaryRow(LocalDate.of(2026, 3, 30), 1_500, 600, 80, 1, 0, 0)
                ));

        rewardPointGrantService.grantCompletedRunningRewards(runningSession, userCalendar, null);

        verify(rewardPointHistoryRepository, never()).saveAndFlush(any());
        verify(rewardPointBalanceRepository, never()).increaseBalance(anyLong(), anyInt());
    }

    @DisplayName("주간 러닝 일수가 5일이면 3일과 5일 스트릭 포인트를 지급한다")
    @Test
    void grantsWeeklyStreakMilestones() {
        User user = user(3L);
        RunningSession runningSession = runningSession(103L, user);
        UserCalendar userCalendar = userCalendar(user, LocalDate.of(2026, 4, 2), 500);
        RewardPointBalance rewardPointBalance = RewardPointBalance.create(user);
        LocalDate weekStart = LocalDate.of(2026, 3, 29);

        when(rewardPointBalanceRepository.findByIdAndDeletedAtIsNull(3L)).thenReturn(Optional.of(rewardPointBalance));
        when(rewardPointHistoryRepository.existsByUserIdAndIdempotencyKeyAndDeletedAtIsNull(3L, "reward:weekly-streak:3:2026-03-29:3"))
                .thenReturn(false);
        when(rewardPointHistoryRepository.existsByUserIdAndIdempotencyKeyAndDeletedAtIsNull(3L, "reward:weekly-streak:3:2026-03-29:5"))
                .thenReturn(false);
        when(userCalendarRepository.findDailySummaries(3L, weekStart, weekStart.plusDays(6)))
                .thenReturn(List.of(
                        new UserCalendarDailySummaryRow(weekStart, 1_000, 600, 80, 1, 0, 0),
                        new UserCalendarDailySummaryRow(weekStart.plusDays(1), 1_000, 600, 80, 1, 0, 0),
                        new UserCalendarDailySummaryRow(weekStart.plusDays(2), 1_000, 600, 80, 1, 0, 0),
                        new UserCalendarDailySummaryRow(weekStart.plusDays(3), 1_000, 600, 80, 1, 0, 0),
                        new UserCalendarDailySummaryRow(weekStart.plusDays(4), 1_000, 600, 80, 1, 0, 0)
                ));

        rewardPointGrantService.grantCompletedRunningRewards(runningSession, userCalendar, null);

        ArgumentCaptor<RewardPointHistory> historyCaptor = ArgumentCaptor.forClass(RewardPointHistory.class);
        verify(rewardPointHistoryRepository, times(2)).saveAndFlush(historyCaptor.capture());
        assertThat(historyCaptor.getAllValues())
                .extracting(RewardPointHistory::getReason)
                .containsExactly(RewardPointReason.WEEKLY_STREAK_3_DAYS, RewardPointReason.WEEKLY_STREAK_5_DAYS);
        verify(rewardPointBalanceRepository, times(2)).increaseBalance(3L, 1);
    }

    @DisplayName("고스트 승리면 고스트 보상 포인트를 지급한다")
    @Test
    void grantsGhostWinPoint() {
        User user = user(4L);
        RunningSession runningSession = runningSession(104L, user);
        UserCalendar userCalendar = userCalendar(user, LocalDate.of(2026, 4, 3), 400);
        RewardPointBalance rewardPointBalance = RewardPointBalance.create(user);
        GhostRunningResult ghostRunningResult = ghostRunningResult(GhostResultStatus.WIN);

        when(rewardPointBalanceRepository.findByIdAndDeletedAtIsNull(4L)).thenReturn(Optional.of(rewardPointBalance));
        when(userCalendarRepository.findDailySummaries(4L, LocalDate.of(2026, 3, 29), LocalDate.of(2026, 4, 4)))
                .thenReturn(List.of(new UserCalendarDailySummaryRow(LocalDate.of(2026, 4, 3), 400, 400, 40, 0, 1, 0)));
        when(rewardPointHistoryRepository.existsByUserIdAndIdempotencyKeyAndDeletedAtIsNull(4L, "reward:ghost-win:4:104"))
                .thenReturn(false);

        rewardPointGrantService.grantCompletedRunningRewards(runningSession, userCalendar, ghostRunningResult);

        ArgumentCaptor<RewardPointHistory> historyCaptor = ArgumentCaptor.forClass(RewardPointHistory.class);
        verify(rewardPointHistoryRepository).saveAndFlush(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getReason()).isEqualTo(RewardPointReason.GHOST_WIN);
        verify(rewardPointBalanceRepository).increaseBalance(4L, 1);
    }

    private User user(Long userId) {
        User user = User.createPendingSocialUser("runner-" + userId);
        setField(user, "id", userId);
        return user;
    }

    private RunningSession runningSession(Long runningSessionId, User user) {
        RunningSession runningSession = instantiate(RunningSession.class);
        setField(runningSession, "id", runningSessionId);
        setField(runningSession, "user", user);
        setField(runningSession, "mode", RunningMode.FREE);
        return runningSession;
    }

    private UserCalendar userCalendar(User user, LocalDate calendarDate, int totalDistanceM) {
        UserCalendar userCalendar = UserCalendar.create(user, calendarDate);
        userCalendar.recordCompletedRun(RunningMode.FREE, totalDistanceM, 600, 80, null);
        return userCalendar;
    }

    private GhostRunningResult ghostRunningResult(GhostResultStatus resultStatus) {
        GhostRunningResult ghostRunningResult = instantiate(GhostRunningResult.class);
        setField(ghostRunningResult, "resultStatus", resultStatus);
        return ghostRunningResult;
    }

    private <T> T instantiate(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to instantiate " + type.getSimpleName(), exception);
        }
    }

    private void setField(Object target, String fieldName, Object value) {
        Class<?> currentClass = target.getClass();
        while (currentClass != null) {
            try {
                Field field = currentClass.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException exception) {
                currentClass = currentClass.getSuperclass();
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException("Failed to set field " + fieldName, exception);
            }
        }

        throw new IllegalArgumentException("Field not found: " + fieldName);
    }
}
