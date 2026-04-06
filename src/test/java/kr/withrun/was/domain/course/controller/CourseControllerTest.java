package kr.withrun.was.domain.course.controller;

import kr.withrun.was.domain.course.dto.CourseDetailResponse;
import kr.withrun.was.domain.course.dto.CourseGhostDetailResponse;
import kr.withrun.was.domain.course.dto.CourseGhostLeaderboardItemResponse;
import kr.withrun.was.domain.course.dto.CourseGhostLeaderboardRequest;
import kr.withrun.was.domain.course.dto.CourseGhostLeaderboardResponse;
import kr.withrun.was.domain.course.dto.NearbyCourseItemResponse;
import kr.withrun.was.domain.course.dto.NearbyCoursesRequest;
import kr.withrun.was.domain.course.dto.NearbyCoursesResponse;
import kr.withrun.was.domain.course.dto.NearbyGhostCoursesRequest;
import kr.withrun.was.domain.course.dto.NearbyGhostCoursesResponse;
import kr.withrun.was.domain.course.dto.PreferredDistanceRange;
import kr.withrun.was.domain.auth.security.AuthenticatedUser;
import kr.withrun.was.domain.course.service.CourseService;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.type.NearbyCourseSortBy;
import kr.withrun.was.domain.course.type.NearbyGhostCourseSortBy;
import kr.withrun.was.domain.course.type.RouteType;
import kr.withrun.was.domain.course.vo.Coordinates;
import kr.withrun.was.domain.course.vo.GeoPoint;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.testSecurityContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CourseController.class, properties = "spring.config.import=")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("코스 조회 컨트롤러")
class CourseControllerTest {

    static {
        System.setProperty("spring.cloud.aws.parameterstore.enabled", "false");
        System.setProperty("spring.config.import", "");
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseService courseService;

    @AfterAll
    static void clearConfigOverrides() {
        System.clearProperty("spring.cloud.aws.parameterstore.enabled");
        System.clearProperty("spring.config.import");
    }

    @AfterEach
    void clearSecurityContext() {
        TestSecurityContextHolder.clearContext();
    }

    @DisplayName("필수 쿼리 파라미터가 없으면 잘못된 입력 응답을 반환한다")
    @ParameterizedTest(name = "누락된 파라미터: {0}")
    @ValueSource(strings = {"latitude", "longitude", "targetLatitude", "targetLongitude", "radiusM", "preferredDistanceMs"})
    void returnsInvalidInputValueWhenRequiredQueryParameterIsMissing(String missingParameter) throws Exception {
        mockMvc.perform(getNearbyCoursesWithout(missingParameter))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INPUT_VALUE.getCode()))
                .andExpect(jsonPath("$.data[0].field").value(missingParameter));

