package kr.withrun.was.domain.reward.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import kr.withrun.was.domain.auth.security.AuthenticatedUser;
import kr.withrun.was.domain.reward.dto.RewardPointBalanceResponse;
import kr.withrun.was.domain.reward.dto.RewardPointHistoryPageResponse;
import kr.withrun.was.domain.reward.service.RewardPointQueryService;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ApiResponse;
import kr.withrun.was.global.response.ResponseCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "Reward Point", description = "사용자 리워드 포인트 조회 API")
@RestController
@RequestMapping("/api/reward-points")
public class RewardPointController {

    private final RewardPointQueryService rewardPointQueryService;

    public RewardPointController(RewardPointQueryService rewardPointQueryService) {
        this.rewardPointQueryService = rewardPointQueryService;
    }

    @Operation(summary = "내 리워드 포인트 잔액을 조회한다")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<RewardPointBalanceResponse>> getRewardPointBalance(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ApiResponse.successEntity(rewardPointQueryService.getRewardPointBalance(resolveUserId(authenticatedUser)));
    }

    @Operation(summary = "내 리워드 포인트 지급 이력을 조회한다")
    @GetMapping("/me/histories")
    public ResponseEntity<ApiResponse<RewardPointHistoryPageResponse>> getRewardPointHistories(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(defaultValue = "20") @Positive int size
    ) {
        return ApiResponse.successEntity(
                rewardPointQueryService.getRewardPointHistories(resolveUserId(authenticatedUser), page, size)
        );
    }

    private Long resolveUserId(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.userId() == null) {
            throw new CustomException(ResponseCode.UNAUTHORIZED);
        }

        return authenticatedUser.userId();
    }
}
