package kr.withrun.was.domain.running.controller;

import kr.withrun.was.domain.auth.jwt.JwtProvider;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.type.RouteType;
import kr.withrun.was.domain.course.vo.Coordinates;
import kr.withrun.was.domain.course.vo.GeoPoint;
import kr.withrun.was.domain.running.dto.CompleteRunningSessionRequest;
import kr.withrun.was.domain.running.dto.CreateRunningSessionRequest;
import kr.withrun.was.domain.running.dto.CreateRunningSessionResponse;
import kr.withrun.was.domain.running.dto.PastRunningSessionItemResponse;
import kr.withrun.was.domain.running.dto.PastRunningSessionsRequest;
import kr.withrun.was.domain.running.dto.PastRunningSessionsResponse;
import kr.withrun.was.domain.running.dto.RegisterRunningSessionCourseRequest;
import kr.withrun.was.domain.running.dto.RegisterRunningSessionCourseResponse;
import kr.withrun.was.domain.running.dto.RunningGpsSampleResponse;
import kr.withrun.was.domain.running.dto.RunningHealthSampleResponse;
import kr.withrun.was.domain.running.dto.RunningSessionDetailResponse;
import kr.withrun.was.domain.running.service.RunningSessionService;
import kr.withrun.was.domain.running.type.GhostResultStatus;
import kr.withrun.was.domain.running.type.RunningSessionCompleteState;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.type.Gender;
import kr.withrun.was.global.common.type.TimeSlot;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
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

