package kr.withrun.was.domain.reward.controller;

import kr.withrun.was.domain.reward.dto.RewardItemListResponse;
import kr.withrun.was.domain.reward.dto.RewardItemResponse;
import kr.withrun.was.domain.reward.dto.UpdateRewardItemRequest;
import kr.withrun.was.domain.reward.service.RewardItemAdminService;
import kr.withrun.was.global.response.ResponseCode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RewardItemAdminController.class, properties = "spring.config.import=")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("리워드 아이템 관리자 컨트롤러")
class RewardItemAdminControllerTest {

    static {
        System.setProperty("spring.cloud.aws.parameterstore.enabled", "false");
        System.setProperty("spring.config.import", "");
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RewardItemAdminService rewardItemAdminService;

    @AfterAll
    static void clearConfigOverrides() {
        System.clearProperty("spring.cloud.aws.parameterstore.enabled");
        System.clearProperty("spring.config.import");
    }

    @DisplayName("POST /api/admin/reward-items 는 생성 응답을 ApiResponse 래퍼로 반환한다")
    @Test
    void wrapsCreateRewardItemResponseInCreatedEnvelope() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "reward.png", "image/png", "image".getBytes());
        MockMultipartFile titlePart = new MockMultipartFile(
                "title",
                "",
                MediaType.TEXT_PLAIN_VALUE,
                "Reward Item".getBytes(StandardCharsets.UTF_8)
        );
        given(rewardItemAdminService.createRewardItem(eq("Reward Item"), any()))
                .willReturn(rewardItemResponse(1L, "Reward Item", "https://cdn.example.com/reward-item/1.png", true));

        mockMvc.perform(multipart("/api/admin/reward-items")
                        .file(file)
                        .file(titlePart))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.CREATED.getCode()))
                .andExpect(jsonPath("$.data.rewardItemId").value(1L))
                .andExpect(jsonPath("$.data.title").value("Reward Item"));

        then(rewardItemAdminService).should().createRewardItem(eq("Reward Item"), any());
    }

    @DisplayName("GET /api/admin/reward-items 는 목록 응답을 ApiResponse 래퍼로 반환한다")
    @Test
    void wrapsGetRewardItemsResponseInSuccessEnvelope() throws Exception {
        given(rewardItemAdminService.getRewardItems(true, 0, 20))
                .willReturn(new RewardItemListResponse(
                        List.of(rewardItemResponse(1L, "Reward Item", "https://cdn.example.com/reward-item/1.png", true)),
                        0,
                        20,
                        1,
                        1,
                        false
                ));

        mockMvc.perform(get("/api/admin/reward-items")
                        .param("active", "true")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()))
                .andExpect(jsonPath("$.data.items[0].rewardItemId").value(1L));

        then(rewardItemAdminService).should().getRewardItems(true, 0, 20);
    }

    @DisplayName("GET /api/admin/reward-items/{rewardItemId} 는 단건 응답을 ApiResponse 래퍼로 반환한다")
    @Test
    void wrapsGetRewardItemResponseInSuccessEnvelope() throws Exception {
        given(rewardItemAdminService.getRewardItem(1L))
                .willReturn(rewardItemResponse(1L, "Reward Item", "https://cdn.example.com/reward-item/1.png", true));

        mockMvc.perform(get("/api/admin/reward-items/{rewardItemId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.rewardItemId").value(1L))
                .andExpect(jsonPath("$.data.imageUrl").value("https://cdn.example.com/reward-item/1.png"));

        then(rewardItemAdminService).should().getRewardItem(1L);
    }

    @DisplayName("PATCH /api/admin/reward-items/{rewardItemId} 는 수정 응답을 ApiResponse 래퍼로 반환한다")
    @Test
    void wrapsPatchRewardItemResponseInSuccessEnvelope() throws Exception {
        given(rewardItemAdminService.updateRewardItem(eq(1L), any(UpdateRewardItemRequest.class)))
                .willReturn(rewardItemResponse(1L, "Updated Reward", "https://cdn.example.com/reward-item/1.png", false));

        mockMvc.perform(patch("/api/admin/reward-items/{rewardItemId}", 1L)
                        .contentType("application/json")
                        .content("{\"title\":\"Updated Reward\",\"isActive\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Updated Reward"))
                .andExpect(jsonPath("$.data.isActive").value(false));

        then(rewardItemAdminService).should().updateRewardItem(eq(1L), any(UpdateRewardItemRequest.class));
    }

    @DisplayName("PUT /api/admin/reward-items/{rewardItemId}/image 는 이미지 교체 응답을 ApiResponse 래퍼로 반환한다")
    @Test
    void wrapsReplaceRewardItemImageResponseInSuccessEnvelope() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "reward.png", "image/png", "image".getBytes());
        given(rewardItemAdminService.updateRewardItemImage(eq(1L), any()))
                .willReturn(rewardItemResponse(1L, "Reward Item", "https://cdn.example.com/reward-item/1.png", true));

        mockMvc.perform(multipart("/api/admin/reward-items/{rewardItemId}/image", 1L)
                        .file(file)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.rewardItemId").value(1L));

        then(rewardItemAdminService).should().updateRewardItemImage(eq(1L), any());
    }

    private RewardItemResponse rewardItemResponse(Long id, String title, String imageUrl, boolean isActive) {
        return new RewardItemResponse(
                id,
                title,
                imageUrl,
                isActive,
                LocalDateTime.of(2026, 3, 29, 10, 0),
                LocalDateTime.of(2026, 3, 29, 10, 30)
        );
    }
}
