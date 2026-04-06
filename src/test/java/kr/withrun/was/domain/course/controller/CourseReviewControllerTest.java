package kr.withrun.was.domain.course.controller;

import kr.withrun.was.domain.auth.jwt.JwtProvider;
import kr.withrun.was.domain.course.dto.CreateCourseReviewRequest;
import kr.withrun.was.domain.course.dto.CreateCourseReviewResponse;
import kr.withrun.was.domain.course.service.CourseReviewService;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.type.Gender;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        properties = {
                "spring.config.import=",
                "spring.cloud.aws.parameterstore.enabled=false"
        }
)
@AutoConfigureMockMvc
@DisplayName("코스 리뷰 컨트롤러")
class CourseReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private CourseReviewService courseReviewService;

    @DisplayName("코스 리뷰 생성 응답을 생성 성공 포맷으로 감싼다")
    @Test
    void wrapsCreateCourseReviewResponseInCreatedEnvelope() throws Exception {
        long currentUserId = 1L;
        long courseId = 101L;
        CreateCourseReviewRequest request = validRequest();
        CreateCourseReviewResponse response = new CreateCourseReviewResponse(
                501L,
                courseId,
                5,
                List.of(
                        new CreateCourseReviewResponse.CourseTypeOption("RIVERSIDE", "강변"),
                        new CreateCourseReviewResponse.CourseTypeOption("URBAN", "도심")
                ),
                new CreateCourseReviewResponse.DifficultyOption("MEDIUM", "보통"),
                LocalDateTime.of(2026, 3, 8, 21, 0)
        );
        given(courseReviewService.createCourseReview(courseId, currentUserId, request)).willReturn(response);

        mockMvc.perform(post("/api/courses/{courseId}/reviews", courseId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(currentUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.CREATED.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.CREATED.getMessage()))
                .andExpect(jsonPath("$.data.courseReviewId").value(501L))
                .andExpect(jsonPath("$.data.courseId").value(courseId))
                .andExpect(jsonPath("$.data.rating").value(5))
                .andExpect(jsonPath("$.data.courseTypes[0].data").value("RIVERSIDE"))
                .andExpect(jsonPath("$.data.courseTypes[0].label").value("강변"))
                .andExpect(jsonPath("$.data.courseTypes[1].data").value("URBAN"))
                .andExpect(jsonPath("$.data.courseTypes[1].label").value("도심"))
                .andExpect(jsonPath("$.data.submittedDifficulty.data").value("MEDIUM"))
                .andExpect(jsonPath("$.data.submittedDifficulty.label").value("보통"))
                .andExpect(jsonPath("$.data.createdAt").value("2026-03-08T21:00:00"));

        then(courseReviewService).should().createCourseReview(courseId, currentUserId, request);
    }

    @DisplayName("필수 요청 필드가 없으면 잘못된 입력 응답을 반환한다")
    @Test
    void returnsInvalidInputValueWhenRequiredFieldIsMissing() throws Exception {
        mockMvc.perform(post("/api/courses/{courseId}/reviews", 101L)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courseTypes": ["RIVERSIDE"],
                                  "submittedDifficulty": "MEDIUM"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INPUT_VALUE.getCode()))
                .andExpect(jsonPath("$.data[0].field").value("rating"));

        then(courseReviewService).shouldHaveNoInteractions();
    }

    @DisplayName("이미 작성한 리뷰면 충돌 응답을 반환한다")
    @Test
    void returnsConflictWhenReviewAlreadyExists() throws Exception {
        long currentUserId = 1L;
        long courseId = 101L;
        CreateCourseReviewRequest request = validRequest();
        given(courseReviewService.createCourseReview(courseId, currentUserId, request))
                .willThrow(new CustomException(ResponseCode.REVIEW_ALREADY_EXISTS));

        mockMvc.perform(post("/api/courses/{courseId}/reviews", courseId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(currentUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.REVIEW_ALREADY_EXISTS.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.REVIEW_ALREADY_EXISTS.getMessage()));

        then(courseReviewService).should().createCourseReview(courseId, currentUserId, request);
    }

    @DisplayName("다른 사용자의 private 코스에 리뷰를 생성하면 코스 없음 응답을 반환한다")
    @Test
    void returnsCourseNotFoundWhenCreatingReviewForPrivateCourseOwnedByAnotherUser() throws Exception {
        long currentUserId = 1L;
        long courseId = 102L;
        CreateCourseReviewRequest request = validRequest();
        given(courseReviewService.createCourseReview(courseId, currentUserId, request))
                .willThrow(new CustomException(ResponseCode.COURSE_NOT_FOUND));

        mockMvc.perform(post("/api/courses/{courseId}/reviews", courseId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(currentUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.COURSE_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.COURSE_NOT_FOUND.getMessage()));

        then(courseReviewService).should().createCourseReview(courseId, currentUserId, request);
    }

    private static CreateCourseReviewRequest validRequest() {
        return new CreateCourseReviewRequest(5, List.of("RIVERSIDE", "URBAN"), "MEDIUM");
    }

    private static String validRequestBody() {
        return """
                {
                  "rating": 5,
                  "courseTypes": ["RIVERSIDE", "URBAN"],
                  "submittedDifficulty": "MEDIUM"
                }
                """;
    }

    private String bearerToken(Long userId) {
        return "Bearer " + jwtProvider.generateTokenPair(completedUser(userId), "google").accessToken();
    }

    private User completedUser(Long id) {
        User user = User.createPendingSocialUser();
        setField(user, "id", id);
        user.completeProfile(
                "runner",
                LocalDate.of(1999, 1, 2),
                Gender.MALE,
                180.0,
                72.5
        );
        return user;
    }

    private void setField(Object target, String fieldName, Object value) {
        Class<?> currentClass = target.getClass();
        while (currentClass != null) {
            try {
                Field field = currentClass.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException exception) {
                currentClass = currentClass.getSuperclass();
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException("Failed to set field " + fieldName, exception);
            }
        }

        throw new IllegalArgumentException("Field not found: " + fieldName);
    }
}
