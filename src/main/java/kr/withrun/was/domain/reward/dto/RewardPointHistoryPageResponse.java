package kr.withrun.was.domain.reward.dto;

import java.util.List;

public record RewardPointHistoryPageResponse(
        List<RewardPointHistoryItemResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
}
