package kr.withrun.was.domain.reward.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RewardDrawPoolItemRequest(
        @NotNull @Positive Long rewardItemId,
        @NotNull @Positive Integer weight
) {
}
