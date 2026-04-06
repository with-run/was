package kr.withrun.was.domain.navigation.controller;

import kr.withrun.was.domain.navigation.dto.GenerateNavigationBundleResponse;
import kr.withrun.was.domain.navigation.dto.GetLatestNavigationBundleResponse;
import kr.withrun.was.domain.navigation.service.NavigationBundleService;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NavigationController.class, properties = "spring.config.import=")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("네비게이션 API 컨트롤러")
class NavigationControllerTest {

    static {
        System.setProperty("spring.cloud.aws.parameterstore.enabled", "false");
        System.setProperty("spring.config.import", "");
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NavigationBundleService navigationBundleService;

    @AfterAll
    static void clearConfigOverrides() {
        System.clearProperty("spring.cloud.aws.parameterstore.enabled");
        System.clearProperty("spring.config.import");
    }

    @DisplayName("생성 요청을 동기 처리하면 200 성공 응답을 반환한다")
    @Test
    void returnsAcceptedWhenGenerateLatestNavigationBundleIsRequested() throws Exception {
        long courseId = 42L;
        GenerateNavigationBundleResponse response = new GenerateNavigationBundleResponse(courseId, "READY");
        given(navigationBundleService.generateBundle(courseId)).willReturn(response);

        mockMvc.perform(post("/api/admin/courses/{courseId}/navigation-bundles:generate", courseId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()))
                .andExpect(jsonPath("$.data.courseId").value(courseId))
                .andExpect(jsonPath("$.data.bundleStatus").value("READY"));

        then(navigationBundleService).should().generateBundle(courseId);
    }

    @DisplayName("latest bundle 이 준비되면 200 READY 응답을 반환한다")
    @Test
    void returnsReadyLatestNavigationBundle() throws Exception {
        long courseId = 42L;
        GetLatestNavigationBundleResponse response = new GetLatestNavigationBundleResponse(
                courseId,
                "READY",
                "https://cdn.withrun.kr/navigation/latest/42.json?Expires=100&Signature=test-signature&Key-Pair-Id=K123"
        );
        given(navigationBundleService.getLatestBundle(courseId)).willReturn(response);

        mockMvc.perform(get("/api/courses/{courseId}/navigation-bundle", courseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()))
                .andExpect(jsonPath("$.data.status").value("READY"))
                .andExpect(jsonPath("$.data.downloadUrl").value("https://cdn.withrun.kr/navigation/latest/42.json?Expires=100&Signature=test-signature&Key-Pair-Id=K123"));

        then(navigationBundleService).should().getLatestBundle(courseId);
    }

    @DisplayName("latest bundle 이 준비 중이면 202 PENDING 실패 응답을 반환한다")
    @Test
    void returnsPendingLatestNavigationBundle() throws Exception {
        long courseId = 42L;
        GetLatestNavigationBundleResponse response = new GetLatestNavigationBundleResponse(
                courseId,
                "PENDING",
                null
        );
        given(navigationBundleService.getLatestBundle(courseId)).willReturn(response);

        mockMvc.perform(get("/api/courses/{courseId}/navigation-bundle", courseId))
                .andDo(print())
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("E600"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.downloadUrl").doesNotExist());

        then(navigationBundleService).should().getLatestBundle(courseId);
    }

    @DisplayName("동기 생성이 실패하면 503 실패 응답을 반환한다")
    @Test
    void returnsFailedWhenGenerateLatestNavigationBundleFails() throws Exception {
        long courseId = 42L;
        given(navigationBundleService.generateBundle(courseId))
                .willThrow(new CustomException(ResponseCode.NAVIGATION_BUNDLE_FAILED));

        mockMvc.perform(post("/api/admin/courses/{courseId}/navigation-bundles:generate", courseId))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.NAVIGATION_BUNDLE_FAILED.getCode()));

        then(navigationBundleService).should().generateBundle(courseId);
    }

    @DisplayName("코스가 없으면 404 응답을 반환한다")
    @Test
    void returnsCourseNotFoundWhenCourseDoesNotExist() throws Exception {
        long courseId = 404L;
        given(navigationBundleService.getLatestBundle(courseId))
                .willThrow(new CustomException(ResponseCode.COURSE_NOT_FOUND));

        mockMvc.perform(get("/api/courses/{courseId}/navigation-bundle", courseId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.COURSE_NOT_FOUND.getCode()));

        then(navigationBundleService).should().getLatestBundle(courseId);
    }

    @DisplayName("reroute endpoint 는 노출하지 않는다")
    @Test
    void doesNotExposeRerouteEndpoint() throws Exception {
        mockMvc.perform(post("/api/navigation/reroutes")
                        .contentType("application/json")
                        .content("{}"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.ENTITY_NOT_FOUND.getCode()));

        then(navigationBundleService).shouldHaveNoInteractions();
    }
}
