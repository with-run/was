package kr.withrun.was.domain.running.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

import java.time.DateTimeException;
import java.time.LocalDate;

@Schema(description = "과거 러닝 세션 목록 조회 조건")
public record PastRunningSessionsRequest(
        @Positive
        @Max(999999999)
        @Schema(description = "조회 기준 연도. month 또는 day 를 함께 사용할 때 필요하다.", example = "2026", minimum = "1")
        Integer year,

        @Min(1)
        @Max(12)
        @Schema(description = "조회 기준 월. year 와 함께 사용한다.", example = "3", minimum = "1", maximum = "12")
        Integer month,

        @Min(1)
        @Max(31)
        @Schema(description = "조회 기준 일. year, month 와 함께 사용한다.", example = "16", minimum = "1", maximum = "31")
        Integer day,

        @Positive
        @Schema(description = "페이지 크기. 생략 시 10이 적용된다.", example = "10", minimum = "1")
        Integer pageSize,

        @Schema(description = "이전 응답의 nextCursor 값을 그대로 전달하는 불투명 페이지네이션 토큰", example = "MjAyNi0wMy0xNVQxOTozMDowMHwxMjA=", nullable = true)
        String cursor
) {

    public PastRunningSessionsRequest {
        pageSize = pageSize == null ? 10 : pageSize;
    }

    //날짜 조합이 올바른지
    @AssertTrue
    public boolean isDateFilterCombinationValid() {

        // month는 year 없이 올 수 없음
        if (month != null && year == null) {
            return false;
        }

        // day는 year와 month가 모두 있어야 함
        if (day != null && (year == null || month == null)) {
            return false;
        }

        return true;
    }

    //유효한 날짜인지
    @AssertTrue
    public boolean isCalendarDateValid() {
        if (!isDateFilterCombinationValid()) {
            return true;
        }
        if (year == null) {
            return true;
        }

        try {
            LocalDate.of(
                    year,
                    month == null ? 1 : month,
                    day == null ? 1 : day
            );
            return true;
        } catch (DateTimeException e) {
            return false;
        }
    }
}
