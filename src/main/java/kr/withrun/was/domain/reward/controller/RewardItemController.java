package kr.withrun.was.domain.reward.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.withrun.was.domain.reward.dto.RewardItemShowcaseListResponse;
import kr.withrun.was.domain.reward.service.RewardItemQueryService;
import kr.withrun.was.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "Reward Item", description = "사용자 공개 리워드 아이템 조회 API")
@RestController
@RequestMapping("/api/reward-items")
@RequiredArgsConstructor
public class RewardItemController {

    private final RewardItemQueryService rewardItemQueryService;

    @Operation(summary = "리워드 아이템 showcase 목록을 조회한다")
    @GetMapping("/showcase")
    public ResponseEntity<ApiResponse<RewardItemShowcaseListResponse>> getRewardItemShowcase() {
        return ApiResponse.successEntity(rewardItemQueryService.getRewardItemShowcase());
    }
}
