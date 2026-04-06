package kr.withrun.was.domain.reward.dto;

import kr.withrun.was.domain.reward.entity.RewardPointHistory;
import kr.withrun.was.domain.reward.type.RewardPointReason;

import java.time.LocalDateTime;

public record RewardPointHistoryItemResponse(
        Long rewardPointHistoryId,
        int deltaPoint,
        RewardPointReason reason,
        String idempotencyKey,
        LocalDateTime createdAt
) {

    public static RewardPointHistoryItemResponse from(RewardPointHistory rewardPointHistory) {
        return new RewardPointHistoryItemResponse(
                rewardPointHistory.getId(),
                rewardPointHistory.getDeltaPoint(),
                rewardPointHistory.getReason(),
                rewardPointHistory.getIdempotencyKey(),
                rewardPointHistory.getCreatedAt()
        );
    }
}
