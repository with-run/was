package kr.withrun.was.domain.course.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "코스 북마크 추가 결과")
public record CourseBookmarkAddResponse(
        @Schema(description = "북마크한 코스 ID", example = "12")
        Long courseId,

        @Schema(description = "현재 북마크 여부. 성공 시 항상 true 다.", example = "true")
        boolean isBookmarked,

        @Schema(description = "북마크 생성 시각", example = "2026-03-17T10:15:30")
        LocalDateTime createdAt
) {
}