        then(courseService).should(never()).findNearbyCourses(any(), any());
    }

    @DisplayName("위도나 경도가 범위를 벗어나면 잘못된 입력 응답을 반환한다")
    @ParameterizedTest(name = "잘못된 {0}: {1}")
    @CsvSource({
            "latitude,91.0",
            "latitude,-91.0",
            "longitude,181.0",
            "longitude,-181.0",
            "targetLatitude,91.0",
            "targetLatitude,-91.0",
            "targetLongitude,181.0",
            "targetLongitude,-181.0"
    })
    void returnsInvalidInputValueWhenLatitudeOrLongitudeIsOutOfRange(String parameterName, String invalidValue) throws Exception {
        mockMvc.perform(getNearbyCoursesWith(parameterName, invalidValue))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INPUT_VALUE.getCode()))
                .andExpect(jsonPath("$.data[0].field").value(parameterName));

        then(courseService).should(never()).findNearbyCourses(any(), any());
    }

    @DisplayName("반경이 허용 범위를 벗어나면 잘못된 반경 응답을 반환한다")
    @ParameterizedTest(name = "radiusM={0}")
    @ValueSource(strings = {"999", "6000"})
    void returnsInvalidRadiusWhenRadiusIsOutOfRange(String invalidRadius) throws Exception {
        mockMvc.perform(nearbyCoursesRequest(
                        "37.5665",
                        "126.9780",
                        "37.5700",
                        "126.9820",
                        invalidRadius,
                        new String[][]{{"1", "3000"}, {"3001", "5000"}},
                        null,
                        null
                ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_RADIUS.getCode()));

        then(courseService).should(never()).findNearbyCourses(any(), any());
    }

    @DisplayName("반경이 정수가 아니면 잘못된 반경 응답을 반환한다")
    @ParameterizedTest(name = "radiusM={0}")
    @ValueSource(strings = {"abc", "1.5"})
    void returnsInvalidRadiusWhenRadiusIsNotAnInteger(String invalidRadius) throws Exception {
        mockMvc.perform(nearbyCoursesRequest(
                        "37.5665",
                        "126.9780",
                        "37.5700",
                        "126.9820",
                        invalidRadius,
                        new String[][]{{"1", "3000"}, {"3001", "5000"}},
                        null,
                        null
                ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_RADIUS.getCode()));

        then(courseService).should(never()).findNearbyCourses(any(), any());
    }

    @DisplayName("목표 거리가 유효하지 않으면 잘못된 목표 거리 응답을 반환한다")
    @ParameterizedTest(name = "targetLatitude={0}")
    @ValueSource(strings = {"91.0", "-91.0"})
    void returnsInvalidTargetDistanceWhenTargetDistanceIsInvalid(String invalidTargetDistance) throws Exception {
        mockMvc.perform(nearbyCoursesRequest(
                        "37.5665",
                        "126.9780",
                        invalidTargetDistance,
                        "126.9820",
                        "1000",
                        new String[][]{{"1", "3000"}, {"3001", "5000"}},
                        null,
                        null
                ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INPUT_VALUE.getCode()));

        then(courseService).should(never()).findNearbyCourses(any(), any());
    }

    @DisplayName("목표 거리가 정수가 아니면 잘못된 목표 거리 응답을 반환한다")
    @ParameterizedTest(name = "targetLongitude={0}")
    @ValueSource(strings = {"abc", "181.0"})
    void returnsInvalidTargetDistanceWhenTargetDistanceIsNotAnInteger(String invalidTargetDistance) throws Exception {
        mockMvc.perform(nearbyCoursesRequest(
                        "37.5665",
                        "126.9780",
                        "37.5700",
                        invalidTargetDistance,
                        "1000",
                        new String[][]{{"1", "3000"}, {"3001", "5000"}},
                        null,
                        null
                ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INPUT_VALUE.getCode()));

        then(courseService).should(never()).findNearbyCourses(any(), any());
    }

    @DisplayName("페이지 크기가 유효하지 않으면 잘못된 입력 응답을 반환한다")
    @ParameterizedTest(name = "size={0}")
    @ValueSource(strings = {"0", "-1", "11", "abc", "1.5"})
    void returnsInvalidInputValueWhenPageSizeIsInvalid(String invalidPageSize) throws Exception {
        mockMvc.perform(nearbyCoursesRequest(
                        "37.5665",
                        "126.9780",
                        "37.5700",
                        "126.9820",
                        "1000",
                        new String[][]{{"1", "3000"}, {"3001", "5000"}},
                        null,
                        invalidPageSize
                ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INPUT_VALUE.getCode()));

        then(courseService).should(never()).findNearbyCourses(any(), any());
    }

    @DisplayName("서비스가 커서를 거부하면 잘못된 커서 응답을 반환한다")
    @ParameterizedTest(name = "cursor={0}")
    @ValueSource(strings = {"%%%", "invalid-cursor"})
    void returnsInvalidCursorWhenServiceRejectsCursor(String invalidCursor) throws Exception {
        mockMvc.perform(nearbyCoursesRequest(
                        "37.5665",
                        "126.9780",
                        "37.5700",
                        "126.9820",
                        "1000",
                        new String[][]{{"1", "3000"}, {"3001", "5000"}},
                        invalidCursor,
                        null
                ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INPUT_VALUE.getCode()));

        then(courseService).should(never()).findNearbyCourses(any(), any());
    }

    @DisplayName("주변 코스 응답을 성공 응답 포맷으로 감싼다")
    @ParameterizedTest(name = "radiusM={0}")
    @ValueSource(strings = {"1000", "1500", "5000"})
    void wrapsNearbyCoursesResponseInSuccessEnvelope(String radiusM) throws Exception {
        NearbyCoursesRequest request = new NearbyCoursesRequest(
                37.5665,
                126.9780,
                37.5700,
                126.9820,
                Integer.parseInt(radiusM),
                List.of(
                        new PreferredDistanceRange(1, 3000),
                        new PreferredDistanceRange(3001, 5000)
                ),
                null,
                null,
                null,
                3
        );
        NearbyCoursesResponse response = new NearbyCoursesResponse(
                List.of(new NearbyCourseItemResponse(
                        1L,
                        "Han River",
                        CourseStatus.OFFICIAL,
                        RouteType.LOOP,
                        10000,
                        35,
                        37.57,
                        126.97,
                        37.58,
                        126.98,
                        320,
                        240,
                        new NearbyCourseItemResponse.DifficultyOption("MEDIUM", "보통"),
                        List.of(
                                new NearbyCourseItemResponse.CourseTypeOption("RIVERSIDE", "강변"),
                                new NearbyCourseItemResponse.CourseTypeOption("PARK", "공원")
                        ),
                        "https://example.com/course.jpg"
                )),
                0,
                3,
                4L,
                2,
                true
        );
        given(courseService.findNearbyCourses(request, null)).willReturn(response);

        mockMvc.perform(nearbyCoursesRequest(
                        "37.5665",
                        "126.9780",
                        "37.5700",
                        "126.9820",
                        radiusM,
                        new String[][]{{"1", "3000"}, {"3001", "5000"}},
                        null,
                        "3"
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.OK.getMessage()))
                .andExpect(jsonPath("$.data.items[0].courseId").value(1L))
                .andExpect(jsonPath("$.data.items[0].title").value("Han River"))
                .andExpect(jsonPath("$.data.items[0].status").value(CourseStatus.OFFICIAL.name()))
                .andExpect(jsonPath("$.data.items[0].routeType").value(RouteType.LOOP.name()))
                .andExpect(jsonPath("$.data.items[0].difficulty.data").value("MEDIUM"))
                .andExpect(jsonPath("$.data.items[0].difficulty.label").value("보통"))
                .andExpect(jsonPath("$.data.items[0].courseTypes[0].data").value("RIVERSIDE"))
                .andExpect(jsonPath("$.data.items[0].courseTypes[0].label").value("강변"))
                .andExpect(jsonPath("$.data.items[0].isLiked").value(false))
                .andExpect(jsonPath("$.data.items[0].isBookmarked").value(false))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(3))
                .andExpect(jsonPath("$.data.totalElements").value(4))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(true));

        then(courseService).should().findNearbyCourses(request, null);
    }

    @DisplayName("추천 코스 조회 응답을 성공 응답 포맷으로 감싼다")
    @Test
    void wrapsRecommendedNearbyCoursesResponseInSuccessEnvelope() throws Exception {
        NearbyCoursesRequest request = new NearbyCoursesRequest(
                37.5665,
                126.9780,
                37.5700,
                126.9820,
                3000,
                List.of(
                        new PreferredDistanceRange(1, 3000),
                        new PreferredDistanceRange(3001, 5000)
                ),
                null,
                NearbyCourseSortBy.DISTANCE,
                1,
                2
        );
        NearbyCoursesResponse response = new NearbyCoursesResponse(
                List.of(new NearbyCourseItemResponse(
                        99L,
                        "Recommended Course",
                        CourseStatus.OFFICIAL,
                        RouteType.OUT_AND_BACK,
                        8000,
                        22,
                        37.57,
                        126.97,
                        37.58,
                        126.98,
                        180,
                        120,
                        new NearbyCourseItemResponse.DifficultyOption("EASY", "쉬움"),
                        List.of(new NearbyCourseItemResponse.CourseTypeOption("PARK", "공원")),
                        true,
                        12L,
                        3L,
                        false,
                        false,
                        "https://example.com/recommended.jpg"
                )),
                0,
                1,
                1L,
                1,
                false
        );
        given(courseService.findRecommendedNearbyCourses(request, null)).willReturn(response);

        mockMvc.perform(get("/api/courses/nearby/recommended")
                        .param("latitude", "37.5665")
                        .param("longitude", "126.9780")
                        .param("targetLatitude", "37.5700")
                        .param("targetLongitude", "126.9820")
                        .param("radiusM", "3000")
                        .param("preferredDistanceMs[0].min", "1")
                        .param("preferredDistanceMs[0].max", "3000")
                        .param("preferredDistanceMs[1].min", "3001")
                        .param("preferredDistanceMs[1].max", "5000")
                        .param("sortBy", "DISTANCE")
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].courseId").value(99L))
                .andExpect(jsonPath("$.data.items[0].routeType").value(RouteType.OUT_AND_BACK.name()))
                .andExpect(jsonPath("$.data.items[0].isRecommended").value(true))
                .andExpect(jsonPath("$.data.items[0].isLiked").value(false))
                .andExpect(jsonPath("$.data.items[0].isBookmarked").value(false))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.hasNext").value(false));

        then(courseService).should().findRecommendedNearbyCourses(request, null);
    }

    @DisplayName("코스 상세 조회 응답을 성공 응답 포맷으로 감싼다")
    @ParameterizedTest(name = "courseId={0}")
    @ValueSource(longs = {1L, 25L})
    void wrapsCourseDetailResponseInSuccessEnvelope(long courseId) throws Exception {
        long userId = 7L;
        CourseDetailResponse response = new CourseDetailResponse(
                courseId,
                "Han River",
                CourseStatus.OFFICIAL,
                RouteType.LOOP,
                new CourseDetailResponse.DifficultyOption("MEDIUM", "보통"),
                10000,
                35,
                "https://example.com/course.jpg",
                37.57,
                126.97,
                37.58,
                126.98,
                new Coordinates(List.of(
                        new GeoPoint(37.57, 126.97, 10.0),
                        new GeoPoint(37.58, 126.98, 12.0)
                )),
                List.of(
                        new CourseDetailResponse.CourseTypeOption("RIVERSIDE", "강변"),
                        new CourseDetailResponse.CourseTypeOption("PARK", "공원")
                ),
                14L,
                false,
                false,
                4.5
        );
        given(courseService.findCourseDetail(courseId, userId)).willReturn(response);

        mockMvc.perform(get("/api/courses/{courseId}", courseId)
                        .with(authenticated(userId)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()))
                .andExpect(jsonPath("$.data.courseId").value(courseId))
                .andExpect(jsonPath("$.data.title").value("Han River"))
                .andExpect(jsonPath("$.data.routeType").value(RouteType.LOOP.name()))
                .andExpect(jsonPath("$.data.difficulty.data").value("MEDIUM"))
                .andExpect(jsonPath("$.data.difficulty.label").value("보통"))
                .andExpect(jsonPath("$.data.coordinates[0].latitude").value(37.57))
                .andExpect(jsonPath("$.data.coordinates[1].longitude").value(126.98))
                .andExpect(jsonPath("$.data.coordinates[0].elevationM").value(10.0))
                .andExpect(jsonPath("$.data.courseTypes[0].data").value("RIVERSIDE"))
                .andExpect(jsonPath("$.data.courseTypes[0].label").value("강변"))
                .andExpect(jsonPath("$.data.likeCount").value(14))
                .andExpect(jsonPath("$.data.isLiked").value(false))
                .andExpect(jsonPath("$.data.isBookmarked").value(false))
                .andExpect(jsonPath("$.data.averageRating").value(4.5));

        then(courseService).should().findCourseDetail(courseId, userId);
    }

    @DisplayName("없는 코스 상세 조회는 코스 없음 응답을 반환한다")
    @ParameterizedTest(name = "courseId={0}")
    @ValueSource(longs = {999L})
    void returnsCourseNotFoundWhenCourseDetailIsMissing(long courseId) throws Exception {
        long userId = 7L;
        given(courseService.findCourseDetail(courseId, userId))
                .willThrow(new CustomException(ResponseCode.COURSE_NOT_FOUND));

        mockMvc.perform(get("/api/courses/{courseId}", courseId)
                        .with(authenticated(userId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.COURSE_NOT_FOUND.getCode()));

        then(courseService).should().findCourseDetail(courseId, userId);
    }

    @DisplayName("코스 상세 조회는 principal user id 를 service 로 전달한다")
    @Test
    void forwardsPrincipalToCourseDetailService() throws Exception {
        long courseId = 77L;
        long userId = 88L;
        CourseDetailResponse response = new CourseDetailResponse(
                courseId,
                "Private Course",
                CourseStatus.PRIVATE,
                RouteType.LOOP,
                null,
                5000,
                100,
                null,
                37.57,
                126.97,
                37.58,
                126.98,
                new Coordinates(List.of()),
                List.of(),
                0L,
                false,
                false,
                null
        );
        given(courseService.findCourseDetail(courseId, userId)).willReturn(response);

        mockMvc.perform(get("/api/courses/{courseId}", courseId)
                        .with(authenticated(userId)))
                .andExpect(status().isOk());

        then(courseService).should().findCourseDetail(courseId, userId);
    }

    @DisplayName("코스 고스트 상세 조회 응답을 성공 응답 포맷으로 감싼다")
    @Test
    void wrapsCourseGhostDetailResponseInSuccessEnvelope() throws Exception {
        long courseId = 12L;
        long userId = 7L;
        CourseGhostDetailResponse response = new CourseGhostDetailResponse(
                courseId,
                "Han River Ghost",
                CourseStatus.OFFICIAL,
                RouteType.OUT_AND_BACK,
                new CourseGhostDetailResponse.DifficultyOption("MEDIUM", "보통"),
                5200,
                48,
                "https://example.com/ghost-course.jpg",
                37.5661,
                126.9738,
                37.5702,
                126.9814,
                new Coordinates(List.of(
                        new GeoPoint(37.5661, 126.9738, 12.3),
                        new GeoPoint(37.5702, 126.9814, 15.8)
                )),
                List.of(new CourseGhostDetailResponse.CourseTypeOption("RIVERSIDE", "강변")),
                128L,
                false,
                false,
                4.3,
                new CourseGhostDetailResponse.MyRecord(
                        205L,
                        2002L,
                        1320,
                        980,
                        14L,
                        LocalDateTime.of(2026, 3, 20, 7, 10)
                )
        );
        given(courseService.findCourseGhostDetail(courseId, userId)).willReturn(response);

        mockMvc.perform(get("/api/courses/{courseId}/ghost-detail", courseId)
                        .with(authenticated(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()))
                .andExpect(jsonPath("$.data.courseId").value(courseId))
                .andExpect(jsonPath("$.data.title").value("Han River Ghost"))
                .andExpect(jsonPath("$.data.routeType").value(RouteType.OUT_AND_BACK.name()))
                .andExpect(jsonPath("$.data.difficulty.data").value("MEDIUM"))
                .andExpect(jsonPath("$.data.courseTypes[0].data").value("RIVERSIDE"))
                .andExpect(jsonPath("$.data.myRecord.leaderboardId").value(205L))
                .andExpect(jsonPath("$.data.myRecord.runningSessionId").value(2002L))
                .andExpect(jsonPath("$.data.myRecord.point").value(980))
                .andExpect(jsonPath("$.data.myRecord.rank").value(14));

        then(courseService).should().findCourseGhostDetail(courseId, userId);
    }

    @DisplayName("코스 고스트 상세 조회는 principal user id 를 service 로 전달한다")
    @Test
    void forwardsPrincipalToCourseGhostDetailService() throws Exception {
        long courseId = 13L;
        long userId = 9L;
        CourseGhostDetailResponse response = new CourseGhostDetailResponse(
                courseId,
                "Private Ghost",
                CourseStatus.PRIVATE,
                RouteType.LOOP,
                null,
                5000,
                50,
                null,
                37.57,
                126.97,
                37.58,
                126.98,
                new Coordinates(List.of()),
                List.of(),
                0L,
                false,
                false,
                null,
                null
        );
        given(courseService.findCourseGhostDetail(courseId, userId)).willReturn(response);

        mockMvc.perform(get("/api/courses/{courseId}/ghost-detail", courseId)
                        .with(authenticated(userId)))
                .andExpect(status().isOk());

        then(courseService).should().findCourseGhostDetail(courseId, userId);
    }

    @DisplayName("없는 코스 고스트 상세 조회는 코스 없음 응답을 반환한다")
    @Test
    void returnsCourseNotFoundWhenCourseGhostDetailIsMissing() throws Exception {
        long courseId = 999L;
        long userId = 7L;
        given(courseService.findCourseGhostDetail(courseId, userId))
                .willThrow(new CustomException(ResponseCode.COURSE_NOT_FOUND));

        mockMvc.perform(get("/api/courses/{courseId}/ghost-detail", courseId)
                        .with(authenticated(userId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.COURSE_NOT_FOUND.getCode()));

        then(courseService).should().findCourseGhostDetail(courseId, userId);
    }

    @DisplayName("페이지 크기 쿼리 파라미터를 요청 DTO에 바인딩한다")
    @ParameterizedTest(name = "pageSize={0}")
    @ValueSource(strings = {"2", "10"})
    void bindsPageSizeQueryParameterToRequestDto(String pageSize) throws Exception {
        NearbyCoursesRequest request = new NearbyCoursesRequest(
                37.5665,
                126.9780,
                37.5700,
                126.9820,
                1000,
                List.of(
                        new PreferredDistanceRange(1, 3000),
                        new PreferredDistanceRange(3001, 5000)
                ),
                null,
                null,
                null,
                Integer.parseInt(pageSize)
        );
        NearbyCoursesResponse response = new NearbyCoursesResponse(List.of(), 0, Integer.parseInt(pageSize), 0L, 0, false);
        given(courseService.findNearbyCourses(request, null)).willReturn(response);

        mockMvc.perform(nearbyCoursesRequest(
                        "37.5665",
                        "126.9780",
                        "37.5700",
                        "126.9820",
                        "1000",
                        new String[][]{{"1", "3000"}, {"3001", "5000"}},
                        null,
                        pageSize
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        then(courseService).should().findNearbyCourses(request, null);
    }

    @Test
    void bindsSortByQueryParameterToRequestDto() throws Exception {
        NearbyCoursesRequest request = new NearbyCoursesRequest(
                37.5665,
                126.9780,
                37.5700,
                126.9820,
                1000,
                List.of(
                        new PreferredDistanceRange(1, 3000),
                        new PreferredDistanceRange(3001, 5000)
                ),
                CourseStatus.COMMUNITY,
                NearbyCourseSortBy.POPULAR,
                0,
                3
        );
        NearbyCoursesResponse response = new NearbyCoursesResponse(List.of(), 0, 3, 0L, 0, false);
        given(courseService.findNearbyCourses(request, null)).willReturn(response);

        mockMvc.perform(nearbyCoursesRequest(
                        "37.5665",
                        "126.9780",
                        "37.5700",
                        "126.9820",
                        "1000",
                        new String[][]{{"1", "3000"}, {"3001", "5000"}},
                        "COMMUNITY",
                        "POPULAR",
                        "0",
                        "3"
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        then(courseService).should().findNearbyCourses(request, null);
    }


    @DisplayName("코스 고스트 리더보드 조회 응답을 성공 응답 포맷으로 감싼다")
    @Test
    void wrapsCourseGhostLeaderboardResponseInSuccessEnvelope() throws Exception {
        long userId = 7L;
        CourseGhostLeaderboardRequest request = new CourseGhostLeaderboardRequest(20, "b3BhcXVlLWN1cnNvcg");
        CourseGhostLeaderboardResponse response = new CourseGhostLeaderboardResponse(
                List.of(new CourseGhostLeaderboardItemResponse(
                        101L,
                        7L,
                        "runner-a",
                        1001L,
                        1250,
                        1L,
                        LocalDateTime.of(2026, 3, 21, 10, 30)
                )),
                RouteType.LOOP,
                true,
                "opaque-cursor"
        );
        given(courseService.findCourseGhostLeaderboard(12L, userId, request)).willReturn(response);

        mockMvc.perform(get("/api/courses/{courseId}/ghost-leaderboard", 12L)
                        .with(authenticated(userId))
                        .queryParam("size", "20")
                        .queryParam("cursor", "b3BhcXVlLWN1cnNvcg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()))
                .andExpect(jsonPath("$.data.items[0].leaderboardId").value(101L))
                .andExpect(jsonPath("$.data.items[0].userId").value(7L))
                .andExpect(jsonPath("$.data.items[0].nickname").value("runner-a"))
                .andExpect(jsonPath("$.data.items[0].point").value(1250))
                .andExpect(jsonPath("$.data.items[0].rank").value(1))
                .andExpect(jsonPath("$.data.routeType").value(RouteType.LOOP.name()))
                .andExpect(jsonPath("$.data.hasMore").value(true))
                .andExpect(jsonPath("$.data.nextCursor").value("opaque-cursor"));

        then(courseService).should().findCourseGhostLeaderboard(12L, userId, request);
    }

    @DisplayName("코스 고스트 리더보드 size가 유효하지 않으면 잘못된 입력 응답을 반환한다")
    @ParameterizedTest(name = "size={0}")
    @ValueSource(strings = {"0", "-1", "51", "abc", "1.5"})
    void returnsInvalidInputWhenCourseGhostLeaderboardSizeIsInvalid(String invalidSize) throws Exception {
        mockMvc.perform(get("/api/courses/{courseId}/ghost-leaderboard", 12L)
                        .queryParam("size", invalidSize))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INPUT_VALUE.getCode()));

        then(courseService).should(never()).findCourseGhostLeaderboard(any(), any(), any());
    }

    @DisplayName("코스 고스트 리더보드 조회에서 서비스가 커서를 거부하면 잘못된 커서 응답을 반환한다")
    @Test
    void returnsInvalidCursorWhenCourseGhostLeaderboardServiceRejectsCursor() throws Exception {
        long userId = 7L;
        CourseGhostLeaderboardRequest request = new CourseGhostLeaderboardRequest(20, "%%%");
        given(courseService.findCourseGhostLeaderboard(12L, userId, request))
                .willThrow(new CustomException(ResponseCode.INVALID_CURSOR));

        mockMvc.perform(get("/api/courses/{courseId}/ghost-leaderboard", 12L)
                        .with(authenticated(userId))
                        .queryParam("cursor", "%%%"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_CURSOR.getCode()));

        then(courseService).should().findCourseGhostLeaderboard(12L, userId, request);
    }

    @DisplayName("없는 코스 고스트 리더보드 조회는 코스 없음 응답을 반환한다")
    @Test
    void returnsCourseNotFoundWhenCourseGhostLeaderboardCourseIsMissing() throws Exception {
        long userId = 7L;
        CourseGhostLeaderboardRequest request = new CourseGhostLeaderboardRequest(20, null);
        given(courseService.findCourseGhostLeaderboard(999L, userId, request))
                .willThrow(new CustomException(ResponseCode.COURSE_NOT_FOUND));

        mockMvc.perform(get("/api/courses/{courseId}/ghost-leaderboard", 999L)
                        .with(authenticated(userId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.COURSE_NOT_FOUND.getCode()));

        then(courseService).should().findCourseGhostLeaderboard(999L, userId, request);
    }

    @DisplayName("코스 고스트 리더보드 조회는 principal user id 를 service 로 전달한다")
    @Test
    void forwardsPrincipalToCourseGhostLeaderboardService() throws Exception {
        long courseId = 55L;
        long userId = 66L;
        CourseGhostLeaderboardRequest request = new CourseGhostLeaderboardRequest(20, null);
        CourseGhostLeaderboardResponse response = new CourseGhostLeaderboardResponse(List.of(), RouteType.LOOP, false, null);
        given(courseService.findCourseGhostLeaderboard(courseId, userId, request)).willReturn(response);

        mockMvc.perform(get("/api/courses/{courseId}/ghost-leaderboard", courseId)
                        .with(authenticated(userId)))
                .andExpect(status().isOk());

        then(courseService).should().findCourseGhostLeaderboard(courseId, userId, request);
    }

    @DisplayName("주변 고스트 코스 조회에서 필수 쿼리 파라미터가 없으면 잘못된 입력 응답을 반환한다")
    @ParameterizedTest(name = "누락 파라미터: {0}")
    @ValueSource(strings = {"latitude", "longitude", "targetLatitude", "targetLongitude", "radiusM", "preferredDistanceMs"})
    void returnsInvalidInputValueWhenRequiredGhostQueryParameterIsMissing(String missingParameter) throws Exception {
        mockMvc.perform(getNearbyGhostCoursesWithout(missingParameter))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INPUT_VALUE.getCode()))
                .andExpect(jsonPath("$.data[0].field").value(missingParameter));

        then(courseService).should(never()).findNearbyGhostCourses(any(), any());
    }

    @DisplayName("주변 고스트 코스 조회에서 위도 또는 경도가 범위를 벗어나면 잘못된 입력 응답을 반환한다")
    @ParameterizedTest(name = "잘못된 {0}: {1}")
    @CsvSource({
            "latitude,91.0",
            "latitude,-91.0",
            "longitude,181.0",
            "longitude,-181.0",
            "targetLatitude,91.0",
            "targetLatitude,-91.0",
            "targetLongitude,181.0",
            "targetLongitude,-181.0"
    })
    void returnsInvalidInputValueWhenGhostLatitudeOrLongitudeIsOutOfRange(String parameterName, String invalidValue) throws Exception {
        mockMvc.perform(getNearbyGhostCoursesWith(parameterName, invalidValue))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INPUT_VALUE.getCode()))
                .andExpect(jsonPath("$.data[0].field").value(parameterName));

        then(courseService).should(never()).findNearbyGhostCourses(any(), any());
    }

    @DisplayName("주변 고스트 코스 조회에서 pageSize가 유효하지 않으면 잘못된 입력 응답을 반환한다")
    @ParameterizedTest(name = "pageSize={0}")
    @ValueSource(strings = {"0", "-1", "11", "abc", "1.5"})
    void returnsInvalidInputValueWhenGhostSizeIsInvalid(String invalidSize) throws Exception {
        mockMvc.perform(nearbyGhostCoursesRequest(
                        "37.5665",
                        "126.9780",
                        "37.5700",
                        "126.9820",
                        "1000",
                        new String[][]{{"1", "3000"}, {"3001", "5000"}},
                        null,
                        null,
                        invalidSize
                ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INPUT_VALUE.getCode()));

        then(courseService).should(never()).findNearbyGhostCourses(any(), any());
    }

    @DisplayName("주변 고스트 코스 응답을 성공 응답 형식으로 감싼다")
    @Test
    void wrapsNearbyGhostCoursesResponseInSuccessEnvelope() throws Exception {
        NearbyGhostCoursesRequest request = new NearbyGhostCoursesRequest(
                37.5665,
                126.9780,
                37.5700,
                126.9820,
                1000,
                List.of(
                        new PreferredDistanceRange(1, 3000),
                        new PreferredDistanceRange(3001, 5000)
                ),
                NearbyGhostCourseSortBy.GHOST_RUN_COUNT,
                0,
                3
        );
        NearbyGhostCoursesResponse response = new NearbyGhostCoursesResponse(
                List.of(new NearbyCourseItemResponse(
                        1L,
                        "Ghost Han River",
                        CourseStatus.OFFICIAL,
                        RouteType.LOOP,
                        5000,
                        35,
                        37.57,
                        126.97,
                        37.58,
                        126.98,
                        240,
                        240,
                        new NearbyCourseItemResponse.DifficultyOption("MEDIUM", "보통"),
                        List.of(
                                new NearbyCourseItemResponse.CourseTypeOption("RIVERSIDE", "강변"),
                                new NearbyCourseItemResponse.CourseTypeOption("PARK", "공원")
                        ),
                        false,
                        14L,
                        8L,
                        false,
                        false,
                        "https://example.com/course.jpg"
                )),
                0,
                3,
                4L,
                2,
                true
        );
        given(courseService.findNearbyGhostCourses(request, null)).willReturn(response);

        mockMvc.perform(nearbyGhostCoursesRequest(
                        "37.5665",
                        "126.9780",
                        "37.5700",
                        "126.9820",
                        "1000",
                        new String[][]{{"1", "3000"}, {"3001", "5000"}},
                        "GHOST_RUN_COUNT",
                        "0",
                        "3"
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.OK.getMessage()))
                .andExpect(jsonPath("$.data.items[0].courseId").value(1L))
                .andExpect(jsonPath("$.data.items[0].title").value("Ghost Han River"))
                .andExpect(jsonPath("$.data.items[0].status").value(CourseStatus.OFFICIAL.name()))
                .andExpect(jsonPath("$.data.items[0].routeType").value(RouteType.LOOP.name()))
                .andExpect(jsonPath("$.data.items[0].difficulty.data").value("MEDIUM"))
                .andExpect(jsonPath("$.data.items[0].difficulty.label").value("보통"))
                .andExpect(jsonPath("$.data.items[0].courseTypes[0].data").value("RIVERSIDE"))
                .andExpect(jsonPath("$.data.items[0].courseTypes[0].label").value("강변"))
                .andExpect(jsonPath("$.data.items[0].likeCount").value(14L))
                .andExpect(jsonPath("$.data.items[0].bookmarkCount").value(8L))
                .andExpect(jsonPath("$.data.items[0].isLiked").value(false))
                .andExpect(jsonPath("$.data.items[0].isBookmarked").value(false))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(3))
                .andExpect(jsonPath("$.data.totalElements").value(4))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(true));

        then(courseService).should().findNearbyGhostCourses(request, null);
    }

    @DisplayName("주변 고스트 코스 조회에서 선호 거리 목록과 pageSize를 요청 DTO에 바인딩한다")
    @Test
    void bindsNearbyGhostCourseQueryParametersToRequestDto() throws Exception {
        NearbyGhostCoursesRequest request = new NearbyGhostCoursesRequest(
                37.5665,
                126.9780,
                37.5700,
                126.9820,
                3000,
                List.of(
                        new PreferredDistanceRange(1, 3000),
                        new PreferredDistanceRange(3001, 5000),
                        new PreferredDistanceRange(5001, 10000)
                ),
                NearbyGhostCourseSortBy.DISTANCE,
                1,
                2
        );
        NearbyGhostCoursesResponse response = new NearbyGhostCoursesResponse(List.of(), 1, 2, 0L, 0, false);
        given(courseService.findNearbyGhostCourses(request, null)).willReturn(response);

        mockMvc.perform(nearbyGhostCoursesRequest(
                        "37.5665",
                        "126.9780",
                        "37.5700",
                        "126.9820",
                        "3000",
                        new String[][]{{"1", "3000"}, {"3001", "5000"}, {"5001", "10000"}},
                        "DISTANCE",
                        "1",
                        "2"
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        then(courseService).should().findNearbyGhostCourses(request, null);
    }

    @DisplayName("주변 고스트 코스 조회는 principal user id 를 service 로 전달한다")
    @Test
    void forwardsPrincipalToNearbyGhostCoursesService() throws Exception {
        long userId = 77L;
        AuthenticatedUser user = new AuthenticatedUser(userId, "google", true);
        NearbyGhostCoursesRequest request = new NearbyGhostCoursesRequest(
                37.5665,
                126.9780,
                37.5700,
                126.9820,
                3000,
                List.of(
                        new PreferredDistanceRange(1, 3000),
                        new PreferredDistanceRange(3001, 5000)
                ),
                NearbyGhostCourseSortBy.DISTANCE,
                1,
                2
        );
        NearbyGhostCoursesResponse response = new NearbyGhostCoursesResponse(List.of(), 1, 2, 0L, 0, false);
        given(courseService.findNearbyGhostCourses(request, user)).willReturn(response);

        mockMvc.perform(nearbyGhostCoursesRequest(
                        "37.5665",
                        "126.9780",
                        "37.5700",
                        "126.9820",
                        "3000",
                        new String[][]{{"1", "3000"}, {"3001", "5000"}},
                        "DISTANCE",
                        "1",
                        "2"
                ).with(authenticated(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        then(courseService).should().findNearbyGhostCourses(request, user);
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder getNearbyCoursesWithout(
            String missingParameter
    ) {
        return nearbyCoursesRequest(
                "latitude".equals(missingParameter) ? null : "37.5665",
                "longitude".equals(missingParameter) ? null : "126.9780",
                "targetLatitude".equals(missingParameter) ? null : "37.5700",
                "targetLongitude".equals(missingParameter) ? null : "126.9820",
                "radiusM".equals(missingParameter) ? null : "1000",
                "preferredDistanceMs".equals(missingParameter) ? null : new String[][]{{"1", "3000"}, {"3001", "5000"}},
                null,
                null
        );
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder getNearbyCoursesWith(
            String parameterName,
            String value
    ) {
        return nearbyCoursesRequest(
                "latitude".equals(parameterName) ? value : "37.5665",
                "longitude".equals(parameterName) ? value : "126.9780",
                "targetLatitude".equals(parameterName) ? value : "37.5700",
                "targetLongitude".equals(parameterName) ? value : "126.9820",
                "1000",
                new String[][]{{"1", "3000"}, {"3001", "5000"}},
                null,
                null
        );
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder nearbyCoursesRequest(
            String latitude,
            String longitude,
            String targetLatitude,
            String targetLongitude,
            String radiusM,
            String[][] preferredDistanceMs,
            String page,
            String size
    ) {
        return nearbyCoursesRequest(latitude, longitude, targetLatitude, targetLongitude, radiusM, preferredDistanceMs, null, null, page, size);
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder nearbyCoursesRequest(
            String latitude,
            String longitude,
            String targetLatitude,
            String targetLongitude,
            String radiusM,
            String[][] preferredDistanceMs,
            String status,
            String sortBy,
            String page,
            String size
    ) {
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder requestBuilder = get("/api/courses/nearby");
        if (latitude != null) {
            requestBuilder.param("latitude", latitude);
        }
        if (longitude != null) {
            requestBuilder.param("longitude", longitude);
        }
        if (targetLatitude != null) {
            requestBuilder.param("targetLatitude", targetLatitude);
        }
        if (targetLongitude != null) {
            requestBuilder.param("targetLongitude", targetLongitude);
        }
        if (radiusM != null) {
            requestBuilder.param("radiusM", radiusM);
        }
        if (preferredDistanceMs != null) {
            for (int index = 0; index < preferredDistanceMs.length; index++) {
                requestBuilder.param("preferredDistanceMs[" + index + "].min", preferredDistanceMs[index][0]);
                requestBuilder.param("preferredDistanceMs[" + index + "].max", preferredDistanceMs[index][1]);
            }
        }
        if (status != null) {
            requestBuilder.param("status", status);
        }
        if (sortBy != null) {
            requestBuilder.param("sortBy", sortBy);
        }
        if (page != null) {
            requestBuilder.param("page", page);
        }
        if (size != null) {
            requestBuilder.param("size", size);
        }
        return requestBuilder;
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder getNearbyGhostCoursesWithout(
            String missingParameter
    ) {
        return nearbyGhostCoursesRequest(
                "latitude".equals(missingParameter) ? null : "37.5665",
                "longitude".equals(missingParameter) ? null : "126.9780",
                "targetLatitude".equals(missingParameter) ? null : "37.5700",
                "targetLongitude".equals(missingParameter) ? null : "126.9820",
                "radiusM".equals(missingParameter) ? null : "1000",
                "preferredDistanceMs".equals(missingParameter) ? null : new String[][]{{"1", "3000"}, {"3001", "5000"}},
                null,
                null,
                null
        );
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder getNearbyGhostCoursesWith(
            String parameterName,
            String value
    ) {
        return nearbyGhostCoursesRequest(
                "latitude".equals(parameterName) ? value : "37.5665",
                "longitude".equals(parameterName) ? value : "126.9780",
                "targetLatitude".equals(parameterName) ? value : "37.5700",
                "targetLongitude".equals(parameterName) ? value : "126.9820",
                "radiusM".equals(parameterName) ? value : "1000",
                new String[][]{{"1", "3000"}, {"3001", "5000"}},
                null,
                null,
                null
        );
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder nearbyGhostCoursesRequest(
            String latitude,
            String longitude,
            String targetLatitude,
            String targetLongitude,
            String radiusM,
            String[][] preferredDistanceMs,
            String sortBy,
            String page,
            String size
    ) {
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder requestBuilder = get("/api/courses/nearby/ghost");
        if (latitude != null) {
            requestBuilder.param("latitude", latitude);
        }
        if (longitude != null) {
            requestBuilder.param("longitude", longitude);
        }
        if (targetLatitude != null) {
            requestBuilder.param("targetLatitude", targetLatitude);
        }
        if (targetLongitude != null) {
            requestBuilder.param("targetLongitude", targetLongitude);
        }
        if (radiusM != null) {
            requestBuilder.param("radiusM", radiusM);
        }
        if (preferredDistanceMs != null) {
            for (int index = 0; index < preferredDistanceMs.length; index++) {
                requestBuilder.param("preferredDistanceMs[" + index + "].min", preferredDistanceMs[index][0]);
                requestBuilder.param("preferredDistanceMs[" + index + "].max", preferredDistanceMs[index][1]);
            }
        }
        if (sortBy != null) {
            requestBuilder.param("sortBy", sortBy);
        }
        if (page != null) {
            requestBuilder.param("page", page);
        }
        if (size != null) {
            requestBuilder.param("size", size);
        }
        return requestBuilder;
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
