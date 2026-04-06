package kr.withrun.was.domain.course.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.withrun.was.domain.course.type.CourseStatus;

@Schema(description = "코스 좋아요 상태 변경 결과")
public record CourseLikeStatusResponse(
        @Schema(description = "코스 ID", example = "12")
        Long courseId,

        @Schema(description = "현재 사용자의 좋아요 여부", example = "true")
        boolean isLiked,

        @Schema(description = "변경 반영 후 좋아요 수", example = "128")
        long likeCount,

        @Schema(description = "좋아요 반영 후 코스 상태")
        CourseStatus status
) {
}
