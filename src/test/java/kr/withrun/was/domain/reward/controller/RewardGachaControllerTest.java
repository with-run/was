package kr.withrun.was.domain.reward.controller;

import kr.withrun.was.domain.auth.security.AuthenticatedUser;
import kr.withrun.was.domain.reward.dto.RewardGachaDrawCardResponse;
import kr.withrun.was.domain.reward.dto.RewardGachaDrawResponse;
import kr.withrun.was.domain.reward.service.RewardGachaDrawService;
import kr.withrun.was.domain.reward.type.RewardGachaCardType;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RewardGachaController.class, properties = "spring.config.import=")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("리워드 가챠 컨트롤러")
class RewardGachaControllerTest {

    static {
        System.setProperty("spring.cloud.aws.parameterstore.enabled", "false");
        System.setProperty("spring.config.import", "");
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RewardGachaDrawService rewardGachaDrawService;

    @AfterAll
    static void clearConfigOverrides() {
        System.clearProperty("spring.cloud.aws.parameterstore.enabled");
        System.clearProperty("spring.config.import");
    }

    @DisplayName("POST /api/reward-gacha/draw 는 요청 바디 없이 draw 결과를 반환한다")
    @Test
    void wrapsRewardGachaDrawResponse() throws Exception {
        given(rewardGachaDrawService.draw(1L))
                .willReturn(new RewardGachaDrawResponse(
                        500L,
                        1,
                        6,
                        List.of(
                                new RewardGachaDrawCardResponse(700L, 1, RewardGachaCardType.REWARD, 100L, "Reward A", "signed::reward-item/1.png"),
                                new RewardGachaDrawCardResponse(701L, 2, RewardGachaCardType.MISS, 200L, "Miss Reward", "signed::reward-item/miss.png")
                        ),
                        LocalDateTime.of(2026, 3, 29, 21, 0)
                ));

        SecurityContextHolder.getContext().setAuthentication(authentication(1L));
        try {
            mockMvc.perform(post("/api/reward-gacha/draw"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.rewardGachaDrawId").value(500L))
                    .andExpect(jsonPath("$.data.spentPoint").value(1))
                    .andExpect(jsonPath("$.data.remainingBalance").value(6))
                    .andExpect(jsonPath("$.data.cards[0].cardType").value("REWARD"))
                    .andExpect(jsonPath("$.data.cards[1].cardType").value("MISS"))
                    .andExpect(jsonPath("$.data.cards[1].rewardItemId").value(200L))
                    .andExpect(jsonPath("$.data.cards[1].title").value("Miss Reward"))
                    .andExpect(jsonPath("$.data.cards[1].imageUrl").value("signed::reward-item/miss.png"));
        } finally {
            SecurityContextHolder.clearContext();
        }

        then(rewardGachaDrawService).should().draw(1L);
    }

    private UsernamePasswordAuthenticationToken authentication(Long userId) {
        return new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(userId, "kakao", true),
                null,
                List.of()
        );
    }
}
