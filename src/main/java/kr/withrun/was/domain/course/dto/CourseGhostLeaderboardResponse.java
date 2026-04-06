package kr.withrun.was.domain.course.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import kr.withrun.was.domain.course.type.RouteType;

import java.util.List;

@Schema(description = "코스 고스트 리더보드 응답")
public record CourseGhostLeaderboardResponse(
        @ArraySchema(schema = @Schema(implementation = CourseGhostLeaderboardItemResponse.class), arraySchema = @Schema(description = "현재 페이지 리더보드 항목 목록"))
        List<CourseGhostLeaderboardItemResponse> items,

        @Schema(description = "코스 경로 유형", nullable = true)
        RouteType routeType,

        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasMore,

        @Schema(description = "다음 페이지 조회용 불투명 커서. hasMore 가 true 일 때만 제공된다.", example = "MTI1MHwxMDE", nullable = true)
        String nextCursor
) {
}
