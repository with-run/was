package kr.withrun.was.domain.course.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import kr.withrun.was.domain.course.entity.CourseReview;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.global.common.type.Difficulty;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "코스 리뷰 작성 결과")
public record CreateCourseReviewResponse(
        @Schema(description = "생성된 리뷰 ID", example = "101")
        Long courseReviewId,

        @Schema(description = "리뷰가 연결된 코스 ID", example = "12")
        Long courseId,

        @Schema(description = "저장된 평점", example = "4")
        Integer rating,

        @ArraySchema(schema = @Schema(implementation = CourseTypeOption.class), arraySchema = @Schema(description = "저장된 코스 유형 목록"))
        List<CourseTypeOption> courseTypes,

        @Schema(description = "저장된 체감 난이도", implementation = DifficultyOption.class)
        DifficultyOption submittedDifficulty,

        @Schema(description = "리뷰 생성 시각", example = "2026-03-17T10:15:30")
        LocalDateTime createdAt
) {

    public static CreateCourseReviewResponse from(CourseReview courseReview, List<CourseType> courseTypes) {
        return new CreateCourseReviewResponse(
                courseReview.getId(),
                courseReview.getCourse().getId(),
                courseReview.getRating(),
                courseTypes.stream().map(CourseTypeOption::from).toList(),
                DifficultyOption.from(courseReview.getDifficulty()),
                courseReview.getCreatedAt()
        );
    }

    @Schema(name = "CreateCourseReviewCourseTypeOption", description = "코스 리뷰 응답의 코스 유형 항목")
    public record CourseTypeOption(
            @Schema(description = "클라이언트가 사용하는 데이터 값", example = "RIVERSIDE")
            String data,
            @Schema(description = "사용자에게 노출하는 라벨", example = "강변")
            String label
    ) {
        public static CourseTypeOption from(CourseType courseType) {
            return new CourseTypeOption(courseType.getData(), courseType.getLabel());
        }
    }

    @Schema(name = "CreateCourseReviewDifficultyOption", description = "코스 리뷰 응답의 난이도 항목")
    public record DifficultyOption(
            @Schema(description = "클라이언트가 사용하는 데이터 값", example = "MEDIUM")
            String data,
            @Schema(description = "사용자에게 노출하는 라벨", example = "보통")
            String label
    ) {
        public static DifficultyOption from(Difficulty difficulty) {
            return new DifficultyOption(difficulty.getData(), difficulty.getLabel());
        }
    }
}
