package kr.withrun.was.domain.reward.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "리워드 가챠 draw 응답")
public record RewardGachaDrawResponse(
        @Schema(description = "가챠 draw ID", example = "500")
        Long rewardGachaDrawId,

        @Schema(description = "이번 draw 에서 차감된 포인트", example = "1")
        Integer spentPoint,

        @Schema(description = "draw 직후 남은 포인트 잔액", example = "6")
        Integer remainingBalance,

        @Schema(description = "이번 draw 의 전체 카드 결과")
        List<RewardGachaDrawCardResponse> cards,

        @Schema(description = "draw 생성 시각")
        LocalDateTime createdAt
) {
}