import static kr.withrun.was.domain.running.type.RunningMode.COURSE;
import static kr.withrun.was.domain.running.type.RunningMode.GHOST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
@DisplayName("러닝 세션 컨트롤러")
class RunningSessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private RunningSessionService runningSessionService;

    @DisplayName("러닝 세션 생성 응답을 생성 성공 포맷으로 감싼다")
    @Test
    void wrapsCreateRunningSessionResponseInCreatedEnvelope() throws Exception {
        long currentUserId = 1L;
        CreateRunningSessionRequest request = validCreateRequest();
        CreateRunningSessionResponse response = new CreateRunningSessionResponse(
                901L,
                COURSE,
                300L,
                null,
                null,
                null,
                0,
                0,
                RunningSessionCompleteState.FAIL
        );
        given(runningSessionService.createRunningSession(currentUserId, request)).willReturn(response);

        mockMvc.perform(post("/api/running-sessions")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(currentUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequestBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.CREATED.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.CREATED.getMessage()))
                .andExpect(jsonPath("$.data.runningSessionId").value(901L))
                .andExpect(jsonPath("$.data.mode").value(COURSE.name()))
                .andExpect(jsonPath("$.data.courseId").value(300L))
                .andExpect(jsonPath("$.data.navigationBundleUrl").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.data.ghostTargetRunningSessionId").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.data.ghostTargetGpsSamples").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.data.startedAt").doesNotExist())
                .andExpect(jsonPath("$.data.distanceM").value(0))
                .andExpect(jsonPath("$.data.caloriesKcal").value(0))
                .andExpect(jsonPath("$.data.completeState").value(RunningSessionCompleteState.FAIL.name()));

        then(runningSessionService).should().createRunningSession(currentUserId, request);
    }

    @DisplayName("GHOST 모드 세션 생성 응답은 대상 세션 GPS 샘플을 함께 직렬화한다")
    @Test
    void serializesGhostTargetGpsSamplesInCreateResponse() throws Exception {
        long currentUserId = 1L;
        CreateRunningSessionRequest request = ghostCreateRequest();
        CreateRunningSessionResponse response = new CreateRunningSessionResponse(
                902L,
                GHOST,
                300L,
                "https://cdn.withrun.kr/navigation/latest/300.json?Expires=100&Signature=test-signature&Key-Pair-Id=K123",
                44L,
                java.util.List.of(
                        new RunningGpsSampleResponse(
                                LocalDateTime.parse("2026-03-12T06:30:05"),
                                0,
                                37.5665,
                                126.9780,
                                12.3,
                                3.2,
                                181.0,
                                null,
                                null,
                                null,
                                null
                        )
                ),
                0,
                0,
                RunningSessionCompleteState.FAIL
        );
        given(runningSessionService.createRunningSession(currentUserId, request)).willReturn(response);

        mockMvc.perform(post("/api/running-sessions")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(currentUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ghostCreateRequestBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.runningSessionId").value(902L))
                .andExpect(jsonPath("$.data.mode").value(GHOST.name()))
                .andExpect(jsonPath("$.data.navigationBundleUrl").value("https://cdn.withrun.kr/navigation/latest/300.json?Expires=100&Signature=test-signature&Key-Pair-Id=K123"))
                .andExpect(jsonPath("$.data.ghostTargetRunningSessionId").value(44L))
                .andExpect(jsonPath("$.data.ghostTargetGpsSamples[0].sampledAt").value("2026-03-12T06:30:05"))
                .andExpect(jsonPath("$.data.ghostTargetGpsSamples[0].time").value(0))
                .andExpect(jsonPath("$.data.ghostTargetGpsSamples[0].latitude").value(37.5665))
                .andExpect(jsonPath("$.data.ghostTargetGpsSamples[0].longitude").value(126.9780));

        then(runningSessionService).should().createRunningSession(currentUserId, request);
    }

    @DisplayName("필수 요청 필드가 없으면 잘못된 입력 응답을 반환한다")
    @ParameterizedTest(name = "누락된 필드: {0}")
    @ValueSource(strings = {"mode", "startLatitude", "startLongitude"})
    void returnsInvalidInputValueWhenRequiredRequestFieldIsMissing(String missingField) throws Exception {
        mockMvc.perform(post("/api/running-sessions")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyWithout(missingField)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INPUT_VALUE.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.INVALID_INPUT_VALUE.getMessage()))
                .andExpect(jsonPath("$.data[0].field").value(missingField));

        then(runningSessionService).shouldHaveNoInteractions();
    }

    @DisplayName("위도나 경도가 범위를 벗어나면 잘못된 입력 응답을 반환한다")
    @ParameterizedTest(name = "잘못된 {0}: {1}")
    @CsvSource({
            "startLatitude,91.0",
            "startLatitude,-91.0",
            "startLongitude,181.0",
            "startLongitude,-181.0"
    })
    void returnsInvalidInputValueWhenCoordinatesAreOutOfRange(String fieldName, String invalidValue) throws Exception {
        mockMvc.perform(post("/api/running-sessions")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyWith(fieldName, invalidValue)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INPUT_VALUE.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.INVALID_INPUT_VALUE.getMessage()))
                .andExpect(jsonPath("$.data[0].field").value(fieldName));

        then(runningSessionService).shouldHaveNoInteractions();
    }

    @DisplayName("잘못된 러닝 모드가 들어오면 내부 서버 오류 응답을 반환하고 서비스를 호출하지 않는다")
    @Test
    void returnsInternalServerErrorWhenRunningModeIsInvalid() throws Exception {
        mockMvc.perform(post("/api/running-sessions")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyWith("mode", "\"MARATHON\"")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INTERNAL_SERVER_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.INTERNAL_SERVER_ERROR.getMessage()));

        then(runningSessionService).shouldHaveNoInteractions();
    }

    @DisplayName("서비스가 없는 코스를 감지하면 코스 없음 응답을 반환한다")
    @Test
    void returnsCourseNotFoundWhenServiceRejectsCourse() throws Exception {
        long currentUserId = 1L;
        CreateRunningSessionRequest request = validCreateRequest();
        given(runningSessionService.createRunningSession(currentUserId, request))
                .willThrow(new CustomException(ResponseCode.COURSE_NOT_FOUND));

        mockMvc.perform(post("/api/running-sessions")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(currentUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequestBody()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.COURSE_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.COURSE_NOT_FOUND.getMessage()));

        then(runningSessionService).should().createRunningSession(currentUserId, request);
    }

    @DisplayName("서비스가 없는 고스트 대상을 감지하면 해당 응답을 반환한다")
    @Test
    void returnsGhostTargetNotFoundWhenServiceRejectsGhostTarget() throws Exception {
        long currentUserId = 1L;
        CreateRunningSessionRequest request = ghostCreateRequest();
        given(runningSessionService.createRunningSession(currentUserId, request))
                .willThrow(new CustomException(ResponseCode.GHOST_TARGET_NOT_FOUND));

        mockMvc.perform(post("/api/running-sessions")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(currentUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ghostCreateRequestBody()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.GHOST_TARGET_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.GHOST_TARGET_NOT_FOUND.getMessage()));

        then(runningSessionService).should().createRunningSession(currentUserId, request);
    }

    @DisplayName("서비스가 유효하지 않은 고스트 대상을 거부하면 해당 응답을 반환한다")
    @Test
    void returnsInvalidGhostTargetWhenServiceRejectsGhostTargetEligibility() throws Exception {
        long currentUserId = 1L;
        CreateRunningSessionRequest request = ghostCreateRequest();
        given(runningSessionService.createRunningSession(currentUserId, request))
                .willThrow(new CustomException(ResponseCode.INVALID_GHOST_TARGET));

        mockMvc.perform(post("/api/running-sessions")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(currentUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ghostCreateRequestBody()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_GHOST_TARGET.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.INVALID_GHOST_TARGET.getMessage()));

        then(runningSessionService).should().createRunningSession(currentUserId, request);
    }

    @DisplayName("러닝 세션 종료 응답을 성공 응답 포맷으로 감싼다")
    @Test
    void wrapsCompleteRunningSessionResponseInSuccessEnvelope() throws Exception {
        long currentUserId = 1L;
        long runningSessionId = 901L;
        RunningSessionDetailResponse response = new RunningSessionDetailResponse(
                RunningSessionCompleteState.SUCCESS,
                runningSessionId,
                currentUserId,
                null,
                LocalDateTime.parse("2026-03-12T06:30:00"),
                LocalDateTime.parse("2026-03-12T07:12:00"),
                10023,
                2520,
                11.0,
                2,
                642,
                3,
                java.util.List.of(new RunningGpsSampleResponse(
                        LocalDateTime.parse("2026-03-12T06:31:30"),
                        0,
                        37.5665,
                        126.9780,
                        21.5,
                        5.2,
                        182.0,
                        3.45,
                        289,
                        1250,
                        (short) 174
                )),
                java.util.List.of(new RunningHealthSampleResponse(
                        LocalDateTime.parse("2026-03-12T06:31:30"),
                        (short) 152,
                        84.5
                )),
                java.util.List.of(),
                null
        );
        given(runningSessionService.completeRunningSession(eq(runningSessionId), eq(currentUserId), any(CompleteRunningSessionRequest.class)))
                .willReturn(response);

        mockMvc.perform(patch("/api/running-sessions/{runningSessionId}", runningSessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(currentUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateRequestJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.OK.getMessage()))
                .andExpect(jsonPath("$.data.runningSessionId").value(runningSessionId))
                .andExpect(jsonPath("$.data.userId").value(currentUserId))
                .andExpect(jsonPath("$.data.startedAt").value("2026-03-12T06:30:00"))
                .andExpect(jsonPath("$.data.endedAt").value("2026-03-12T07:12:00"))
                .andExpect(jsonPath("$.data.distanceM").value(10023))
                .andExpect(jsonPath("$.data.caloriesKcal").value(642))
                .andExpect(jsonPath("$.data.avgSpeedMps").value(11.0))
                .andExpect(jsonPath("$.data.durationSec").value(2520))
                .andExpect(jsonPath("$.data.avgPaceSecPerKm").value(2))
                .andExpect(jsonPath("$.data.completeState").value(RunningSessionCompleteState.SUCCESS.name()))
                .andExpect(jsonPath("$.data.elevationGainM").value(3))
                .andExpect(jsonPath("$.data.gpsSamples[0].sampledAt").value("2026-03-12T06:31:30"))
                .andExpect(jsonPath("$.data.gpsSamples[0].time").value(0))
                .andExpect(jsonPath("$.data.gpsSamples[0].speedMps").value(3.45))
                .andExpect(jsonPath("$.data.gpsSamples[0].paceSecPerKm").value(289))
                .andExpect(jsonPath("$.data.gpsSamples[0].distanceM").value(1250))
                .andExpect(jsonPath("$.data.gpsSamples[0].cadenceSpm").value(174))
                .andExpect(jsonPath("$.data.healthSamples[0].sampledAt").value("2026-03-12T06:31:30"))
                .andExpect(jsonPath("$.data.healthSamples[0].heartRate").value(152))
                .andExpect(jsonPath("$.data.healthSamples[0].caloriesKcal").value(84.5))
                .andExpect(jsonPath("$.data.healthSamples[0].speedMps").doesNotExist())
                .andExpect(jsonPath("$.data.splits").isArray())
                .andExpect(jsonPath("$.data.ghostRunningResult").doesNotExist());

        ArgumentCaptor<CompleteRunningSessionRequest> requestCaptor = ArgumentCaptor.forClass(CompleteRunningSessionRequest.class);
        then(runningSessionService).should().completeRunningSession(eq(runningSessionId), eq(currentUserId), requestCaptor.capture());

        CompleteRunningSessionRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.gpsSamples()).hasSize(1);
        assertThat(capturedRequest.healthSamples()).hasSize(1);
        assertThat(capturedRequest.gpsSamples().getFirst().speedMps()).isEqualTo(3.45);
        assertThat(capturedRequest.gpsSamples().getFirst().paceSecPerKm()).isEqualTo(289);
        assertThat(capturedRequest.gpsSamples().getFirst().distanceM()).isEqualTo(1250);
        assertThat(capturedRequest.gpsSamples().getFirst().cadenceSpm()).isEqualTo((short) 174);
        assertThat(capturedRequest.healthSamples().getFirst().caloriesKcal()).isEqualTo(84.5);
    }

    @DisplayName("종료 요청의 distanceGapM 이 음수여도 서비스로 전달한다")
    @Test
    void passesNegativeDistanceGapMToService() throws Exception {
        long currentUserId = 1L;
        long runningSessionId = 901L;
        RunningSessionDetailResponse response = new RunningSessionDetailResponse(
                RunningSessionCompleteState.SUCCESS,
                runningSessionId,
                currentUserId,
                null,
                LocalDateTime.parse("2026-03-12T06:30:00"),
                LocalDateTime.parse("2026-03-12T07:12:00"),
                10023,
                2520,
                11.0,
                2,
                642,
                3,
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of(),
                null
        );
        given(runningSessionService.completeRunningSession(eq(runningSessionId), eq(currentUserId), any(CompleteRunningSessionRequest.class)))
                .willReturn(response);

        mockMvc.perform(patch("/api/running-sessions/{runningSessionId}", runningSessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(currentUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateRequestJson(-12)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()));

        ArgumentCaptor<CompleteRunningSessionRequest> requestCaptor = ArgumentCaptor.forClass(CompleteRunningSessionRequest.class);
        then(runningSessionService).should().completeRunningSession(eq(runningSessionId), eq(currentUserId), requestCaptor.capture());

        assertThat(requestCaptor.getValue().distanceGapM()).isEqualTo(-12);
    }

    @DisplayName("이미 종료된 세션은 충돌 응답을 반환한다")
    @Test
    void returnsConflictWhenRunningSessionIsAlreadyCompleted() throws Exception {
        long currentUserId = 1L;
        long runningSessionId = 901L;
        given(runningSessionService.completeRunningSession(eq(runningSessionId), eq(currentUserId), any(CompleteRunningSessionRequest.class)))
                .willThrow(new CustomException(ResponseCode.RUNNING_SESSION_ALREADY_COMPLETED));

        mockMvc.perform(patch("/api/running-sessions/{runningSessionId}", runningSessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(currentUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateRequestJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.RUNNING_SESSION_ALREADY_COMPLETED.getCode()));

        then(runningSessionService).should().completeRunningSession(eq(runningSessionId), eq(currentUserId), any(CompleteRunningSessionRequest.class));
    }

    @DisplayName("존재하지 않는 세션은 찾을 수 없음 응답을 반환한다")
    @Test
    void returnsNotFoundWhenRunningSessionDoesNotExist() throws Exception {
        long currentUserId = 1L;
        long runningSessionId = 999L;
        given(runningSessionService.completeRunningSession(eq(runningSessionId), eq(currentUserId), any(CompleteRunningSessionRequest.class)))
                .willThrow(new CustomException(ResponseCode.RUNNING_SESSION_NOT_FOUND));

        mockMvc.perform(patch("/api/running-sessions/{runningSessionId}", runningSessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(currentUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateRequestJson()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.RUNNING_SESSION_NOT_FOUND.getCode()));

        then(runningSessionService).should().completeRunningSession(eq(runningSessionId), eq(currentUserId), any(CompleteRunningSessionRequest.class));
    }

    @DisplayName("러닝 세션 상세 조회 응답은 종료 상태를 포함한다")
    @Test
    void wrapsRunningSessionDetailResponseWithCompletionState() throws Exception {
        long currentUserId = 1L;
        long runningSessionId = 777L;
        RunningSessionDetailResponse response = new RunningSessionDetailResponse(
                RunningSessionCompleteState.SUCCESS,
                runningSessionId,
                currentUserId,
                "https://cdn.withrun.app/snapshots/777.png",
                LocalDateTime.parse("2026-03-12T06:30:00"),
                LocalDateTime.parse("2026-03-12T07:12:00"),
                10023,
                2520,
                11.0,
                2,
                642,
                3,
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of(),
                null
        );
        given(runningSessionService.findRunningSessionDetail(runningSessionId, currentUserId)).willReturn(response);

        mockMvc.perform(get("/api/running-sessions/{runningSessionId}/detail", runningSessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(currentUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.runningSessionId").value(runningSessionId))
                .andExpect(jsonPath("$.data.completeState").value(RunningSessionCompleteState.SUCCESS.name()));

        then(runningSessionService).should().findRunningSessionDetail(runningSessionId, currentUserId);
    }

    @DisplayName("유효하지 않은 종료 요청은 잘못된 입력 응답을 반환한다")
    @Test
    void returnsBadRequestWhenCompletionPayloadIsInvalid() throws Exception {
        long runningSessionId = 901L;

        mockMvc.perform(patch("/api/running-sessions/{runningSessionId}", runningSessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "distanceM": -1,
                                  "caloriesKcal": 642,
                                  "endLatitude": 30,
                                  "endLongitude": 150,
                                  "avgSpeedMps": 11,
                                  "durationSec": 2520,
                                  "avgPaceSecPerKm": 2,
                                  "elevationGainM": 3,
                                  "distanceGapM": 12,
                                  "gpsSamples": [],
                                  "healthSamples": [],
                                  "splits": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INPUT_VALUE.getCode()));
    }

    @DisplayName("러닝 기록 코스 등록 응답을 생성 성공 포맷으로 감싼다")
    @Test
    void wrapsRegisterRunningSessionCourseResponseInCreatedEnvelope() throws Exception {
        long currentUserId = 7L;
        long runningSessionId = 901L;
        RegisterRunningSessionCourseRequest request = validRegisterCourseRequest();
        RegisterRunningSessionCourseResponse response = new RegisterRunningSessionCourseResponse(
                101L,
                "한강 야간 러닝 10K",
                CourseStatus.COMMUNITY,
                new RegisterRunningSessionCourseResponse.DifficultyOption("MEDIUM", "보통"),
                10000,
                120,
                "https://cdn.example.com/course/snapshot.png",
                37.566501,
                126.978001,
                37.574501,
                126.989001,
                new Coordinates(java.util.List.of(
                        new GeoPoint(37.566501, 126.978001, 0.0),
                        new GeoPoint(37.574501, 126.989001, 1.0)
                ))
        );
        given(runningSessionService.registerRunningSessionCourse(runningSessionId, currentUserId, request)).willReturn(response);

        mockMvc.perform(post("/api/running-sessions/{runningSessionId}/register-course", runningSessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(currentUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRegisterCourseRequestBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.CREATED.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.CREATED.getMessage()))
                .andExpect(jsonPath("$.data.courseId").value(101L))
                .andExpect(jsonPath("$.data.status").value(CourseStatus.COMMUNITY.name()))
                .andExpect(jsonPath("$.data.difficulty.data").value("MEDIUM"))
                .andExpect(jsonPath("$.data.difficulty.label").value("보통"))
                .andExpect(jsonPath("$.data.coordinates[0].latitude").value(37.566501))
                .andExpect(jsonPath("$.data.coordinates[0].elevationM").value(0.0));

        then(runningSessionService).should().registerRunningSessionCourse(runningSessionId, currentUserId, request);
    }

    @DisplayName("코스 등록 요청에 필수 필드가 없으면 잘못된 입력 응답을 반환한다")
    @ParameterizedTest(name = "누락된 필드: {0}")
    @ValueSource(strings = {"title", "mode", "difficulty", "courseTypes", "routeType"})
    void returnsInvalidInputValueWhenRegisterCourseRequiredFieldIsMissing(String missingField) throws Exception {
        mockMvc.perform(post("/api/running-sessions/{runningSessionId}/register-course", 901L)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(7L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerCourseRequestBodyWithout(missingField)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INPUT_VALUE.getCode()))
                .andExpect(jsonPath("$.data[0].field").value(missingField));

        then(runningSessionService).shouldHaveNoInteractions();
    }

    @DisplayName("완료되지 않은 세션의 코스 등록 요청은 충돌 응답을 반환한다")
    @Test
    void returnsConflictWhenRegisterCourseRequestedForIncompleteSession() throws Exception {
        long currentUserId = 7L;
        long runningSessionId = 901L;
        RegisterRunningSessionCourseRequest request = validRegisterCourseRequest();
        given(runningSessionService.registerRunningSessionCourse(runningSessionId, currentUserId, request))
                .willThrow(new CustomException(ResponseCode.RUNNING_SESSION_NOT_COMPLETED));

        mockMvc.perform(post("/api/running-sessions/{runningSessionId}/register-course", runningSessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(currentUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRegisterCourseRequestBody()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.RUNNING_SESSION_NOT_COMPLETED.getCode()));

        then(runningSessionService).should().registerRunningSessionCourse(runningSessionId, currentUserId, request);
    }

    @DisplayName("코스 등록 중 네비게이션 번들 생성이 실패하면 서비스 불가 응답을 반환한다")
    @Test
    void returnsServiceUnavailableWhenRegisterCourseNavigationBundleGenerationFails() throws Exception {
        long currentUserId = 7L;
        long runningSessionId = 901L;
        RegisterRunningSessionCourseRequest request = validRegisterCourseRequest();
        given(runningSessionService.registerRunningSessionCourse(runningSessionId, currentUserId, request))
                .willThrow(new CustomException(ResponseCode.NAVIGATION_BUNDLE_FAILED));

        mockMvc.perform(post("/api/running-sessions/{runningSessionId}/register-course", runningSessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(currentUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRegisterCourseRequestBody()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.NAVIGATION_BUNDLE_FAILED.getCode()));

        then(runningSessionService).should().registerRunningSessionCourse(runningSessionId, currentUserId, request);
    }

    @DisplayName("과거 러닝 세션 조회 응답을 성공 응답 포맷으로 감싼다")
    @Test
    void wrapsPastRunningSessionsResponseInSuccessEnvelope() throws Exception {
        long currentUserId = 7L;
        PastRunningSessionsRequest request = new PastRunningSessionsRequest(2026, 3, 16, 10, "cursor-token");
        PastRunningSessionsResponse response = new PastRunningSessionsResponse(
                java.util.List.of(
                        new PastRunningSessionItemResponse(
                                120L,
                                LocalDateTime.of(2026, 3, 15, 19, 30),
                                7200,
                                2100,
                                430,
                                "https://cdn.withrun.app/snapshots/120.png",
                                42,
                                RunningSessionCompleteState.SUCCESS,
                                GHOST,
                                GhostResultStatus.WIN,
                                TimeSlot.EVENING
                        )
                ),
                true,
                "next-cursor"
        );
        given(runningSessionService.findPastRunningSessions(currentUserId, request)).willReturn(response);

        mockMvc.perform(get("/api/running-sessions/history/past")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(currentUserId))
                        .queryParam("year", "2026")
                        .queryParam("month", "3")
                        .queryParam("day", "16")
                        .queryParam("pageSize", "10")
                        .queryParam("cursor", "cursor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.OK.getMessage()))
                .andExpect(jsonPath("$.data.items[0].runningSessionId").value(120L))
                .andExpect(jsonPath("$.data.items[0].runningMode").value(GHOST.name()))
                .andExpect(jsonPath("$.data.items[0].ghostResultStatus").value(GhostResultStatus.WIN.name()))
                .andExpect(jsonPath("$.data.items[0].timeSlot").value(TimeSlot.EVENING.name()))
                .andExpect(jsonPath("$.data.hasMore").value(true))
                .andExpect(jsonPath("$.data.nextCursor").value("next-cursor"));

        then(runningSessionService).should().findPastRunningSessions(currentUserId, request);
    }

    @DisplayName("pageSize가 없으면 기본값 10으로 과거 러닝 세션을 조회한다")
    @Test
    void usesDefaultPageSizeWhenPastRunningSessionsPageSizeIsMissing() throws Exception {
        long currentUserId = 7L;
        PastRunningSessionsRequest request = new PastRunningSessionsRequest(null, null, null, 10, null);
        given(runningSessionService.findPastRunningSessions(currentUserId, request))
                .willReturn(new PastRunningSessionsResponse(java.util.List.of(), false, null));

        mockMvc.perform(get("/api/running-sessions/history/past")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(currentUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.hasMore").value(false));

        then(runningSessionService).should().findPastRunningSessions(currentUserId, request);
    }

    @DisplayName("과거 조회는 인증된 사용자 ID를 서비스에 전달한다")
    @Test
    void passesAuthenticatedUserIdToPastSessionsService() throws Exception {
        long currentUserId = 7L;
        PastRunningSessionsRequest request = new PastRunningSessionsRequest(2026, 3, null, 10, null);
        given(runningSessionService.findPastRunningSessions(currentUserId, request))
                .willReturn(new PastRunningSessionsResponse(java.util.List.of(), false, null));

        mockMvc.perform(get("/api/running-sessions/history/past")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(currentUserId))
                        .queryParam("year", "2026")
                        .queryParam("month", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        then(runningSessionService).should().findPastRunningSessions(currentUserId, request);
    }

    @DisplayName("month만 전달된 과거 조회는 잘못된 입력 응답을 반환한다")
    @Test
    void returnsBadRequestWhenPastRunningSessionFiltersAreInvalid() throws Exception {
        mockMvc.perform(get("/api/running-sessions/history/past")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(7L))
                        .queryParam("month", "3"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INPUT_VALUE.getCode()));

        then(runningSessionService).shouldHaveNoInteractions();
    }

    @DisplayName("존재할 수 없는 날짜 조합은 컨트롤러 레벨에서 잘못된 입력 응답을 반환한다")
    @Test
    void returnsBadRequestWhenPastRunningSessionDateIsImpossible() throws Exception {
        mockMvc.perform(get("/api/running-sessions/history/past")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(7L))
                        .queryParam("year", "2026")
                        .queryParam("month", "2")
                        .queryParam("day", "30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INPUT_VALUE.getCode()));

        then(runningSessionService).shouldHaveNoInteractions();
    }

    private static CreateRunningSessionRequest validCreateRequest() {
        return new CreateRunningSessionRequest(COURSE, 300L, null, 30.0, 176.0);
    }

    private static CreateRunningSessionRequest ghostCreateRequest() {
        return new CreateRunningSessionRequest(GHOST, 300L, 44L, 30.0, 176.0);
    }

    private static String validCreateRequestBody() {
        return """
                {
                  "mode": "COURSE",
                  "courseId": 300,
                  "ghostTargetRunningSessionId": null,
                  "startedAt": "2026-03-12T06:30:00",
                  "startLatitude": 30.0,
                  "startLongitude": 176.0
                }
                """;
    }

    private static String ghostCreateRequestBody() {
        return """
                {
                  "mode": "GHOST",
                  "courseId": 300,
                  "ghostTargetRunningSessionId": 44,
                  "startedAt": "2026-03-12T06:30:00",
                  "startLatitude": 30.0,
                  "startLongitude": 176.0
                }
                """;
    }

    private static String requestBodyWithout(String missingField) {
        return switch (missingField) {
            case "mode" -> """
                    {
                      "courseId": 300,
                      "ghostTargetRunningSessionId": null,
                      "startedAt": "2026-03-12T06:30:00",
                      "startLatitude": 30.0,
                      "startLongitude": 176.0
                    }
                    """;
            case "startLatitude" -> """
                    {
                      "mode": "COURSE",
                      "courseId": 300,
                      "ghostTargetRunningSessionId": null,
                      "startedAt": "2026-03-12T06:30:00",
                      "startLongitude": 176.0
                    }
                    """;
            case "startLongitude" -> """
                    {
                      "mode": "COURSE",
                      "courseId": 300,
                      "ghostTargetRunningSessionId": null,
                      "startedAt": "2026-03-12T06:30:00",
                      "startLatitude": 30.0
                    }
                    """;
            default -> throw new IllegalArgumentException("Unsupported missing field: " + missingField);
        };
    }

    private static String requestBodyWith(String fieldName, String value) {
        String modeValue = "\"COURSE\"";
        String courseIdValue = "300";
        String ghostTargetValue = "null";
        String startedAtValue = "\"2026-03-12T06:30:00\"";
        String startLatitudeValue = "30.0";
        String startLongitudeValue = "176.0";

        if ("mode".equals(fieldName)) {
            modeValue = value;
        }
        if ("courseId".equals(fieldName)) {
            courseIdValue = value;
        }
        if ("ghostTargetRunningSessionId".equals(fieldName)) {
            ghostTargetValue = value;
        }
        if ("startedAt".equals(fieldName)) {
            startedAtValue = value;
        }
        if ("startLatitude".equals(fieldName)) {
            startLatitudeValue = value;
        }
        if ("startLongitude".equals(fieldName)) {
            startLongitudeValue = value;
        }

        return """
                {
                  "mode": %s,
                  "courseId": %s,
                  "ghostTargetRunningSessionId": %s,
                  "startedAt": %s,
                  "startLatitude": %s,
                  "startLongitude": %s
                }
                """.formatted(modeValue, courseIdValue, ghostTargetValue, startedAtValue, startLatitudeValue, startLongitudeValue);
    }

    private static RegisterRunningSessionCourseRequest validRegisterCourseRequest() {
        return new RegisterRunningSessionCourseRequest(
                "한강 야간 러닝 10K",
                "COMMUNITY",
                "MEDIUM",
                java.util.List.of("RIVERSIDE", "URBAN"),
                RouteType.LOOP,
                "https://cdn.example.com/course/snapshot.png"
        );
    }

    private static String validRegisterCourseRequestBody() {
        return """
                {
                  "title": "한강 야간 러닝 10K",
                  "mode": "COMMUNITY",
                  "difficulty": "MEDIUM",
                  "courseTypes": ["RIVERSIDE", "URBAN"],
                  "routeType": "LOOP",
                  "snapshotImageUrl": "https://cdn.example.com/course/snapshot.png"
                }
                """;
    }

    private static String registerCourseRequestBodyWithout(String missingField) {
        return switch (missingField) {
            case "title" -> """
                    {
                      "mode": "COMMUNITY",
                      "difficulty": "MEDIUM",
                      "courseTypes": ["RIVERSIDE", "URBAN"],
                      "routeType": "LOOP",
                      "snapshotImageUrl": "https://cdn.example.com/course/snapshot.png"
                    }
                    """;
            case "difficulty" -> """
                    {
                      "title": "한강 야간 러닝 10K",
                      "mode": "COMMUNITY",
                      "courseTypes": ["RIVERSIDE", "URBAN"],
                      "routeType": "LOOP",
                      "snapshotImageUrl": "https://cdn.example.com/course/snapshot.png"
                    }
                    """;
            case "mode" -> """
                    {
                      "title": "한강 야간 러닝 10K",
                      "difficulty": "MEDIUM",
                      "courseTypes": ["RIVERSIDE", "URBAN"],
                      "routeType": "LOOP",
                      "snapshotImageUrl": "https://cdn.example.com/course/snapshot.png"
                    }
                    """;
            case "courseTypes" -> """
                    {
                      "title": "한강 야간 러닝 10K",
                      "mode": "COMMUNITY",
                      "difficulty": "MEDIUM",
                      "routeType": "LOOP",
                      "snapshotImageUrl": "https://cdn.example.com/course/snapshot.png"
                    }
                    """;
            case "routeType" -> """
                    {
                      "title": "한강 야간 러닝 10K",
                      "mode": "COMMUNITY",
                      "difficulty": "MEDIUM",
                      "courseTypes": ["RIVERSIDE", "URBAN"],
                      "snapshotImageUrl": "https://cdn.example.com/course/snapshot.png"
                    }
                    """;
            default -> throw new IllegalArgumentException("Unsupported missing field: " + missingField);
        };
    }

    private static String validUpdateRequestJson() {
        return validUpdateRequestJson(12);
    }

    private static String validUpdateRequestJson(int distanceGapM) {
        return """
                {
                  "completeState": "SUCCESS",
                  "distanceM": 10023,
                  "caloriesKcal": 642,
                  "endLatitude": 30,
                  "endLongitude": 150,
                  "avgSpeedMps": 11,
                  "durationSec": 2520,
                  "avgPaceSecPerKm": 2,
                  "elevationGainM": 3,
                  "distanceGapM": %d,
                  "gpsSamples": [
                    {
                      "latitude": 37.5665,
                      "longitude": 126.9780,
                      "altitudeM": 21.5,
                      "accuracyM": 5.2,
                      "bearingDeg": 182.0,
                      "speedMps": 3.45,
                      "paceSecPerKm": 289,
                      "distanceM": 1250,
                      "cadenceSpm": 174,
                      "sampledAt": "2026-03-12T06:31:30"
                    }
                  ],
                  "healthSamples": [
                    {
                      "sampledAt": "2026-03-12T06:31:30",
                      "heartRate": 152,
                      "caloriesKcal": 84.5
                    }
                  ],
                  "splits": []
                }
                """.formatted(distanceGapM);
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
