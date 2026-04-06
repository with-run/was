package kr.withrun.was.domain.course.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "북마크한 코스 목록 응답")
public record BookmarkedCoursesResponse(
        @ArraySchema(schema = @Schema(implementation = BookmarkedCourseItemResponse.class), arraySchema = @Schema(description = "현재 페이지 북마크 코스 목록. 각 항목에는 좋아요 수와 현재 사용자 좋아요 여부가 포함된다."))
        List<BookmarkedCourseItemResponse> items,

        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasMore,

        @Schema(description = "다음 페이지 조회용 불투명 커서. hasMore 가 true 일 때만 제공된다.", example = "MjAyNi0wMy0xN1QxMDoxNTozMHw0Mg", nullable = true)
        String nextCursor
) {
}
