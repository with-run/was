package kr.withrun.was.domain.reward.controller;

import kr.withrun.was.domain.auth.security.AuthenticatedUser;
import kr.withrun.was.domain.reward.dto.RewardInventoryItemResponse;
import kr.withrun.was.domain.reward.dto.RewardInventoryPageResponse;
import kr.withrun.was.domain.reward.service.RewardGachaInventoryQueryService;
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

@WebMvcTest(controllers = RewardGachaInventoryController.class, properties = "spring.config.import=")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("리워드 인벤토리 컨트롤러")
class RewardGachaInventoryControllerTest {

    static {
        System.setProperty("spring.cloud.aws.parameterstore.enabled", "false");
        System.setProperty("spring.config.import", "");
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RewardGachaInventoryQueryService rewardGachaInventoryQueryService;

    @AfterAll
    static void clearConfigOverrides() {
        System.clearProperty("spring.cloud.aws.parameterstore.enabled");
        System.clearProperty("spring.config.import");
    }

    @DisplayName("GET /api/reward-gacha/me/inventory 는 내 인벤토리 페이지를 반환한다")
    @Test
    void wrapsRewardInventoryResponse() throws Exception {
        given(rewardGachaInventoryQueryService.getRewardInventory(1L, 0, 20))
                .willReturn(new RewardInventoryPageResponse(
                        List.of(new RewardInventoryItemResponse(
                                700L,
                                500L,
                                100L,
                                "Reward A",
                                "signed::reward-item/1.png",
                                LocalDateTime.of(2026, 3, 30, 10, 0)
                        )),
                        0,
                        20,
                        1,
                        1,
                        false
                ));

        SecurityContextHolder.getContext().setAuthentication(authentication(1L));
        try {
            mockMvc.perform(get("/api/reward-gacha/me/inventory")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.items[0].rewardGachaDrawCardId").value(700L))
                    .andExpect(jsonPath("$.data.items[0].rewardItemId").value(100L));
        } finally {
            SecurityContextHolder.clearContext();
        }

        then(rewardGachaInventoryQueryService).should().getRewardInventory(1L, 0, 20);
    }

    private UsernamePasswordAuthenticationToken authentication(Long userId) {
        return new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(userId, "kakao", true),
                null,
                List.of()
        );
    }
}
