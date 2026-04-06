package kr.withrun.was.domain.reward.controller;

import kr.withrun.was.domain.reward.dto.CreateRewardDrawPoolRequest;
import kr.withrun.was.domain.reward.dto.RewardDrawPoolItemRequest;
import kr.withrun.was.domain.reward.dto.RewardDrawPoolItemResponse;
import kr.withrun.was.domain.reward.dto.RewardDrawPoolListResponse;
import kr.withrun.was.domain.reward.dto.RewardDrawPoolResponse;
import kr.withrun.was.domain.reward.dto.UpdateRewardDrawPoolRequest;
import kr.withrun.was.domain.reward.service.RewardDrawPoolAdminService;
import kr.withrun.was.global.response.ResponseCode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RewardDrawPoolAdminController.class, properties = "spring.config.import=")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("리워드 draw pool 관리자 컨트롤러")
class RewardDrawPoolAdminControllerTest {

    static {
        System.setProperty("spring.cloud.aws.parameterstore.enabled", "false");
        System.setProperty("spring.config.import", "");
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RewardDrawPoolAdminService rewardDrawPoolAdminService;

    @AfterAll
    static void clearConfigOverrides() {
        System.clearProperty("spring.cloud.aws.parameterstore.enabled");
        System.clearProperty("spring.config.import");
    }

    @DisplayName("POST /api/admin/reward-draw-pools 는 생성 응답을 ApiResponse 래퍼로 반환한다")
    @Test
    void wrapsCreateRewardDrawPoolResponseInCreatedEnvelope() throws Exception {
        doAnswer(invocation -> {
            CreateRewardDrawPoolRequest request = invocation.getArgument(0);
            assertRequestExposesMissRewardItemId(request, 9L);
            return rewardDrawPoolResponse(1L, "Launch Pool", false, 9L);
        }).when(rewardDrawPoolAdminService).createRewardDrawPool(any(CreateRewardDrawPoolRequest.class));

        mockMvc.perform(post("/api/admin/reward-draw-pools")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Launch Pool",
                                  "cardsPerDraw": 3,
                                  "missWeight": 40,
                                  "missRewardItemId": 9,
                                  "items": [
                                    {"rewardItemId": 1, "weight": 70},
                                    {"rewardItemId": 2, "weight": 30}
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.CREATED.getCode()))
                .andExpect(jsonPath("$.data.rewardDrawPoolId").value(1L))
                .andExpect(jsonPath("$.data.cardsPerDraw").value(3))
                .andExpect(jsonPath("$.data.missWeight").value(40))
                .andExpect(jsonPath("$.data.missRewardItemId").value(9L));

        then(rewardDrawPoolAdminService).should().createRewardDrawPool(any(CreateRewardDrawPoolRequest.class));
    }

    @DisplayName("GET /api/admin/reward-draw-pools 는 목록 응답을 ApiResponse 래퍼로 반환한다")
    @Test
    void wrapsGetRewardDrawPoolsResponseInSuccessEnvelope() throws Exception {
        given(rewardDrawPoolAdminService.getRewardDrawPools(0, 20))
                .willReturn(new RewardDrawPoolListResponse(List.of(rewardDrawPoolResponse(1L, "Launch Pool", true, 9L)), 0, 20, 1, 1, false));

        mockMvc.perform(get("/api/admin/reward-draw-pools")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()))
                .andExpect(jsonPath("$.data.items[0].rewardDrawPoolId").value(1L));

        then(rewardDrawPoolAdminService).should().getRewardDrawPools(0, 20);
    }

    @DisplayName("GET /api/admin/reward-draw-pools/{rewardDrawPoolId} 는 단건 응답을 ApiResponse 래퍼로 반환한다")
    @Test
    void wrapsGetRewardDrawPoolResponseInSuccessEnvelope() throws Exception {
        given(rewardDrawPoolAdminService.getRewardDrawPool(1L))
                .willReturn(rewardDrawPoolResponse(1L, "Launch Pool", true, 9L));

        mockMvc.perform(get("/api/admin/reward-draw-pools/{rewardDrawPoolId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.rewardDrawPoolId").value(1L))
                .andExpect(jsonPath("$.data.cardsPerDraw").value(3))
                .andExpect(jsonPath("$.data.missRewardItemId").value(9L))
                .andExpect(jsonPath("$.data.items[0].weight").value(70));

        then(rewardDrawPoolAdminService).should().getRewardDrawPool(1L);
    }

