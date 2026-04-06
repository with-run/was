package kr.withrun.was.domain.reward.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "리워드 draw pool 목록 응답")
public record RewardDrawPoolListResponse(
        @Schema(description = "draw pool 목록")
        List<RewardDrawPoolResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
}
