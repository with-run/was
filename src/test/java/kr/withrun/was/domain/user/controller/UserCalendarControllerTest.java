package kr.withrun.was.domain.user.controller;

import kr.withrun.was.domain.auth.jwt.JwtProvider;
import kr.withrun.was.domain.user.dto.UserCalendarMonthlyResponse;
import kr.withrun.was.domain.user.dto.UserCalendarSummaryResponse;
import kr.withrun.was.domain.user.dto.UserCalendarWeeklyResponse;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.service.UserCalendarService;
import kr.withrun.was.domain.user.type.Gender;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        properties = {
                "spring.config.import=",
                "spring.cloud.aws.parameterstore.enabled=false"
        }
)
@AutoConfigureMockMvc
@DisplayName("사용자 캘린더 컨트롤러")
class UserCalendarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private UserCalendarService userCalendarService;

    @DisplayName("월별 캘린더 응답을 성공 응답 포맷으로 감싼다")
    @Test
    void wrapsMonthlyCalendarResponseInSuccessEnvelope() throws Exception {
        long userId = 30L;
        int year = 2026;
        int month = 3;
        UserCalendarMonthlyResponse response = new UserCalendarMonthlyResponse(
                year,
                month,
                4,
                23000,
                1280,
                1,
                2,
                1,
                List.of(
                        new UserCalendarMonthlyResponse.DailySummary(
                                LocalDate.of(2026, 3, 8),
                                12000,
                                4200,
                                640,
                                1,
                                1,
                                1
                        )
                )
        );
        given(userCalendarService.findMonthlyCalendar(userId, year, month)).willReturn(response);

        mockMvc.perform(get("/api/calendars/me")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(userId))
                        .queryParam("year", String.valueOf(year))
                        .queryParam("month", String.valueOf(month)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.OK.getMessage()))
                .andExpect(jsonPath("$.data.year").value(year))
                .andExpect(jsonPath("$.data.month").value(month))
                .andExpect(jsonPath("$.data.monthlyRunCount").value(4))
                .andExpect(jsonPath("$.data.monthlyDistanceM").value(23000))
                .andExpect(jsonPath("$.data.monthlyCaloriesKcal").value(1280))
                .andExpect(jsonPath("$.data.monthlyCourseRunCount").value(1))
                .andExpect(jsonPath("$.data.monthlyFreeRunCount").value(2))
                .andExpect(jsonPath("$.data.monthlyGhostRunCount").value(1))
                .andExpect(jsonPath("$.data.dailySummaries[0].calendarDate").value("2026-03-08"))
                .andExpect(jsonPath("$.data.dailySummaries[0].dailyDistanceM").value(12000))
                .andExpect(jsonPath("$.data.dailySummaries[0].dailyDurationSec").value(4200))
                .andExpect(jsonPath("$.data.dailySummaries[0].dailyCaloriesKcal").value(640))
                .andExpect(jsonPath("$.data.dailySummaries[0].dailyCourseRunCount").value(1))
                .andExpect(jsonPath("$.data.dailySummaries[0].dailyFreeRunCount").value(1))
                .andExpect(jsonPath("$.data.dailySummaries[0].dailyGhostRunCount").value(1));

        then(userCalendarService).should().findMonthlyCalendar(userId, year, month);
    }

    @DisplayName("월 파라미터가 범위를 벗어나면 잘못된 입력 응답을 반환한다")
    @Test
    void returnsInvalidInputWhenMonthIsOutOfRange() throws Exception {
        mockMvc.perform(get("/api/calendars/me")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(30L))
                        .queryParam("year", "2026")
                        .queryParam("month", "13"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INPUT_VALUE.getCode()));

        then(userCalendarService).should(never()).findMonthlyCalendar(30L, 2026, 13);
    }

    @DisplayName("연도 파라미터가 없으면 잘못된 입력 응답을 반환한다")
    @Test
    void returnsInvalidInputWhenYearIsMissing() throws Exception {
        mockMvc.perform(get("/api/calendars/me")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(30L))
                        .queryParam("month", "3"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INPUT_VALUE.getCode()));

        then(userCalendarService).shouldHaveNoInteractions();
    }

    @DisplayName("월 파라미터가 없으면 잘못된 입력 응답을 반환한다")
    @Test
    void returnsInvalidInputWhenMonthIsMissing() throws Exception {
        mockMvc.perform(get("/api/calendars/me")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(30L))
                        .queryParam("year", "2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INPUT_VALUE.getCode()));

        then(userCalendarService).shouldHaveNoInteractions();
    }

    @DisplayName("연도 파라미터가 허용 범위를 넘으면 잘못된 입력 응답을 반환한다")
    @Test
    void returnsInvalidInputWhenYearIsOutOfRange() throws Exception {
        mockMvc.perform(get("/api/calendars/me")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(30L))
                        .queryParam("year", "1000000000")
                        .queryParam("month", "3"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INPUT_VALUE.getCode()));

        then(userCalendarService).shouldHaveNoInteractions();
    }

    @DisplayName("월별 캘린더에서 없는 사용자를 조회하면 사용자 없음 응답을 반환한다")
    @Test
    void returnsUserNotFoundWhenMonthlyCalendarUserIsMissing() throws Exception {
        long userId = 999L;
        given(userCalendarService.findMonthlyCalendar(userId, 2026, 3))
                .willThrow(new CustomException(ResponseCode.USER_NOT_FOUND));

        mockMvc.perform(get("/api/calendars/me")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(userId))
                        .queryParam("year", "2026")
                        .queryParam("month", "3"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.USER_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.USER_NOT_FOUND.getMessage()));

        then(userCalendarService).should().findMonthlyCalendar(userId, 2026, 3);
    }

    @DisplayName("캘린더 요약 응답을 성공 응답 포맷으로 감싼다")
    @Test
    void wrapsCalendarSummaryResponseInSuccessEnvelope() throws Exception {
        long userId = 30L;
        UserCalendarSummaryResponse response = new UserCalendarSummaryResponse(
                128,
                742300,
                58,
                47,
                23,
                6,
                21,
                LocalDate.of(2026, 3, 15)
        );
        given(userCalendarService.findSummary(userId)).willReturn(response);

        mockMvc.perform(get("/api/calendars/me/summary")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.OK.getMessage()))
                .andExpect(jsonPath("$.data.lifetimeRunCount").value(128))
                .andExpect(jsonPath("$.data.lifetimeDistanceM").value(742300))
                .andExpect(jsonPath("$.data.lifetimeCourseRunCount").value(58))
                .andExpect(jsonPath("$.data.lifetimeFreeRunCount").value(47))
                .andExpect(jsonPath("$.data.lifetimeGhostRunCount").value(23))
                .andExpect(jsonPath("$.data.currentStreakDays").value(6))
                .andExpect(jsonPath("$.data.longestStreakDays").value(21))
                .andExpect(jsonPath("$.data.calculatedDate").value("2026-03-15"));

        then(userCalendarService).should().findSummary(userId);
    }

    @DisplayName("없는 사용자를 조회하면 사용자 없음 응답을 반환한다")
    @Test
    void returnsUserNotFoundWhenUserIsMissing() throws Exception {
        long userId = 999L;
        given(userCalendarService.findSummary(userId))
                .willThrow(new CustomException(ResponseCode.USER_NOT_FOUND));

        mockMvc.perform(get("/api/calendars/me/summary")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(userId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.USER_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.USER_NOT_FOUND.getMessage()));

        then(userCalendarService).should().findSummary(userId);
    }

    @DisplayName("주간 러닝 요약 응답을 성공 응답 포맷으로 감싼다")
    @Test
    void wrapsWeeklySummaryResponseInSuccessEnvelope() throws Exception {
        long userId = 30L;
        UserCalendarWeeklyResponse response = new UserCalendarWeeklyResponse(
                4,
                23100,
                1240,
                5380
        );
        given(userCalendarService.findWeeklySummary(userId)).willReturn(response);

        mockMvc.perform(get("/api/calendars/me/weekly")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.OK.getMessage()))
                .andExpect(jsonPath("$.data.weeklyRunCount").value(4))
                .andExpect(jsonPath("$.data.weeklyDistanceM").value(23100))
                .andExpect(jsonPath("$.data.weeklyCaloriesKcal").value(1240))
                .andExpect(jsonPath("$.data.weeklyDurationSec").value(5380));

        then(userCalendarService).should().findWeeklySummary(userId);
    }

    @DisplayName("주간 요약에서 없는 사용자를 조회하면 사용자 없음 응답을 반환한다")
    @Test
    void returnsUserNotFoundWhenWeeklySummaryUserIsMissing() throws Exception {
        long userId = 999L;
        given(userCalendarService.findWeeklySummary(userId))
                .willThrow(new CustomException(ResponseCode.USER_NOT_FOUND));

        mockMvc.perform(get("/api/calendars/me/weekly")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(userId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.USER_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.USER_NOT_FOUND.getMessage()));

        then(userCalendarService).should().findWeeklySummary(userId);
    }

    private String bearerToken(Long userId) {
        return "Bearer " + jwtProvider.generateTokenPair(completedUser(userId), "google").accessToken();
    }

    private User completedUser(Long id) {
        User user = User.createPendingSocialUser();
        setField(user, "id", id);
        user.completeProfile(
                "runner",
                LocalDate.of(1999, 1, 2),
                Gender.MALE,
                180.0,
                72.5
        );
        return user;
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
