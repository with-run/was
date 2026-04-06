package kr.withrun.was.domain.reward.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "리워드 아이템 응답")
public record RewardItemResponse(
        @Schema(description = "리워드 아이템 ID", example = "1")
        Long rewardItemId,

        @Schema(description = "리워드 아이템 제목", example = "스타벅스 기프티콘")
        String title,

        @Schema(description = "클라이언트 노출용 이미지 URL", example = "https://cdn.example.com/reward-item/1.png")
        String imageUrl,

        @Schema(description = "활성 여부", example = "true")
        boolean isActive,

        @Schema(description = "생성 시각")
        LocalDateTime createdAt,

        @Schema(description = "수정 시각")
        LocalDateTime updatedAt
) {
}
