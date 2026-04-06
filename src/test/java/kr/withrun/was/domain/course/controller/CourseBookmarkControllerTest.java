package kr.withrun.was.domain.course.controller;

import java.time.LocalDateTime;
import java.util.List;

import kr.withrun.was.domain.auth.security.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import kr.withrun.was.domain.course.dto.BookmarkedCourseItemResponse;
import kr.withrun.was.domain.course.dto.BookmarkedCoursesRequest;
import kr.withrun.was.domain.course.dto.BookmarkedCoursesResponse;
import kr.withrun.was.domain.course.dto.CourseBookmarkAddResponse;
import kr.withrun.was.domain.course.dto.CourseBookmarkRemoveResponse;
import kr.withrun.was.domain.course.service.CourseBookmarkService;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.domain.course.type.RouteType;
import kr.withrun.was.global.common.type.Difficulty;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.testSecurityContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CourseBookmarkController.class, properties = "spring.config.import=")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("코스 북마크 컨트롤러")
class CourseBookmarkControllerTest {

    static {
        System.setProperty("spring.cloud.aws.parameterstore.enabled", "false");
        System.setProperty("spring.config.import", "");
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseBookmarkService courseBookmarkService;

    @AfterAll
    static void clearConfigOverrides() {
        System.clearProperty("spring.cloud.aws.parameterstore.enabled");
        System.clearProperty("spring.config.import");
    }

    @AfterEach
    void clearSecurityContext() {
        TestSecurityContextHolder.clearContext();
    }

    @DisplayName("북마크 추가 응답을 성공 응답 포맷으로 감싼다")
    @Test
    void wrapsAddBookmarkResponseInSuccessEnvelope() throws Exception {
        long courseId = 10L;
        long userId = 20L;
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 12, 16, 0);
        CourseBookmarkAddResponse response = new CourseBookmarkAddResponse(courseId, true, createdAt);
        given(courseBookmarkService.addBookmark(courseId, userId)).willReturn(response);

        mockMvc.perform(post("/api/courses/{courseId}/bookmark", courseId)
                        .with(authenticated(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.OK.getMessage()))
                .andExpect(jsonPath("$.data.courseId").value(courseId))
                .andExpect(jsonPath("$.data.isBookmarked").value(true))
                .andExpect(jsonPath("$.data.createdAt").value("2026-03-12T16:00:00"));

        then(courseBookmarkService).should().addBookmark(courseId, userId);
    }

    @DisplayName("북마크 삭제 응답을 성공 응답 포맷으로 감싼다")
    @Test
    void wrapsRemoveBookmarkResponseInSuccessEnvelope() throws Exception {
        long courseId = 11L;
        long userId = 21L;
        CourseBookmarkRemoveResponse response = new CourseBookmarkRemoveResponse(courseId, false);
        given(courseBookmarkService.removeBookmark(courseId, userId)).willReturn(response);

        mockMvc.perform(delete("/api/courses/{courseId}/bookmark", courseId)
                        .with(authenticated(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.OK.getMessage()))
                .andExpect(jsonPath("$.data.courseId").value(courseId))
                .andExpect(jsonPath("$.data.isBookmarked").value(false));

        then(courseBookmarkService).should().removeBookmark(courseId, userId);
    }

    @DisplayName("북마크 목록 조회 응답을 성공 응답 포맷으로 감싼다")
    @Test
    void wrapsBookmarkedCoursesResponseInSuccessEnvelope() throws Exception {
        long userId = 20L;
        BookmarkedCoursesRequest request = new BookmarkedCoursesRequest(2, "previous-cursor");
        BookmarkedCoursesResponse response = new BookmarkedCoursesResponse(
                List.of(new BookmarkedCourseItemResponse(
                        12L,
                        LocalDateTime.of(2026, 3, 14, 10, 30),
                        42L,
                        "Han River",
                        CourseStatus.OFFICIAL,
                        RouteType.LOOP,
                        10000,
                        35,
                        new BookmarkedCourseItemResponse.DifficultyOption("MEDIUM", "보통"),
                        List.of(
                                new BookmarkedCourseItemResponse.CourseTypeOption("RIVERSIDE", "강변"),
                                new BookmarkedCourseItemResponse.CourseTypeOption("PARK", "공원")
                        ),
                        "https://example.com/course.jpg",
                        23L,
                        true,
                        true
                )),
                true,
                "next-cursor"
        );
        given(courseBookmarkService.findBookmarkedCourses(userId, request)).willReturn(response);

        mockMvc.perform(get("/api/courses/bookmarks")
                        .with(authenticated(userId))
                        .queryParam("size", "2")
                        .queryParam("cursor", "previous-cursor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.OK.getMessage()))
                .andExpect(jsonPath("$.data.items[0].bookmarkId").value(12L))
                .andExpect(jsonPath("$.data.items[0].bookmarkedAt").value("2026-03-14T10:30:00"))
                .andExpect(jsonPath("$.data.items[0].courseId").value(42L))
                .andExpect(jsonPath("$.data.items[0].title").value("Han River"))
                .andExpect(jsonPath("$.data.items[0].status").value(CourseStatus.OFFICIAL.name()))
                .andExpect(jsonPath("$.data.items[0].routeType").value(RouteType.LOOP.name()))
                .andExpect(jsonPath("$.data.items[0].difficulty.data").value("MEDIUM"))
                .andExpect(jsonPath("$.data.items[0].difficulty.label").value("보통"))
                .andExpect(jsonPath("$.data.items[0].courseTypes[0].data").value("RIVERSIDE"))
                .andExpect(jsonPath("$.data.items[0].courseTypes[0].label").value("강변"))
                .andExpect(jsonPath("$.data.items[0].likeCount").value(23L))
                .andExpect(jsonPath("$.data.items[0].isLiked").value(true))
                .andExpect(jsonPath("$.data.items[0].isBookmarked").value(true))
                .andExpect(jsonPath("$.data.hasMore").value(true))
                .andExpect(jsonPath("$.data.nextCursor").value("next-cursor"));

        then(courseBookmarkService).should().findBookmarkedCourses(userId, request);
    }

    @DisplayName("북마크 목록 size가 유효하지 않으면 잘못된 입력 응답을 반환한다")
    @ParameterizedTest(name = "size={0}")
    @ValueSource(strings = {"0", "-1", "51", "abc", "1.5"})
    void returnsInvalidInputWhenBookmarkedCoursesSizeIsInvalid(String invalidSize) throws Exception {
        mockMvc.perform(get("/api/courses/bookmarks")
                        .with(authenticated(20L))
                        .queryParam("size", invalidSize))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INPUT_VALUE.getCode()));

        then(courseBookmarkService).should(never()).findBookmarkedCourses(anyLong(), any(BookmarkedCoursesRequest.class));
    }

    @DisplayName("북마크 목록 size가 빈 문자열이면 기본값 10을 사용한다")
    @Test
    void usesDefaultSizeWhenBookmarkedCoursesSizeIsBlank() throws Exception {
        long userId = 20L;
        BookmarkedCoursesRequest request = new BookmarkedCoursesRequest(10, null);
        BookmarkedCoursesResponse response = new BookmarkedCoursesResponse(List.of(), false, null);
        given(courseBookmarkService.findBookmarkedCourses(userId, request)).willReturn(response);

        mockMvc.perform(get("/api/courses/bookmarks")
                        .with(authenticated(userId))
                        .queryParam("size", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()));

        then(courseBookmarkService).should().findBookmarkedCourses(userId, request);
    }

    @DisplayName("북마크 목록 size 숫자 변환 오류는 size 필드로 응답한다")
    @Test
    void returnsSizeFieldForBookmarkedCoursesSizeTypeMismatch() throws Exception {
        mockMvc.perform(get("/api/courses/bookmarks")
                        .with(authenticated(20L))
                        .queryParam("size", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INPUT_VALUE.getCode()))
                .andExpect(jsonPath("$.data[0].field").value("size"))
                .andExpect(jsonPath("$.data[0].rejectedValue").value("abc"));

        then(courseBookmarkService).should(never()).findBookmarkedCourses(anyLong(), any(BookmarkedCoursesRequest.class));
    }

    @DisplayName("서비스가 커서를 거부하면 잘못된 커서 응답을 반환한다")
    @Test
    void returnsInvalidCursorWhenBookmarkedCoursesServiceRejectsCursor() throws Exception {
        long userId = 20L;
        BookmarkedCoursesRequest request = new BookmarkedCoursesRequest(10, "%%%");
        given(courseBookmarkService.findBookmarkedCourses(userId, request))
                .willThrow(new CustomException(ResponseCode.INVALID_CURSOR));

        mockMvc.perform(get("/api/courses/bookmarks")
                        .with(authenticated(userId))
                        .queryParam("cursor", "%%%"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_CURSOR.getCode()));

        then(courseBookmarkService).should().findBookmarkedCourses(userId, request);
    }

    @DisplayName("없는 코스에 북마크를 추가하면 코스 없음 응답을 반환한다")
    @Test
    void returnsCourseNotFoundWhenAddingBookmarkToMissingCourse() throws Exception {
        long courseId = 999L;
        long userId = 20L;
        given(courseBookmarkService.addBookmark(courseId, userId))
                .willThrow(new CustomException(ResponseCode.COURSE_NOT_FOUND));

        mockMvc.perform(post("/api/courses/{courseId}/bookmark", courseId)
                        .with(authenticated(userId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.COURSE_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.COURSE_NOT_FOUND.getMessage()));

        then(courseBookmarkService).should().addBookmark(courseId, userId);
    }

    @DisplayName("다른 사용자의 private 코스에 북마크를 추가하면 코스 없음 응답을 반환한다")
    @Test
    void returnsCourseNotFoundWhenAddingBookmarkToPrivateCourseOwnedByAnotherUser() throws Exception {
        long courseId = 888L;
        long userId = 20L;
        given(courseBookmarkService.addBookmark(courseId, userId))
                .willThrow(new CustomException(ResponseCode.COURSE_NOT_FOUND));

        mockMvc.perform(post("/api/courses/{courseId}/bookmark", courseId)
                        .with(authenticated(userId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.COURSE_NOT_FOUND.getCode()));

        then(courseBookmarkService).should().addBookmark(courseId, userId);
    }

    @DisplayName("없는 사용자가 북마크를 삭제하면 사용자 없음 응답을 반환한다")
    @Test
    void returnsUserNotFoundWhenRemovingBookmarkForMissingUser() throws Exception {
        long courseId = 12L;
        long userId = 999L;
        given(courseBookmarkService.removeBookmark(courseId, userId))
                .willThrow(new CustomException(ResponseCode.USER_NOT_FOUND));

        mockMvc.perform(delete("/api/courses/{courseId}/bookmark", courseId)
                        .with(authenticated(userId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.USER_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.USER_NOT_FOUND.getMessage()));

        then(courseBookmarkService).should().removeBookmark(courseId, userId);
    }

    @DisplayName("다른 사용자의 private 코스 북마크 해제는 코스 없음 응답을 반환한다")
    @Test
    void returnsCourseNotFoundWhenRemovingBookmarkFromPrivateCourseOwnedByAnotherUser() throws Exception {
        long courseId = 889L;
        long userId = 20L;
        given(courseBookmarkService.removeBookmark(courseId, userId))
                .willThrow(new CustomException(ResponseCode.COURSE_NOT_FOUND));

        mockMvc.perform(delete("/api/courses/{courseId}/bookmark", courseId)
                        .with(authenticated(userId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.COURSE_NOT_FOUND.getCode()));

        then(courseBookmarkService).should().removeBookmark(courseId, userId);
    }

    private Authentication authenticatedUser(Long userId) {
        return UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedUser(userId, "google", true),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    private RequestPostProcessor authenticated(Long userId) {
        return request -> {
            TestSecurityContextHolder.setAuthentication(authenticatedUser(userId));
            return testSecurityContext().postProcessRequest(request);
        };
    }
}
