package kr.withrun.was.domain.course.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "코스 개인 리더보드 응답")
public record CourseMyLeaderboardResponse(
        @Schema(description = "사용자 닉네임", example = "runner-a")
        String nickname,

        @Schema(description = "사용자 ID", example = "7")
        Long id,

        @Schema(description = "코스 내 사용자 순위. 기록이 없으면 0", example = "4")
        Long rank,

        @Schema(description = "코스 내 사용자 포인트. 기록이 없으면 0", example = "980")
        Integer point
) {
}
