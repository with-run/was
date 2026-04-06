package kr.withrun.was.domain.user.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import kr.withrun.was.domain.user.repository.query.dto.UserCalendarDailySummaryRow;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "사용자 월간 러닝 캘린더 응답")
public record UserCalendarMonthlyResponse(
        @Schema(description = "조회 연도", example = "2026")
        Integer year,

        @Schema(description = "조회 월", example = "3")
        Integer month,

        @Schema(description = "해당 월 총 러닝 횟수", example = "12")
        Integer monthlyRunCount,

        @Schema(description = "해당 월 총 러닝 거리(m)", example = "64320")
        Integer monthlyDistanceM,

        @Schema(description = "해당 월 총 소모 칼로리(kcal)", example = "4210")
        Integer monthlyCaloriesKcal,

        @Schema(description = "해당 월 코스 러닝 횟수", example = "5")
        Integer monthlyCourseRunCount,

        @Schema(description = "해당 월 자유 러닝 횟수", example = "4")
        Integer monthlyFreeRunCount,

        @Schema(description = "해당 월 고스트 러닝 횟수", example = "3")
        Integer monthlyGhostRunCount,

        @ArraySchema(schema = @Schema(implementation = DailySummary.class), arraySchema = @Schema(description = "일자별 러닝 집계 목록"))
        List<DailySummary> dailySummaries
) {
    public static UserCalendarMonthlyResponse from(Integer year, Integer month, List<DailySummary> dailySummaries) {
        return new UserCalendarMonthlyResponse(
                year,
                month,
                dailySummaries.stream()
                        .mapToInt(DailySummary::dailyRunCount)
                        .sum(),
                dailySummaries.stream()
                        .mapToInt(DailySummary::dailyDistanceM)
                        .sum(),
                dailySummaries.stream()
                        .mapToInt(DailySummary::dailyCaloriesKcal)
                        .sum(),
                dailySummaries.stream()
                        .mapToInt(DailySummary::dailyCourseRunCount)
                        .sum(),
                dailySummaries.stream()
                        .mapToInt(DailySummary::dailyFreeRunCount)
                        .sum(),
                dailySummaries.stream()
                        .mapToInt(DailySummary::dailyGhostRunCount)
                        .sum(),
                dailySummaries
        );
    }

    @Schema(name = "UserCalendarDailySummary", description = "특정 일자의 러닝 요약")
    public record DailySummary(
            @Schema(description = "캘린더 날짜", example = "2026-03-17")
            LocalDate calendarDate,

            @Schema(description = "해당 날짜 총 러닝 거리(m)", example = "5300")
            Integer dailyDistanceM,

            @Schema(description = "해당 날짜 총 러닝 시간(초)", example = "1820")
            Integer dailyDurationSec,

            @Schema(description = "해당 날짜 총 소모 칼로리(kcal)", example = "320")
            Integer dailyCaloriesKcal,

            @Schema(description = "해당 날짜 코스 러닝 횟수", example = "1")
            Integer dailyCourseRunCount,

            @Schema(description = "해당 날짜 자유 러닝 횟수", example = "0")
            Integer dailyFreeRunCount,

            @Schema(description = "해당 날짜 고스트 러닝 횟수", example = "1")
            Integer dailyGhostRunCount
    ) {
        public static DailySummary from(UserCalendarDailySummaryRow dailySummaryRow) {
            return new DailySummary(
                    dailySummaryRow.calendarDate(),
                    dailySummaryRow.dailyDistanceM(),
                    dailySummaryRow.dailyDurationSec(),
                    dailySummaryRow.dailyCaloriesKcal(),
                    dailySummaryRow.dailyCourseRunCount(),
                    dailySummaryRow.dailyFreeRunCount(),
                    dailySummaryRow.dailyGhostRunCount()
            );
        }

        public int dailyRunCount() {
            return dailyCourseRunCount + dailyFreeRunCount + dailyGhostRunCount;
        }
    }
}
