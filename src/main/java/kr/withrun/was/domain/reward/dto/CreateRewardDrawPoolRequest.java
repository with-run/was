package kr.withrun.was.domain.reward.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

public record CreateRewardDrawPoolRequest(
        @NotBlank String name,
        @Positive int cardsPerDraw,
        @PositiveOrZero int missWeight,
        Long missRewardItemId,
        @NotEmpty List<@Valid RewardDrawPoolItemRequest> items
) {
}
