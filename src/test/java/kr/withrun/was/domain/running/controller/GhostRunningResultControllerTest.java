package kr.withrun.was.domain.running.controller;

import kr.withrun.was.domain.running.dto.GhostRunningResultResponse;
import kr.withrun.was.domain.running.service.GhostRunningResultService;
import kr.withrun.was.domain.running.type.GhostResultStatus;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import java.time.LocalDateTime;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(controllers = GhostRunningResultController.class, properties = "spring.config.import=")
@DisplayName("고스트 러닝 결과 컨트롤러")
class GhostRunningResultControllerTest {

    static {
        System.setProperty("spring.cloud.aws.parameterstore.enabled", "false");
        System.setProperty("spring.config.import", "");
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GhostRunningResultService ghostRunningResultService;

    @AfterAll
    static void clearConfigOverrides() {
        System.clearProperty("spring.cloud.aws.parameterstore.enabled");
        System.clearProperty("spring.config.import");
    }

    @DisplayName("고스트 러닝 결과 조회 응답을 성공 응답 포맷으로 감싼다")
    @Test
    void wrapsGhostRunningResultResponseInSuccessEnvelope() throws Exception {
        long runningSessionId = 901L;
        GhostRunningResultResponse response = new GhostRunningResultResponse(
                77L,
                runningSessionId,
                345L,
                202L,
                GhostResultStatus.WIN,
                2140,
                14,
                32,
                LocalDateTime.of(2026, 3, 8, 8, 15)
        );
        given(ghostRunningResultService.findGhostRunningResult(runningSessionId)).willReturn(response);

        mockMvc.perform(get("/api/ghost-runnings/{runningSessionId}/result", runningSessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.OK.getMessage()))
                .andExpect(jsonPath("$.data.ghostRunningResultId").value(77L))
                .andExpect(jsonPath("$.data.runningSessionId").value(runningSessionId))
                .andExpect(jsonPath("$.data.ghostTargetRunningSessionId").value(345L))
                .andExpect(jsonPath("$.data.targetUserId").value(202L))
                .andExpect(jsonPath("$.data.resultStatus").value(GhostResultStatus.WIN.name()))
                .andExpect(jsonPath("$.data.point").value(2140))
                .andExpect(jsonPath("$.data.timeGapSec").value(14))
                .andExpect(jsonPath("$.data.distanceGapM").value(32))
                .andExpect(jsonPath("$.data.createdAt").value("2026-03-08T08:15:00"));

        then(ghostRunningResultService).should().findGhostRunningResult(runningSessionId);
    }

    @DisplayName("고스트 러닝 결과가 없으면 찾을 수 없음 응답을 반환한다")
    @Test
    void returnsNotFoundWhenGhostRunningResultDoesNotExist() throws Exception {
        long runningSessionId = 901L;
        given(ghostRunningResultService.findGhostRunningResult(runningSessionId))
                .willThrow(new CustomException(ResponseCode.GHOST_RUNNING_RESULT_NOT_FOUND));

        mockMvc.perform(get("/api/ghost-runnings/{runningSessionId}/result", runningSessionId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.GHOST_RUNNING_RESULT_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.GHOST_RUNNING_RESULT_NOT_FOUND.getMessage()));

        then(ghostRunningResultService).should().findGhostRunningResult(runningSessionId);
    }

}
