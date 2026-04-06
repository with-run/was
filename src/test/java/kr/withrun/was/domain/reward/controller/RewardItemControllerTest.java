package kr.withrun.was.domain.reward.controller;

import kr.withrun.was.domain.reward.dto.RewardItemShowcaseListResponse;
import kr.withrun.was.domain.reward.dto.RewardItemShowcaseResponse;
import kr.withrun.was.domain.reward.service.RewardItemQueryService;
import kr.withrun.was.global.response.ResponseCode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RewardItemController.class, properties = "spring.config.import=")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("리워드 아이템 공개 컨트롤러")
class RewardItemControllerTest {

    static {
        System.setProperty("spring.cloud.aws.parameterstore.enabled", "false");
        System.setProperty("spring.config.import", "");
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RewardItemQueryService rewardItemQueryService;

    @AfterAll
    static void clearConfigOverrides() {
        System.clearProperty("spring.cloud.aws.parameterstore.enabled");
        System.clearProperty("spring.config.import");
    }

    @DisplayName("GET /api/reward-items/showcase 는 성공 응답 래퍼와 최소 response shape 를 반환한다")
    @Test
    void wrapsShowcaseResponseInSuccessEnvelope() throws Exception {
        given(rewardItemQueryService.getRewardItemShowcase())
                .willReturn(new RewardItemShowcaseListResponse(
                        List.of(
                                new RewardItemShowcaseResponse(2L, "Newest Reward", "https://cdn.example.com/reward-item/2.png"),
                                new RewardItemShowcaseResponse(1L, "Older Reward", "https://cdn.example.com/reward-item/1.png")
                        )
                ));

        mockMvc.perform(get("/api/reward-items/showcase"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.OK.getMessage()))
                .andExpect(jsonPath("$.data.items[0].rewardItemId").value(2L))
                .andExpect(jsonPath("$.data.items[0].title").value("Newest Reward"))
                .andExpect(jsonPath("$.data.items[0].imageUrl").value("https://cdn.example.com/reward-item/2.png"))
                .andExpect(jsonPath("$.data.items[0].isActive").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].createdAt").doesNotExist())
                .andExpect(jsonPath("$.data.items.length()").value(2));

        then(rewardItemQueryService).should().getRewardItemShowcase();
    }
}
