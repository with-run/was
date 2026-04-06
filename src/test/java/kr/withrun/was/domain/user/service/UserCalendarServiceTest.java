package kr.withrun.was.domain.user.service;

import kr.withrun.was.domain.user.dto.UserCalendarMonthlyResponse;
import kr.withrun.was.domain.user.dto.UserCalendarSummaryResponse;
import kr.withrun.was.domain.user.dto.UserCalendarWeeklyResponse;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.repository.UserCalendarRepository;
import kr.withrun.was.domain.user.repository.UserRepository;
import kr.withrun.was.domain.user.repository.query.dto.UserCalendarDailySummaryRow;
import kr.withrun.was.domain.user.repository.query.dto.UserCalendarSummaryRow;
import kr.withrun.was.domain.user.type.Gender;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("사용자 캘린더 서비스")
class UserCalendarServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserCalendarRepository userCalendarRepository;

    private UserCalendarService userCalendarService;

    @BeforeEach
    void setUp() {
        userCalendarService = new UserCalendarService(userRepository, userCalendarRepository);
    }

    @DisplayName("월별 캘린더 요약과 날짜별 러닝 합계를 함께 반환한다")
    @Test
    void returnsMonthlyCalendarWithAggregatedTotals() {
        User user = user(13L);
        when(userRepository.findNotDeletedUser(user.getId())).thenReturn(Optional.of(user));
        when(userCalendarRepository.findDailySummaries(
                user.getId(),
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31)
        )).thenReturn(List.of(
                new UserCalendarDailySummaryRow(LocalDate.of(2026, 3, 8), 12000, 4200, 640, 1, 1, 1),
                new UserCalendarDailySummaryRow(LocalDate.of(2026, 3, 15), 8000, 2400, 410, 0, 2, 0)
        ));

        UserCalendarMonthlyResponse response = userCalendarService.findMonthlyCalendar(user.getId(), 2026, 3);

        assertThat(response.year()).isEqualTo(2026);
        assertThat(response.month()).isEqualTo(3);
        assertThat(response.monthlyRunCount()).isEqualTo(5);
        assertThat(response.monthlyDistanceM()).isEqualTo(20000);
        assertThat(response.monthlyCaloriesKcal()).isEqualTo(1050);
        assertThat(response.monthlyCourseRunCount()).isEqualTo(1);
        assertThat(response.monthlyFreeRunCount()).isEqualTo(3);
        assertThat(response.monthlyGhostRunCount()).isEqualTo(1);
        assertThat(response.dailySummaries()).containsExactly(
                new UserCalendarMonthlyResponse.DailySummary(
                        LocalDate.of(2026, 3, 8),
                        12000,
                        4200,
                        640,
                        1,
                        1,
                        1
                ),
                new UserCalendarMonthlyResponse.DailySummary(
                        LocalDate.of(2026, 3, 15),
                        8000,
                        2400,
                        410,
                        0,
                        2,
                        0
                )
        );
    }

    @DisplayName("월별 캘린더 데이터가 없어도 0 합계와 빈 목록을 반환한다")
    @Test
    void returnsZeroMonthlyCalendarWhenUserHasNoCalendarsInMonth() {
        User user = user(14L);
        when(userRepository.findNotDeletedUser(user.getId())).thenReturn(Optional.of(user));
        when(userCalendarRepository.findDailySummaries(
                user.getId(),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30)
        )).thenReturn(List.of());

        UserCalendarMonthlyResponse response = userCalendarService.findMonthlyCalendar(user.getId(), 2026, 4);

        assertThat(response.year()).isEqualTo(2026);
        assertThat(response.month()).isEqualTo(4);
        assertThat(response.monthlyRunCount()).isZero();
        assertThat(response.monthlyDistanceM()).isZero();
        assertThat(response.monthlyCaloriesKcal()).isZero();
        assertThat(response.monthlyCourseRunCount()).isZero();
        assertThat(response.monthlyFreeRunCount()).isZero();
        assertThat(response.monthlyGhostRunCount()).isZero();
        assertThat(response.dailySummaries()).isEmpty();
    }

    @DisplayName("연도가 java.time 허용 범위를 벗어나면 잘못된 입력 예외를 던진다")
    @Test
    void throwsInvalidInputWhenYearIsOutOfSupportedRange() {
        User user = user(15L);
        when(userRepository.findNotDeletedUser(user.getId())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userCalendarService.findMonthlyCalendar(user.getId(), 1_000_000_000, 3))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.INVALID_INPUT_VALUE);
    }

    @DisplayName("전체 누적 요약과 현재/최장 스트릭을 함께 반환한다")
    @Test
    void returnsLifetimeSummaryWithCurrentAndLongestStreaks() {
        LocalDate today = LocalDate.now();
        User user = user(10L);
        when(userRepository.findNotDeletedUser(user.getId())).thenReturn(Optional.of(user));
        when(userCalendarRepository.findSummaryRow(user.getId())).thenReturn(
                new UserCalendarSummaryRow(18, 742300, 8, 6, 4)
        );
        when(userCalendarRepository.findRunningDates(user.getId())).thenReturn(List.of(
                today,
                today.minusDays(1),
                today.minusDays(2),
                today.minusDays(5),
                today.minusDays(6),
                today.minusDays(7),
                today.minusDays(8)
        ));

        UserCalendarSummaryResponse response = userCalendarService.findSummary(user.getId());

        assertThat(response.lifetimeRunCount()).isEqualTo(18);
        assertThat(response.lifetimeDistanceM()).isEqualTo(742300);
        assertThat(response.lifetimeCourseRunCount()).isEqualTo(8);
        assertThat(response.lifetimeFreeRunCount()).isEqualTo(6);
        assertThat(response.lifetimeGhostRunCount()).isEqualTo(4);
        assertThat(response.currentStreakDays()).isEqualTo(3);
        assertThat(response.longestStreakDays()).isEqualTo(4);
        assertThat(response.calculatedDate()).isEqualTo(today);
    }

    @DisplayName("캘린더 데이터가 없어도 기존 사용자는 0 요약을 반환한다")
    @Test
    void returnsZeroSummaryWhenUserHasNoCalendars() {
        LocalDate today = LocalDate.now();
        User user = user(11L);
        when(userRepository.findNotDeletedUser(user.getId())).thenReturn(Optional.of(user));
        when(userCalendarRepository.findSummaryRow(user.getId())).thenReturn(
                new UserCalendarSummaryRow(0, 0, 0, 0, 0)
        );
        when(userCalendarRepository.findRunningDates(user.getId())).thenReturn(List.of());

        UserCalendarSummaryResponse response = userCalendarService.findSummary(user.getId());

        assertThat(response.lifetimeRunCount()).isZero();
        assertThat(response.lifetimeDistanceM()).isZero();
        assertThat(response.lifetimeCourseRunCount()).isZero();
        assertThat(response.lifetimeFreeRunCount()).isZero();
        assertThat(response.lifetimeGhostRunCount()).isZero();
        assertThat(response.currentStreakDays()).isZero();
        assertThat(response.longestStreakDays()).isZero();
        assertThat(response.calculatedDate()).isEqualTo(today);
    }

    @DisplayName("이번 주 일요일부터 토요일까지의 러닝 합계를 반환한다")
    @Test
    void returnsWeeklySummaryForCurrentWeek() {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays((today.getDayOfWeek().getValue()) % 7L);
        LocalDate endDate = startDate.plusDays(6);
        User user = user(16L);

        when(userRepository.findNotDeletedUser(user.getId())).thenReturn(Optional.of(user));
        when(userCalendarRepository.findDailySummaries(user.getId(), startDate, endDate)).thenReturn(List.of(
                new UserCalendarDailySummaryRow(startDate, 7000, 1800, 390, 1, 0, 0),
                new UserCalendarDailySummaryRow(startDate.plusDays(2), 5000, 1500, 280, 0, 1, 1)
        ));

        UserCalendarWeeklyResponse response = userCalendarService.findWeeklySummary(user.getId());

        assertThat(response.weeklyRunCount()).isEqualTo(3);
        assertThat(response.weeklyDistanceM()).isEqualTo(12000);
        assertThat(response.weeklyCaloriesKcal()).isEqualTo(670);
        assertThat(response.weeklyDurationSec()).isEqualTo(3300);
    }

    @DisplayName("이번 주 캘린더 데이터가 없어도 0 주간 합계를 반환한다")
    @Test
    void returnsZeroWeeklySummaryWhenUserHasNoCalendarsInWeek() {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays((today.getDayOfWeek().getValue()) % 7L);
        LocalDate endDate = startDate.plusDays(6);
        User user = user(17L);

        when(userRepository.findNotDeletedUser(user.getId())).thenReturn(Optional.of(user));
        when(userCalendarRepository.findDailySummaries(user.getId(), startDate, endDate)).thenReturn(List.of());

        UserCalendarWeeklyResponse response = userCalendarService.findWeeklySummary(user.getId());

        assertThat(response.weeklyRunCount()).isZero();
        assertThat(response.weeklyDistanceM()).isZero();
        assertThat(response.weeklyCaloriesKcal()).isZero();
        assertThat(response.weeklyDurationSec()).isZero();
    }

    @DisplayName("없는 사용자는 USER_NOT_FOUND 예외를 던진다")
    @Test
    void throwsUserNotFoundWhenUserIsMissing() {
        when(userRepository.findNotDeletedUser(12L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userCalendarService.findSummary(12L))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.USER_NOT_FOUND);

        verify(userRepository).findNotDeletedUser(12L);
        verifyNoInteractions(userCalendarRepository);
    }

    private User user(Long userId) {
        User user = instantiate(User.class);
        setField(user, "id", userId);
        setField(user, "nickname", "runner-" + userId);
        setField(user, "birthDate", LocalDate.of(1995, 3, 11));
        setField(user, "gender", Gender.MALE);
        setField(user, "height", 175.0);
        setField(user, "weight", 68.0);
        return user;
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
