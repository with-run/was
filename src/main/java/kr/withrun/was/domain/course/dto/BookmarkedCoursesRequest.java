package kr.withrun.was.domain.course.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;

@Schema(description = "북마크한 코스 목록 조회 조건")
public record BookmarkedCoursesRequest(
        @Schema(description = "한 번에 조회할 최대 항목 수. 생략 시 기본값 10을 사용한다.", example = "10", defaultValue = "10", minimum = "1", maximum = "50")
        @Positive
        @Max(50)
        Integer size,

        @Schema(description = "이전 응답의 nextCursor 값을 그대로 전달하는 불투명 페이지네이션 토큰", example = "MjAyNi0wMy0xN1QxMDoxNTozMHw0Mg", nullable = true)
        String cursor
) {

    public BookmarkedCoursesRequest {
        size = size == null ? 10 : size;
    }
}
