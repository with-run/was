package kr.withrun.was.domain.reward.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "리워드 draw pool item 응답")
public record RewardDrawPoolItemResponse(
        @Schema(description = "리워드 draw pool item ID", example = "101")
        Long rewardDrawPoolItemId,

        @Schema(description = "리워드 아이템 ID", example = "1")
        Long rewardItemId,

        @Schema(description = "리워드 아이템 제목", example = "스타벅스 기프티콘")
        String title,

        @Schema(description = "클라이언트 노출용 이미지 URL", example = "https://cdn.example.com/reward-item/1.png")
        String imageUrl,

        @Schema(description = "weight 값", example = "70")
        Integer weight
) {
}
