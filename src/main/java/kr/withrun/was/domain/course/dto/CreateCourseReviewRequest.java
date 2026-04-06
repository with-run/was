package kr.withrun.was.domain.course.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "코스 리뷰 작성 요청")
public record CreateCourseReviewRequest(
        @Schema(description = "코스 평점(1~5점)", example = "4", minimum = "1", maximum = "5", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @Min(1)
        @Max(5)
        Integer rating,

        @ArraySchema(
                arraySchema = @Schema(description = "코스 특징 태그 목록. 하나 이상 전달해야 하며 중복은 서버에서 제거된다.", requiredMode = Schema.RequiredMode.REQUIRED),
                schema = @Schema(description = "코스 유형 코드", allowableValues = {"RIVERSIDE", "PARK", "MOUNTAIN_TRAIL", "TRACK", "URBAN", "OTHER"}, example = "RIVERSIDE")
        )
        @NotEmpty
        List<@NotBlank String> courseTypes,

        @Schema(description = "리뷰 작성자가 체감한 난이도 코드", allowableValues = {"EASY", "MEDIUM", "HARD"}, example = "MEDIUM", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String submittedDifficulty
) {
}
