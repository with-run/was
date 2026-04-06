package kr.withrun.was.domain.reward.dto;

import java.time.LocalDateTime;

public record RewardInventoryItemResponse(
        Long rewardGachaDrawCardId,
        Long rewardGachaDrawId,
        Long rewardItemId,
        String title,
        String imageUrl,
        LocalDateTime acquiredAt
) {
}
