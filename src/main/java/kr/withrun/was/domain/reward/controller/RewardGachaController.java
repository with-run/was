package kr.withrun.was.domain.reward.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.withrun.was.domain.auth.security.AuthenticatedUser;
import kr.withrun.was.domain.reward.dto.RewardGachaDrawResponse;
import kr.withrun.was.domain.reward.service.RewardGachaDrawService;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ApiResponse;
import kr.withrun.was.global.response.ResponseCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Reward Gacha", description = "사용자 리워드 가챠 API")
@RestController
@RequestMapping("/api/reward-gacha")
public class RewardGachaController {

    private final RewardGachaDrawService rewardGachaDrawService;

    public RewardGachaController(RewardGachaDrawService rewardGachaDrawService) {
        this.rewardGachaDrawService = rewardGachaDrawService;
    }

    @Operation(summary = "리워드 가챠를 1회 실행한다")
    @PostMapping("/draw")
    public ResponseEntity<ApiResponse<RewardGachaDrawResponse>> draw(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ApiResponse.successEntity(rewardGachaDrawService.draw(resolveUserId(authenticatedUser)));
    }

    private Long resolveUserId(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.userId() == null) {
            throw new CustomException(ResponseCode.UNAUTHORIZED);
        }

        return authenticatedUser.userId();
    }
}
