package kr.withrun.was.domain.reward.controller;

import kr.withrun.was.domain.auth.security.AuthenticatedUser;
import kr.withrun.was.domain.reward.dto.RewardPointBalanceResponse;
import kr.withrun.was.domain.reward.dto.RewardPointHistoryItemResponse;
import kr.withrun.was.domain.reward.dto.RewardPointHistoryPageResponse;
import kr.withrun.was.domain.reward.service.RewardPointQueryService;
import kr.withrun.was.domain.reward.type.RewardPointReason;
import kr.withrun.was.global.response.ResponseCode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RewardPointController.class, properties = "spring.config.import=")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("리워드 포인트 컨트롤러")
class RewardPointControllerTest {

    static {
        System.setProperty("spring.cloud.aws.parameterstore.enabled", "false");
        System.setProperty("spring.config.import", "");
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RewardPointQueryService rewardPointQueryService;

    @AfterAll
    static void clearConfigOverrides() {
        System.clearProperty("spring.cloud.aws.parameterstore.enabled");
        System.clearProperty("spring.config.import");
    }

    @DisplayName("GET /api/reward-points/me 는 잔액 응답을 ApiResponse 래퍼로 반환한다")
    @Test
    void wrapsRewardPointBalanceResponse() throws Exception {
        given(rewardPointQueryService.getRewardPointBalance(1L))
                .willReturn(new RewardPointBalanceResponse(7));

        SecurityContextHolder.getContext().setAuthentication(authentication(1L));
        try {
            mockMvc.perform(get("/api/reward-points/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.currentBalance").value(7));
        } finally {
            SecurityContextHolder.clearContext();
        }

        then(rewardPointQueryService).should().getRewardPointBalance(1L);
    }

    @DisplayName("GET /api/reward-points/me/histories 는 이력 응답을 ApiResponse 래퍼로 반환한다")
    @Test
    void wrapsRewardPointHistoryResponse() throws Exception {
        given(rewardPointQueryService.getRewardPointHistories(1L, 0, 20))
                .willReturn(new RewardPointHistoryPageResponse(
                        List.of(new RewardPointHistoryItemResponse(
                                10L,
                                1,
                                RewardPointReason.DAILY_RUNNING,
                                "reward:daily-running:1:2026-03-29",
                                LocalDateTime.of(2026, 3, 29, 10, 0)
                        )),
                        0,
                        20,
                        1,
                        1,
                        false
                ));

        SecurityContextHolder.getContext().setAuthentication(authentication(1L));
        try {
            mockMvc.perform(get("/api/reward-points/me/histories")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.items[0].rewardPointHistoryId").value(10L))
                    .andExpect(jsonPath("$.data.items[0].reason").value("DAILY_RUNNING"));
        } finally {
            SecurityContextHolder.clearContext();
        }

        then(rewardPointQueryService).should().getRewardPointHistories(1L, 0, 20);
    }

    private UsernamePasswordAuthenticationToken authentication(Long userId) {
        return new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(userId, "kakao", true),
                null,
                List.of()
        );
    }
}
