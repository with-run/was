package kr.withrun.was.domain.course.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;

@Schema(description = "코스 고스트 리더보드 조회 조건")
public record CourseGhostLeaderboardRequest(
        @Schema(description = "한 번에 조회할 최대 항목 수입니다. 생략하면 20을 사용합니다.", example = "20", defaultValue = "20", minimum = "1", maximum = "50")
        @Positive
        @Max(50)
        Integer size,

        @Schema(description = "이전 응답의 nextCursor 값을 그대로 전달하는 불투명 페이지네이션 토큰입니다.", example = "MTI1MHwxMDE", nullable = true)
        String cursor
) {

    public CourseGhostLeaderboardRequest {
        size = size == null ? 20 : size;
    }
}
