package kr.withrun.was.domain.reward.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.withrun.was.domain.reward.type.RewardGachaCardType;

@Schema(description = "리워드 가챠 draw 카드 응답")
public record RewardGachaDrawCardResponse(
        @Schema(description = "가챠 draw 카드 ID", example = "700")
        Long rewardGachaDrawCardId,

        @Schema(description = "카드 순서", example = "1")
        Integer cardIndex,

        @Schema(description = "카드 결과 타입", example = "REWARD")
        RewardGachaCardType cardType,

        @Schema(description = "리워드 아이템 ID", example = "100")
        Long rewardItemId,

        @Schema(description = "리워드 아이템 제목", example = "Reward A")
        String title,

        @Schema(description = "클라이언트 노출용 이미지 URL", example = "https://cdn.example.com/reward-item/1.png")
        String imageUrl
) {
}
