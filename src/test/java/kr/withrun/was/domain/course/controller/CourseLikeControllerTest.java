package kr.withrun.was.domain.course.controller;

import kr.withrun.was.domain.auth.security.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import kr.withrun.was.domain.course.dto.CourseLikeStatusResponse;
import kr.withrun.was.domain.course.service.CourseLikeService;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.testSecurityContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

@WebMvcTest(controllers = CourseLikeController.class, properties = "spring.config.import=")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("코스 좋아요 컨트롤러")
class CourseLikeControllerTest {

    static {
        System.setProperty("spring.cloud.aws.parameterstore.enabled", "false");
        System.setProperty("spring.config.import", "");
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseLikeService courseLikeService;

    @AfterAll
    static void clearConfigOverrides() {
        System.clearProperty("spring.cloud.aws.parameterstore.enabled");
        System.clearProperty("spring.config.import");
    }

    @AfterEach
    void clearSecurityContext() {
        TestSecurityContextHolder.clearContext();
    }

    @DisplayName("코스 좋아요 응답을 성공 응답 포맷으로 감싼다")
    @Test
    void wrapsLikeCourseResponseInSuccessEnvelope() throws Exception {
        long courseId = 1L;
        long userId = 10L;
        CourseLikeStatusResponse response = new CourseLikeStatusResponse(courseId, true, 14L, CourseStatus.OFFICIAL);
        given(courseLikeService.likeCourse(courseId, userId)).willReturn(response);

        mockMvc.perform(post("/api/courses/{courseId}/like", courseId)
                        .with(authenticated(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.OK.getMessage()))
                .andExpect(jsonPath("$.data.courseId").value(courseId))
                .andExpect(jsonPath("$.data.isLiked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(14L))
                .andExpect(jsonPath("$.data.status").value(CourseStatus.OFFICIAL.name()));

        then(courseLikeService).should().likeCourse(courseId, userId);
    }

    @DisplayName("코스 좋아요 취소 응답을 성공 응답 포맷으로 감싼다")
    @Test
    void wrapsUnlikeCourseResponseInSuccessEnvelope() throws Exception {
        long courseId = 1L;
        long userId = 10L;
        CourseLikeStatusResponse response = new CourseLikeStatusResponse(courseId, false, 13L, CourseStatus.OFFICIAL);
        given(courseLikeService.unlikeCourse(courseId, userId)).willReturn(response);

        mockMvc.perform(delete("/api/courses/{courseId}/like", courseId)
                        .with(authenticated(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.OK.getMessage()))
                .andExpect(jsonPath("$.data.courseId").value(courseId))
                .andExpect(jsonPath("$.data.isLiked").value(false))
                .andExpect(jsonPath("$.data.likeCount").value(13L))
                .andExpect(jsonPath("$.data.status").value(CourseStatus.OFFICIAL.name()));

        then(courseLikeService).should().unlikeCourse(courseId, userId);
    }

    @DisplayName("존재하지 않는 코스에 좋아요를 누르면 코스 없음 응답을 반환한다")
    @Test
    void returnsCourseNotFoundWhenLikeCourseTargetDoesNotExist() throws Exception {
        long courseId = 999L;
        long userId = 10L;
        given(courseLikeService.likeCourse(courseId, userId))
                .willThrow(new CustomException(ResponseCode.COURSE_NOT_FOUND));

        mockMvc.perform(post("/api/courses/{courseId}/like", courseId)
                        .with(authenticated(userId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.COURSE_NOT_FOUND.getCode()));

        then(courseLikeService).should().likeCourse(courseId, userId);
    }

    @DisplayName("다른 사용자의 private 코스에 좋아요를 누르면 코스 없음 응답을 반환한다")
    @Test
    void returnsCourseNotFoundWhenLikingPrivateCourseOwnedByAnotherUser() throws Exception {
        long courseId = 777L;
        long userId = 10L;
        given(courseLikeService.likeCourse(courseId, userId))
                .willThrow(new CustomException(ResponseCode.COURSE_NOT_FOUND));

        mockMvc.perform(post("/api/courses/{courseId}/like", courseId)
                        .with(authenticated(userId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.COURSE_NOT_FOUND.getCode()));

        then(courseLikeService).should().likeCourse(courseId, userId);
    }

    @DisplayName("존재하지 않는 사용자의 좋아요 취소는 사용자 없음 응답을 반환한다")
    @Test
    void returnsUserNotFoundWhenUnlikeCourseUserDoesNotExist() throws Exception {
        long courseId = 1L;
        long userId = 999L;
        given(courseLikeService.unlikeCourse(courseId, userId))
                .willThrow(new CustomException(ResponseCode.USER_NOT_FOUND));

        mockMvc.perform(delete("/api/courses/{courseId}/like", courseId)
                        .with(authenticated(userId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.USER_NOT_FOUND.getCode()));

        then(courseLikeService).should().unlikeCourse(courseId, userId);
    }

    @DisplayName("다른 사용자의 private 코스 좋아요 취소는 코스 없음 응답을 반환한다")
    @Test
    void returnsCourseNotFoundWhenUnlikingPrivateCourseOwnedByAnotherUser() throws Exception {
        long courseId = 778L;
        long userId = 10L;
        given(courseLikeService.unlikeCourse(courseId, userId))
                .willThrow(new CustomException(ResponseCode.COURSE_NOT_FOUND));

        mockMvc.perform(delete("/api/courses/{courseId}/like", courseId)
                        .with(authenticated(userId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.COURSE_NOT_FOUND.getCode()));

        then(courseLikeService).should().unlikeCourse(courseId, userId);
    }

    private Authentication authenticatedUser(Long userId) {
        return UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedUser(userId, "google", true),
                null,
                java.util.List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    private RequestPostProcessor authenticated(Long userId) {
        return request -> {
            TestSecurityContextHolder.setAuthentication(authenticatedUser(userId));
            return testSecurityContext().postProcessRequest(request);
        };
    }
}
