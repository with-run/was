package kr.withrun.was.domain.user.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.domain.user.type.Purpose;
import kr.withrun.was.global.common.type.Difficulty;
import kr.withrun.was.global.common.type.TimeSlot;

import java.util.List;

@Schema(description = "내 러닝 기본 설정 수정 요청")
public record UpdateUserRunningPreferenceRequest(
        @ArraySchema(
                arraySchema = @Schema(description = "러닝 목적 목록", requiredMode = Schema.RequiredMode.REQUIRED),
                schema = @Schema(implementation = Purpose.class)
        )
        @NotEmpty
        List<@NotNull Purpose> purposes,

        @ArraySchema(
                arraySchema = @Schema(description = "선호 시간대 목록", requiredMode = Schema.RequiredMode.REQUIRED),
                schema = @Schema(implementation = TimeSlot.class)
        )
        @NotEmpty
        List<@NotNull TimeSlot> timeSlots,

        @Schema(description = "선호 거리(km). 0 초과 10 이하만 허용된다.", example = "5.0", minimum = "0", maximum = "10.0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @Positive
        @DecimalMax("10.0")
        Double preferredDistanceKm,

        @Schema(description = "선호 난이도", implementation = Difficulty.class, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        Difficulty preferredDifficulty,

        @ArraySchema(
                arraySchema = @Schema(description = "선호 코스 타입 목록", requiredMode = Schema.RequiredMode.REQUIRED),
                schema = @Schema(implementation = CourseType.class)
        )
        @NotEmpty
        List<@NotNull CourseType> courseTypes
) {
}
