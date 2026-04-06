package kr.withrun.was.domain.reward.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "리워드 아이템 전시 목록 응답")
public record RewardItemShowcaseListResponse(
        @Schema(description = "전시용 리워드 아이템 목록")
        List<RewardItemShowcaseResponse> items
) {
}
