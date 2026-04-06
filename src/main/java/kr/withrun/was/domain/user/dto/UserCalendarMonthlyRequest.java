package kr.withrun.was.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "월간 캘린더 조회 조건")
public record UserCalendarMonthlyRequest(
        @Schema(description = "조회 대상 연도", example = "2026", minimum = "1", maximum = "999999999", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @Positive
        @Max(999999999)
        Integer year,

        @Schema(description = "조회 대상 월", example = "3", minimum = "1", maximum = "12", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @Min(1)
        @Max(12)
        Integer month
) {
}
