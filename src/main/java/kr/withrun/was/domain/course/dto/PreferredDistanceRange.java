package kr.withrun.was.domain.course.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "선호 거리 범위")
public record PreferredDistanceRange(
        @Schema(description = "선호 거리 최소값(m)", example = "1")
        @NotNull
        @Positive
        Integer min,

        @Schema(description = "선호 거리 최대값(m)", example = "3000")
        @NotNull
        @Positive
        Integer max
) {
    @AssertTrue(message = "min 값은 항상 max보다 작거나 같아야 합니다.")
    public boolean isValidRange() {
        return min != null && max != null && min <= max;
    }
}
