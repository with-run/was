package kr.withrun.was.domain.running.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "과거 러닝 세션 목록 응답")
public record PastRunningSessionsResponse(
        @Schema(description = "조회된 과거 러닝 세션 목록")
        List<PastRunningSessionItemResponse> items,
        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasMore,
        @Schema(description = "다음 페이지 조회에 사용할 cursor 값. 마지막 페이지면 null 이다.", example = "MjAyNi0wMy0xNVQxOTozMDowMHwxMjA=", nullable = true)
        String nextCursor
) {
}
