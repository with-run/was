package kr.withrun.was.domain.reward.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "리워드 아이템 메타데이터 수정 요청")
public record UpdateRewardItemRequest(
        @Schema(description = "리워드 아이템 제목", example = "배달의민족 상품권")
        @Size(max = 100)
        String title,

        @Schema(description = "활성 여부", example = "false")
        Boolean isActive
) {
}
