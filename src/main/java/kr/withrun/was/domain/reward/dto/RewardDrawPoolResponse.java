package kr.withrun.was.domain.reward.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "리워드 draw pool 응답")
public record RewardDrawPoolResponse(
        @Schema(description = "리워드 draw pool ID", example = "1")
        Long rewardDrawPoolId,

        @Schema(description = "pool 이름", example = "런칭 풀")
        String name,

        @Schema(description = "현재 활성 여부", example = "true")
        Boolean isActive,

        @Schema(description = "1회 draw 당 생성할 카드 수", example = "3")
        Integer cardsPerDraw,

        @Schema(description = "꽝 weight 값", example = "40")
        Integer missWeight,

        @Schema(description = "MISS 카드가 참조할 리워드 아이템 ID", example = "10", nullable = true)
        Long missRewardItemId,

        @Schema(description = "pool item 목록")
        List<RewardDrawPoolItemResponse> items,

        @Schema(description = "생성 시각")
        LocalDateTime createdAt,

        @Schema(description = "수정 시각")
        LocalDateTime updatedAt
) {
}