    @DisplayName("PATCH /api/admin/reward-draw-pools/{rewardDrawPoolId} 는 수정 응답을 ApiResponse 래퍼로 반환한다")
    @Test
    void wrapsPatchRewardDrawPoolResponseInSuccessEnvelope() throws Exception {
        doAnswer(invocation -> {
            UpdateRewardDrawPoolRequest request = invocation.getArgument(1);
            assertRequestExposesMissRewardItemId(request, 5L);
            return rewardDrawPoolResponse(1L, "Updated Pool", false, 5L);
        }).when(rewardDrawPoolAdminService).updateRewardDrawPool(eq(1L), any(UpdateRewardDrawPoolRequest.class));

        mockMvc.perform(patch("/api/admin/reward-draw-pools/{rewardDrawPoolId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Updated Pool",
                                  "cardsPerDraw": 5,
                                  "missWeight": 25,
                                  "missRewardItemId": 5,
                                  "items": [
                                    {"rewardItemId": 1, "weight": 100}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Updated Pool"))
                .andExpect(jsonPath("$.data.cardsPerDraw").value(3))
                .andExpect(jsonPath("$.data.missWeight").value(40))
                .andExpect(jsonPath("$.data.missRewardItemId").value(5L));

        then(rewardDrawPoolAdminService).should().updateRewardDrawPool(eq(1L), any(UpdateRewardDrawPoolRequest.class));
    }

    @DisplayName("POST /api/admin/reward-draw-pools/{rewardDrawPoolId}/activate 는 활성화 응답을 ApiResponse 래퍼로 반환한다")
    @Test
    void wrapsActivateRewardDrawPoolResponseInSuccessEnvelope() throws Exception {
        given(rewardDrawPoolAdminService.activateRewardDrawPool(1L))
                .willReturn(rewardDrawPoolResponse(1L, "Launch Pool", true, 9L));

        mockMvc.perform(post("/api/admin/reward-draw-pools/{rewardDrawPoolId}/activate", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isActive").value(true));

        then(rewardDrawPoolAdminService).should().activateRewardDrawPool(1L);
    }

    @DisplayName("GET /api/admin/reward-draw-pools/active 는 현재 활성 pool 응답을 ApiResponse 래퍼로 반환한다")
    @Test
    void wrapsGetActiveRewardDrawPoolResponseInSuccessEnvelope() throws Exception {
        given(rewardDrawPoolAdminService.getActiveRewardDrawPool())
                .willReturn(rewardDrawPoolResponse(1L, "Launch Pool", true, 9L));

        mockMvc.perform(get("/api/admin/reward-draw-pools/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.rewardDrawPoolId").value(1L));

        then(rewardDrawPoolAdminService).should().getActiveRewardDrawPool();
    }

    private RewardDrawPoolResponse rewardDrawPoolResponse(Long id, String name, boolean isActive, Long missRewardItemId) {
        return new RewardDrawPoolResponse(
                id,
                name,
                isActive,
                3,
                40,
                missRewardItemId,
                List.of(new RewardDrawPoolItemResponse(101L, 1L, "Reward Item", "https://cdn.example.com/reward-item/1.png", 70)),
                LocalDateTime.of(2026, 3, 29, 10, 0),
                LocalDateTime.of(2026, 3, 29, 10, 30)
        );
    }

    private void assertRequestExposesMissRewardItemId(Object request, Long expectedValue) {
        try {
            Object value = request.getClass().getMethod("missRewardItemId").invoke(request);
            if (!expectedValue.equals(value)) {
                throw new AssertionError("Expected missRewardItemId " + expectedValue + " but was " + value);
            }
        } catch (InvocationTargetException exception) {
            throw new IllegalStateException("Failed to invoke missRewardItemId", exception);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Request DTO must expose missRewardItemId", exception);
        }
    }
}
