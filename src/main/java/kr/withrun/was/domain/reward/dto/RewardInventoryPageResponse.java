package kr.withrun.was.domain.reward.dto;

import java.util.List;

public record RewardInventoryPageResponse(
        List<RewardInventoryItemResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
}
