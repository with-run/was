package kr.withrun.was.domain.course.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "코스 북마크 해제 결과")
public record CourseBookmarkRemoveResponse(
        @Schema(description = "북마크 해제 대상 코스 ID", example = "12")
        Long courseId,

        @Schema(description = "현재 북마크 여부. 성공 시 항상 false 다.", example = "false")
        boolean isBookmarked
) {
}
