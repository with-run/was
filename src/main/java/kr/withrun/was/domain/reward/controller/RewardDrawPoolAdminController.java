package kr.withrun.was.domain.reward.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import kr.withrun.was.domain.reward.dto.CreateRewardDrawPoolRequest;
import kr.withrun.was.domain.reward.dto.RewardDrawPoolListResponse;
import kr.withrun.was.domain.reward.dto.RewardDrawPoolResponse;
import kr.withrun.was.domain.reward.dto.UpdateRewardDrawPoolRequest;
import kr.withrun.was.domain.reward.service.RewardDrawPoolAdminService;
import kr.withrun.was.global.response.ApiResponse;
import kr.withrun.was.global.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "Reward Draw Pool Admin", description = "관리자용 리워드 draw pool 관리 API")
@RestController
@RequestMapping("/api/admin/reward-draw-pools")
@RequiredArgsConstructor
public class RewardDrawPoolAdminController {

    private final RewardDrawPoolAdminService rewardDrawPoolAdminService;

    @Operation(summary = "리워드 draw pool 을 생성한다")
    @PostMapping
    public ResponseEntity<ApiResponse<RewardDrawPoolResponse>> createRewardDrawPool(
            @Valid @RequestBody CreateRewardDrawPoolRequest request
    ) {
        return ApiResponse.successEntity(ResponseCode.CREATED, rewardDrawPoolAdminService.createRewardDrawPool(request));
    }

    @Operation(summary = "리워드 draw pool 목록을 조회한다")
    @GetMapping
    public ResponseEntity<ApiResponse<RewardDrawPoolListResponse>> getRewardDrawPools(
            @RequestParam(defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(defaultValue = "20") @Positive int size
    ) {
        return ApiResponse.successEntity(rewardDrawPoolAdminService.getRewardDrawPools(page, size));
    }

    @Operation(summary = "리워드 draw pool 단건을 조회한다")
    @GetMapping("/{rewardDrawPoolId}")
    public ResponseEntity<ApiResponse<RewardDrawPoolResponse>> getRewardDrawPool(
            @PathVariable @Positive Long rewardDrawPoolId
    ) {
        return ApiResponse.successEntity(rewardDrawPoolAdminService.getRewardDrawPool(rewardDrawPoolId));
    }

    @Operation(summary = "리워드 draw pool 을 수정한다")
    @PatchMapping("/{rewardDrawPoolId}")
    public ResponseEntity<ApiResponse<RewardDrawPoolResponse>> updateRewardDrawPool(
            @PathVariable @Positive Long rewardDrawPoolId,
            @Valid @RequestBody UpdateRewardDrawPoolRequest request
    ) {
        return ApiResponse.successEntity(rewardDrawPoolAdminService.updateRewardDrawPool(rewardDrawPoolId, request));
    }

    @Operation(summary = "리워드 draw pool 을 활성화한다")
    @PostMapping("/{rewardDrawPoolId}/activate")
    public ResponseEntity<ApiResponse<RewardDrawPoolResponse>> activateRewardDrawPool(
            @PathVariable @Positive Long rewardDrawPoolId
    ) {
        return ApiResponse.successEntity(rewardDrawPoolAdminService.activateRewardDrawPool(rewardDrawPoolId));
    }

    @Operation(summary = "현재 활성 리워드 draw pool 을 조회한다")
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<RewardDrawPoolResponse>> getActiveRewardDrawPool() {
        return ApiResponse.successEntity(rewardDrawPoolAdminService.getActiveRewardDrawPool());
    }
}
