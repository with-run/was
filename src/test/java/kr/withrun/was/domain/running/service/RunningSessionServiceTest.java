package kr.withrun.was.domain.running.service;

import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.entity.CourseDifficulty;
import kr.withrun.was.domain.course.entity.CourseGhostLeaderboard;
import kr.withrun.was.domain.course.entity.CourseTypeMap;
import kr.withrun.was.domain.course.repository.CourseDifficultyRepository;
import kr.withrun.was.domain.course.repository.CourseGhostLeaderboardRepository;
import kr.withrun.was.domain.course.repository.CourseRepository;
import kr.withrun.was.domain.course.repository.CourseTypeMapRepository;
import kr.withrun.was.domain.course.service.CourseAccessPolicy;
import kr.withrun.was.domain.course.service.CourseSignalService;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.domain.course.type.RouteType;
import kr.withrun.was.domain.course.vo.Coordinates;
import kr.withrun.was.domain.course.vo.GeoPoint;
import kr.withrun.was.domain.file.service.CloudFrontSignedUrlService;
import kr.withrun.was.domain.navigation.exception.NavigationBundleGenerationException;
import kr.withrun.was.domain.navigation.service.bundle.NavigationBundleGenerationService;
import kr.withrun.was.domain.navigation.type.NavigationBundleFailureCode;
import kr.withrun.was.domain.reward.service.RewardPointGrantService;
import kr.withrun.was.domain.running.dto.CompleteRunningSessionRequest;
import kr.withrun.was.domain.running.dto.CreateRunningGpsSampleRequest;
import kr.withrun.was.domain.running.dto.CreateRunningHealthSampleRequest;
import kr.withrun.was.domain.running.dto.CreateRunningSessionRequest;
import kr.withrun.was.domain.running.dto.CreateRunningSessionResponse;
import kr.withrun.was.domain.running.dto.CreateRunningSessionSplitRequest;
import kr.withrun.was.domain.running.dto.PastRunningSessionItemResponse;
import kr.withrun.was.domain.running.dto.PastRunningSessionsRequest;
import kr.withrun.was.domain.running.dto.PastRunningSessionsResponse;
import kr.withrun.was.domain.running.dto.RegisterRunningSessionCourseRequest;
import kr.withrun.was.domain.running.dto.RegisterRunningSessionCourseResponse;
import kr.withrun.was.domain.running.dto.RunningGpsSampleResponse;
import kr.withrun.was.domain.running.dto.RunningHealthSampleResponse;
import kr.withrun.was.domain.running.dto.RunningSessionDetailResponse;
import kr.withrun.was.domain.running.entity.RunningGpsSample;
import kr.withrun.was.domain.running.entity.RunningSession;
import kr.withrun.was.domain.running.repository.GhostRunningResultRepository;
import kr.withrun.was.domain.running.repository.RunningGpsSampleRepository;
import kr.withrun.was.domain.running.repository.RunningHealthSampleRepository;
import kr.withrun.was.domain.running.repository.RunningSessionRepository;
import kr.withrun.was.domain.running.repository.RunningSessionSplitRepository;
import kr.withrun.was.domain.running.repository.query.dto.PastRunningSessionHistoryRow;
import kr.withrun.was.domain.running.type.GhostResultStatus;
import kr.withrun.was.domain.running.type.RunningMode;
import kr.withrun.was.domain.running.type.RunningSessionCompleteState;
import kr.withrun.was.domain.running.vo.RunningSessionDateRange;
import kr.withrun.was.domain.user.entity.UserCalendar;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.repository.UserCalendarRepository;
import kr.withrun.was.domain.user.repository.UserRepository;
import kr.withrun.was.domain.user.type.Gender;
import kr.withrun.was.global.common.type.Difficulty;
import kr.withrun.was.global.common.type.TimeSlot;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static kr.withrun.was.domain.running.type.RunningMode.COURSE;
import static kr.withrun.was.domain.running.type.RunningMode.FREE;
import static kr.withrun.was.domain.running.type.RunningMode.GHOST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("러닝 세션 서비스")
class RunningSessionServiceTest {

    private static final LocalDateTime STARTED_AT = LocalDateTime.of(2026, 3, 12, 6, 30);

    @Mock
    private RunningSessionRepository runningSessionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserCalendarRepository userCalendarRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseDifficultyRepository courseDifficultyRepository;

    @Mock
    private CourseGhostLeaderboardRepository courseGhostLeaderboardRepository;

    @Mock
    private CourseTypeMapRepository courseTypeMapRepository;

    @Mock
    private RunningGpsSampleRepository runningGpsSampleRepository;

    @Mock
    private RunningHealthSampleRepository runningHealthSampleRepository;

    @Mock
    private RunningSessionSplitRepository runningSessionSplitRepository;

    @Mock
    private GhostRunningResultRepository ghostRunningResultRepository;

    @Mock
    private GhostRunningResultService ghostRunningResultService;

    @Mock
    private RewardPointGrantService rewardPointGrantService;

    @Mock
    private CourseSignalService courseSignalService;

    @Mock
    private CloudFrontSignedUrlService cloudFrontSignedUrlService;

    @Mock
    private NavigationBundleGenerationService navigationBundleGenerationService;

    private RunningSessionService runningSessionService;

    @BeforeEach
    void setUp() {
        runningSessionService = new RunningSessionService(
                runningSessionRepository,
                userRepository,
                userCalendarRepository,
                courseRepository,
                courseDifficultyRepository,
                courseGhostLeaderboardRepository,
                courseTypeMapRepository,
                runningGpsSampleRepository,
                runningHealthSampleRepository,
                runningSessionSplitRepository,
                ghostRunningResultRepository,
                ghostRunningResultService,
                rewardPointGrantService,
                courseSignalService,
                cloudFrontSignedUrlService,
                navigationBundleGenerationService,
                new CourseAccessPolicy()
        );
        setField(runningSessionService, "duplicateCourseEndpointThresholdM", 150);
        setField(runningSessionService, "duplicateCourseDistanceDiffRatio", 0.12d);
        setField(runningSessionService, "duplicateCoursePhase2Enabled", false);
        setField(runningSessionService, "duplicateCourseShapeToleranceM", 45d);
        setField(runningSessionService, "duplicateCourseShapeMinOverlapRatio", 0.93d);
        setField(runningSessionService, "duplicateCourseShapeSegmentizeStepM", 15d);
        lenient().when(userCalendarRepository.findByUserIdAndCalendarDateAndDeletedAtIsNull(any(), any()))
                .thenReturn(Optional.empty());
        lenient().when(userCalendarRepository.findDailySummaries(any(), any(), any()))
                .thenReturn(List.of());
        lenient().when(userCalendarRepository.save(any(UserCalendar.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(cloudFrontSignedUrlService.normalizeObjectKey(any())).thenAnswer(invocation -> {
            String value = invocation.getArgument(0);
            if (value == null || value.isBlank()) {
                return null;
            }
            if (value.startsWith("http://") || value.startsWith("https://")) {
                int pathStart = value.indexOf("://");
                int firstSlashAfterHost = value.indexOf('/', pathStart + 3);
                if (firstSlashAfterHost < 0) {
                    return null;
                }
                int queryStart = value.indexOf('?', firstSlashAfterHost);
                String path = queryStart >= 0
                        ? value.substring(firstSlashAfterHost + 1, queryStart)
                        : value.substring(firstSlashAfterHost + 1);
                return path;
            }
            return value.startsWith("/") ? value.substring(1) : value;
        });
        lenient().when(cloudFrontSignedUrlService.generateSignedUrl(any())).thenAnswer(invocation -> {
            String value = invocation.getArgument(0);
            String normalized = cloudFrontSignedUrlService.normalizeObjectKey(value);
            return normalized == null ? null : "signed::" + normalized;
        });
    }

    @DisplayName("서비스는 클래스 기본 readOnly와 쓰기 메서드 트랜잭션을 함께 사용한다")
    @Test
    void usesReadOnlyAtClassLevelAndWritableMethods() throws NoSuchMethodException {
        Transactional classTransactional = RunningSessionService.class.getAnnotation(Transactional.class);
        Transactional createTransactional = RunningSessionService.class
                .getDeclaredMethod("createRunningSession", Long.class, CreateRunningSessionRequest.class)
                .getAnnotation(Transactional.class);
        Transactional completeTransactional = RunningSessionService.class
                .getDeclaredMethod("completeRunningSession", Long.class, Long.class, CompleteRunningSessionRequest.class)
                .getAnnotation(Transactional.class);

        assertThat(classTransactional).isNotNull();
        assertThat(classTransactional.readOnly()).isTrue();
        assertThat(createTransactional).isNotNull();
        assertThat(createTransactional.readOnly()).isFalse();
        assertThat(completeTransactional).isNotNull();
        assertThat(completeTransactional.readOnly()).isFalse();
    }

    @DisplayName("FREE 모드는 코스와 고스트 대상 없이 세션을 생성한다")
    @Test
    void createsFreeRunningSessionWithoutCourseOrGhostTarget() {
        User user = user(1L);
        stubUser(user);
        when(runningSessionRepository.save(any(RunningSession.class))).thenAnswer(invocation -> persist(invocation.getArgument(0), 901L));

        CreateRunningSessionResponse response = runningSessionService.createRunningSession(
                1L,
                new CreateRunningSessionRequest(FREE, null, null, 30.0, 176.0)
        );

        ArgumentCaptor<RunningSession> sessionCaptor = ArgumentCaptor.forClass(RunningSession.class);
        verify(runningSessionRepository, times(1)).save(sessionCaptor.capture());
        RunningSession savedSession = sessionCaptor.getValue();

        assertThat(savedSession.getUser()).isEqualTo(user);
        assertThat(savedSession.getCourse()).isNull();
        assertThat(savedSession.getGhostTargetRunningSession()).isNull();
        assertThat(savedSession.getMode()).isEqualTo(FREE);
        assertThat(savedSession.getStartedAt()).isNotNull();
        assertThat(savedSession.getDistanceM()).isZero();
        assertThat(savedSession.getCaloriesKcal()).isZero();
        assertThat(savedSession.getCompleteState()).isEqualTo(RunningSessionCompleteState.FAIL);

        assertThat(response.runningSessionId()).isEqualTo(901L);
        assertThat(response.mode()).isEqualTo(FREE);
        assertThat(response.courseId()).isNull();
        assertThat(response.ghostTargetRunningSessionId()).isNull();
        assertThat(response.distanceM()).isZero();
        assertThat(response.caloriesKcal()).isZero();
        assertThat(response.completeState()).isEqualTo(RunningSessionCompleteState.FAIL);

        verifyNoInteractions(courseRepository);
    }

    @DisplayName("COURSE 모드는 공식 코스로 세션을 생성한다")
    @Test
    void createsCourseRunningSessionWithOfficialCourse() {
        User user = user(2L);
        Course course = course(300L, CourseStatus.OFFICIAL);
        stubUser(user);
        when(courseRepository.findNotDeletedCourse(course.getId())).thenReturn(Optional.of(course));
        when(runningSessionRepository.save(any(RunningSession.class))).thenAnswer(invocation -> persist(invocation.getArgument(0), 902L));

        CreateRunningSessionResponse response = runningSessionService.createRunningSession(
                2L,
                new CreateRunningSessionRequest(COURSE, 300L, null, 30.0, 176.0)
        );

        ArgumentCaptor<RunningSession> sessionCaptor = ArgumentCaptor.forClass(RunningSession.class);
        verify(runningSessionRepository, times(1)).save(sessionCaptor.capture());
        RunningSession savedSession = sessionCaptor.getValue();

        assertThat(savedSession.getCourse()).isEqualTo(course);
        assertThat(savedSession.getGhostTargetRunningSession()).isNull();
        assertThat(savedSession.getMode()).isEqualTo(COURSE);
        assertThat(response.runningSessionId()).isEqualTo(902L);
        assertThat(response.mode()).isEqualTo(COURSE);
        assertThat(response.courseId()).isEqualTo(300L);
        assertThat(response.ghostTargetRunningSessionId()).isNull();
        assertThat(response.distanceM()).isZero();
        assertThat(response.caloriesKcal()).isZero();
        assertThat(response.completeState()).isEqualTo(RunningSessionCompleteState.FAIL);
    }

    @DisplayName("COURSE 모드는 커뮤니티 코스로도 세션을 생성한다")
    @Test
    void createsCourseRunningSessionWithCommunityCourse() {
        User user = user(31L);
        Course course = course(307L, CourseStatus.COMMUNITY);
        stubUser(user);
        when(courseRepository.findNotDeletedCourse(course.getId())).thenReturn(Optional.of(course));
        when(runningSessionRepository.save(any(RunningSession.class))).thenAnswer(invocation -> persist(invocation.getArgument(0), 904L));

        CreateRunningSessionResponse response = runningSessionService.createRunningSession(
                31L,
                new CreateRunningSessionRequest(COURSE, 307L, null, 30.0, 176.0)
        );

        ArgumentCaptor<RunningSession> sessionCaptor = ArgumentCaptor.forClass(RunningSession.class);
        verify(runningSessionRepository, times(1)).save(sessionCaptor.capture());
        RunningSession savedSession = sessionCaptor.getValue();

        assertThat(savedSession.getCourse()).isEqualTo(course);
        assertThat(savedSession.getMode()).isEqualTo(COURSE);
        assertThat(response.runningSessionId()).isEqualTo(904L);
        assertThat(response.courseId()).isEqualTo(307L);
        assertThat(response.mode()).isEqualTo(COURSE);
    }

    @DisplayName("COURSE 모드는 자신이 생성한 비공개 코스로 세션을 생성한다")
    @Test
    void createsCourseRunningSessionWithOwnedPrivateCourse() {
        User user = user(32L);
        Course course = course(308L, CourseStatus.PRIVATE, user);
        stubUser(user);
        when(courseRepository.findNotDeletedCourse(course.getId())).thenReturn(Optional.of(course));
        when(runningSessionRepository.save(any(RunningSession.class))).thenAnswer(invocation -> persist(invocation.getArgument(0), 905L));

        CreateRunningSessionResponse response = runningSessionService.createRunningSession(
                32L,
                new CreateRunningSessionRequest(COURSE, 308L, null, 30.0, 176.0)
        );

        ArgumentCaptor<RunningSession> sessionCaptor = ArgumentCaptor.forClass(RunningSession.class);
        verify(runningSessionRepository, times(1)).save(sessionCaptor.capture());
        RunningSession savedSession = sessionCaptor.getValue();

        assertThat(savedSession.getCourse()).isEqualTo(course);
        assertThat(savedSession.getMode()).isEqualTo(COURSE);
        assertThat(response.runningSessionId()).isEqualTo(905L);
        assertThat(response.courseId()).isEqualTo(308L);
        assertThat(response.mode()).isEqualTo(COURSE);
    }

    @DisplayName("COURSE 모드 시작 응답은 코스의 navigation bundle URL 을 포함한다")
    @Test
    void includesNavigationBundleUrlInCreateResponse() {
        User user = user(22L);
        Course course = course(320L, CourseStatus.OFFICIAL);
        course.updateNavigationBundleUrl("https://cdn.withrun.kr/navigation/latest/320.json");
        stubUser(user);
        when(courseRepository.findNotDeletedCourse(course.getId())).thenReturn(Optional.of(course));
        when(runningSessionRepository.save(any(RunningSession.class))).thenAnswer(invocation -> persist(invocation.getArgument(0), 922L));

        CreateRunningSessionResponse response = runningSessionService.createRunningSession(
                22L,
                new CreateRunningSessionRequest(COURSE, 320L, null, 30.0, 176.0)
        );

        assertThat(recordComponentNames(CreateRunningSessionResponse.class)).contains("navigationBundleUrl");
        assertThat(invokeAccessor(response, "navigationBundleUrl"))
                .isEqualTo("signed::navigation/latest/320.json");
    }

    @DisplayName("COURSE 모드 시작 응답은 빈 navigation bundle URL 을 null 로 반환한다")
    @Test
    void returnsNullWhenNavigationBundleUrlIsBlank() {
        User user = user(23L);
        Course course = course(321L, CourseStatus.OFFICIAL);
        course.updateNavigationBundleUrl("   ");
        stubUser(user);
        when(courseRepository.findNotDeletedCourse(course.getId())).thenReturn(Optional.of(course));
        when(runningSessionRepository.save(any(RunningSession.class))).thenAnswer(invocation -> persist(invocation.getArgument(0), 923L));

        CreateRunningSessionResponse response = runningSessionService.createRunningSession(
                23L,
                new CreateRunningSessionRequest(COURSE, 321L, null, 30.0, 176.0)
        );

        assertThat(response.navigationBundleUrl()).isNull();
    }

    @DisplayName("GHOST 모드는 공식 코스와 완료된 고스트 대상으로 세션을 생성한다")
    @Test
    void createsGhostRunningSessionWithEligibleGhostTarget() {
        User user = user(3L);
        Course course = course(301L, CourseStatus.OFFICIAL);
        RunningSession ghostTarget = completedRunningSession(44L);
        stubUser(user);
        when(courseRepository.findNotDeletedCourse(course.getId())).thenReturn(Optional.of(course));
        when(runningSessionRepository.findByIdAndDeletedAtIsNull(ghostTarget.getId()))
                .thenReturn(Optional.of(ghostTarget));
        when(runningSessionRepository.save(any(RunningSession.class))).thenAnswer(invocation -> persist(invocation.getArgument(0), 903L));

        CreateRunningSessionResponse response = runningSessionService.createRunningSession(
                3L,
                new CreateRunningSessionRequest(GHOST, 301L, 44L, 30.0, 176.0)
        );

        ArgumentCaptor<RunningSession> sessionCaptor = ArgumentCaptor.forClass(RunningSession.class);
        verify(runningSessionRepository, times(1)).save(sessionCaptor.capture());
        RunningSession savedSession = sessionCaptor.getValue();

        assertThat(savedSession.getCourse()).isEqualTo(course);
        assertThat(savedSession.getGhostTargetRunningSession()).isEqualTo(ghostTarget);
        assertThat(savedSession.getMode()).isEqualTo(GHOST);
        assertThat(savedSession.getIsPublic()).isTrue();
        assertThat(response.runningSessionId()).isEqualTo(903L);
        assertThat(response.mode()).isEqualTo(GHOST);
        assertThat(response.courseId()).isEqualTo(301L);
        assertThat(response.ghostTargetRunningSessionId()).isEqualTo(44L);
        assertThat(response.distanceM()).isZero();
        assertThat(response.caloriesKcal()).isZero();
        assertThat(response.completeState()).isEqualTo(RunningSessionCompleteState.FAIL);
    }

    @DisplayName("GHOST 모드는 커뮤니티 코스로도 세션을 생성한다")
    @Test
    void createsGhostRunningSessionWithCommunityCourse() {
        User user = user(33L);
        Course course = course(309L, CourseStatus.COMMUNITY);
        RunningSession ghostTarget = completedRunningSession(58L);
        stubUser(user);
        when(courseRepository.findNotDeletedCourse(course.getId())).thenReturn(Optional.of(course));
        when(runningSessionRepository.findByIdAndDeletedAtIsNull(ghostTarget.getId()))
                .thenReturn(Optional.of(ghostTarget));
        when(runningSessionRepository.save(any(RunningSession.class))).thenAnswer(invocation -> persist(invocation.getArgument(0), 906L));

        CreateRunningSessionResponse response = runningSessionService.createRunningSession(
                33L,
                new CreateRunningSessionRequest(GHOST, 309L, 58L, 30.0, 176.0)
        );

        ArgumentCaptor<RunningSession> sessionCaptor = ArgumentCaptor.forClass(RunningSession.class);
        verify(runningSessionRepository, times(1)).save(sessionCaptor.capture());
        RunningSession savedSession = sessionCaptor.getValue();

        assertThat(savedSession.getCourse()).isEqualTo(course);
        assertThat(savedSession.getGhostTargetRunningSession()).isEqualTo(ghostTarget);
        assertThat(savedSession.getMode()).isEqualTo(GHOST);
        assertThat(response.runningSessionId()).isEqualTo(906L);
        assertThat(response.courseId()).isEqualTo(309L);
        assertThat(response.ghostTargetRunningSessionId()).isEqualTo(58L);
    }

    @DisplayName("GHOST 모드는 자신이 생성한 비공개 코스로 세션을 생성한다")
    @Test
    void createsGhostRunningSessionWithOwnedPrivateCourse() {
        User user = user(34L);
        Course course = course(310L, CourseStatus.PRIVATE, user);
        RunningSession ghostTarget = completedRunningSession(59L);
        stubUser(user);
        when(courseRepository.findNotDeletedCourse(course.getId())).thenReturn(Optional.of(course));
        when(runningSessionRepository.findByIdAndDeletedAtIsNull(ghostTarget.getId()))
                .thenReturn(Optional.of(ghostTarget));
        when(runningSessionRepository.save(any(RunningSession.class))).thenAnswer(invocation -> persist(invocation.getArgument(0), 907L));

        CreateRunningSessionResponse response = runningSessionService.createRunningSession(
                34L,
                new CreateRunningSessionRequest(GHOST, 310L, 59L, 30.0, 176.0)
        );

        ArgumentCaptor<RunningSession> sessionCaptor = ArgumentCaptor.forClass(RunningSession.class);
        verify(runningSessionRepository, times(1)).save(sessionCaptor.capture());
        RunningSession savedSession = sessionCaptor.getValue();

        assertThat(savedSession.getCourse()).isEqualTo(course);
        assertThat(savedSession.getGhostTargetRunningSession()).isEqualTo(ghostTarget);
        assertThat(savedSession.getMode()).isEqualTo(GHOST);
        assertThat(response.runningSessionId()).isEqualTo(907L);
        assertThat(response.courseId()).isEqualTo(310L);
        assertThat(response.ghostTargetRunningSessionId()).isEqualTo(59L);
    }

    @DisplayName("GHOST 모드 시작 응답은 대상 세션의 GPS 샘플을 포함한다")
    @Test
    void includesGhostTargetGpsSamplesInGhostCreateResponse() {
        User user = user(30L);
        Course course = course(306L, CourseStatus.OFFICIAL);
        RunningSession ghostTarget = completedRunningSession(57L);
        RunningGpsSample firstGpsSample = gpsSample(
                ghostTarget,
                99L,
                LocalDateTime.of(2026, 3, 12, 6, 30, 5),
                37.5665,
                126.9780,
                12.3,
                3.2,
                181.0,
                null,
                null,
                null,
                null
        );
        RunningGpsSample secondGpsSample = gpsSample(
                ghostTarget,
                99L,
                LocalDateTime.of(2026, 3, 12, 6, 30, 15),
                37.5667,
                126.9782,
                12.8,
                2.9,
                184.0,
                null,
                null,
                null,
                null
        );
        stubUser(user);
        when(courseRepository.findNotDeletedCourse(course.getId())).thenReturn(Optional.of(course));
        when(runningSessionRepository.findByIdAndDeletedAtIsNull(ghostTarget.getId()))
                .thenReturn(Optional.of(ghostTarget));
        when(runningSessionRepository.save(any(RunningSession.class))).thenAnswer(invocation -> persist(invocation.getArgument(0), 904L));
        when(runningGpsSampleRepository.findByRunningSessionIdOrderBySampledAtAsc(ghostTarget.getId()))
                .thenReturn(List.of(firstGpsSample, secondGpsSample));

        CreateRunningSessionResponse response = runningSessionService.createRunningSession(
                30L,
                new CreateRunningSessionRequest(GHOST, 306L, 57L, 30.0, 176.0)
        );

        assertThat(recordComponentNames(CreateRunningSessionResponse.class)).contains("ghostTargetGpsSamples");
        assertThat(invokeAccessor(response, "ghostTargetGpsSamples")).isEqualTo(List.of(
                new RunningGpsSampleResponse(
                        LocalDateTime.of(2026, 3, 12, 6, 30, 5),
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
                ),
                new RunningGpsSampleResponse(
                        LocalDateTime.of(2026, 3, 12, 6, 30, 15),
                        10,
                        37.5667,
                        126.9782,
                        12.8,
                        2.9,
                        184.0,
                        null,
                        null,
                        null,
                        null
                )
        ));
        verify(runningGpsSampleRepository).findByRunningSessionIdOrderBySampledAtAsc(57L);
    }

    @DisplayName("GHOST 모드에서 대상 ID가 없고 내 리더보드 기록도 없으면 빈 GPS 샘플 목록으로 세션을 생성한다")
    @Test
    void createsGhostRunningSessionWithEmptyFallbackGpsSamplesWhenLeaderboardRecordIsMissing() {
        User user = user(13L);
        Course course = course(303L, CourseStatus.OFFICIAL);
        stubUser(user);
        when(courseRepository.findNotDeletedCourse(course.getId())).thenReturn(Optional.of(course));
        when(courseGhostLeaderboardRepository.findTopByUserIdAndCourseId(user.getId(), course.getId()))
                .thenReturn(Optional.empty());
        when(runningSessionRepository.save(any(RunningSession.class))).thenAnswer(invocation -> persist(invocation.getArgument(0), 905L));

        CreateRunningSessionResponse response = runningSessionService.createRunningSession(
                13L,
                new CreateRunningSessionRequest(GHOST, 303L, null, 30.0, 176.0)
        );

        ArgumentCaptor<RunningSession> sessionCaptor = ArgumentCaptor.forClass(RunningSession.class);
        verify(runningSessionRepository).save(sessionCaptor.capture());
        assertThat(sessionCaptor.getValue().getGhostTargetRunningSession()).isNull();
        assertThat(response.ghostTargetRunningSessionId()).isNull();
        assertThat(response.ghostTargetGpsSamples()).isEmpty();
        verify(runningGpsSampleRepository, never()).findByRunningSessionIdOrderBySampledAtAsc(any());
    }

    @DisplayName("GHOST 모드에서 대상 ID가 없으면 내 코스 리더보드 최고 기록을 고스트 대상으로 사용한다")
    @Test
    void createsGhostRunningSessionWithLeaderboardFallbackTarget() {
        User user = user(31L);
        Course course = course(307L, CourseStatus.OFFICIAL);
        RunningSession leaderboardTarget = completedRunningSession(58L);
        setField(leaderboardTarget, "course", course);
        CourseGhostLeaderboard leaderboard = leaderboard(700L, user, course, leaderboardTarget, 1250);
        RunningGpsSample gpsSample = gpsSample(
                leaderboardTarget,
                user.getId(),
                LocalDateTime.of(2026, 3, 12, 6, 30, 5),
                37.5665,
                126.9780,
                12.3,
                3.2,
                181.0,
                null,
                null,
                null,
                null
        );
        stubUser(user);
        when(courseRepository.findNotDeletedCourse(course.getId())).thenReturn(Optional.of(course));
        when(courseGhostLeaderboardRepository.findTopByUserIdAndCourseId(user.getId(), course.getId()))
                .thenReturn(Optional.of(leaderboard));
        when(runningSessionRepository.save(any(RunningSession.class))).thenAnswer(invocation -> persist(invocation.getArgument(0), 906L));
        when(runningGpsSampleRepository.findByRunningSessionIdOrderBySampledAtAsc(leaderboardTarget.getId()))
                .thenReturn(List.of(gpsSample));

        CreateRunningSessionResponse response = runningSessionService.createRunningSession(
                user.getId(),
                new CreateRunningSessionRequest(GHOST, course.getId(), null, 30.0, 176.0)
        );

        ArgumentCaptor<RunningSession> sessionCaptor = ArgumentCaptor.forClass(RunningSession.class);
        verify(runningSessionRepository).save(sessionCaptor.capture());
        assertThat(sessionCaptor.getValue().getGhostTargetRunningSession()).isEqualTo(leaderboardTarget);
        assertThat(response.ghostTargetRunningSessionId()).isEqualTo(leaderboardTarget.getId());
        assertThat(response.ghostTargetGpsSamples()).containsExactly(new RunningGpsSampleResponse(
                LocalDateTime.of(2026, 3, 12, 6, 30, 5),
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
        ));
    }

    @DisplayName("없는 사용자는 USER_NOT_FOUND 예외를 던진다")
    @Test
    void throwsUserNotFoundWhenUserDoesNotExist() {
        when(userRepository.findNotDeletedUser(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> runningSessionService.createRunningSession(
                10L,
                new CreateRunningSessionRequest(FREE, null, null, 30.0, 176.0)
        ))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.USER_NOT_FOUND);
    }

    @DisplayName("COURSE 모드에서 courseId가 없으면 잘못된 입력 예외를 던진다")
    @Test
    void throwsInvalidInputValueWhenCourseModeHasNoCourseId() {
        stubUser(user(11L));

        assertThatThrownBy(() -> runningSessionService.createRunningSession(
                11L,
                new CreateRunningSessionRequest(COURSE, null, null, 30.0, 176.0)
        ))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.INVALID_INPUT_VALUE);

        verify(runningSessionRepository, never()).save(any(RunningSession.class));
    }

    @DisplayName("COURSE 모드에서 다른 사용자의 비공개 코스면 COURSE_NOT_FOUND 예외를 던진다")
    @Test
    void throwsCourseNotFoundWhenPrivateCourseBelongsToAnotherUser() {
        User user = user(12L);
        Course privateCourse = course(302L, CourseStatus.PRIVATE, user(99L));
        stubUser(user);
        when(courseRepository.findNotDeletedCourse(privateCourse.getId())).thenReturn(Optional.of(privateCourse));

        assertThatThrownBy(() -> runningSessionService.createRunningSession(
                12L,
                new CreateRunningSessionRequest(COURSE, 302L, null, 30.0, 176.0)
        ))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.COURSE_NOT_FOUND);

        verify(runningSessionRepository, never()).save(any(RunningSession.class));
    }

    @DisplayName("GHOST 모드에서 고스트 대상을 찾지 못하면 GHOST_TARGET_NOT_FOUND 예외를 던진다")
    @Test
    void throwsGhostTargetNotFoundWhenGhostTargetDoesNotExist() {
        User user = user(14L);
        Course course = course(304L, CourseStatus.OFFICIAL);
        stubUser(user);
        when(courseRepository.findNotDeletedCourse(course.getId())).thenReturn(Optional.of(course));
        when(runningSessionRepository.findByIdAndDeletedAtIsNull(55L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> runningSessionService.createRunningSession(
                14L,
                new CreateRunningSessionRequest(GHOST, 304L, 55L, 30.0, 176.0)
        ))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.GHOST_TARGET_NOT_FOUND);

        verify(runningSessionRepository, never()).save(any(RunningSession.class));
    }

    @DisplayName("GHOST 모드에서 다른 사용자의 비공개 코스면 COURSE_NOT_FOUND 예외를 던진다")
    @Test
    void throwsCourseNotFoundWhenGhostModePrivateCourseBelongsToAnotherUser() {
        User user = user(35L);
        Course privateCourse = course(311L, CourseStatus.PRIVATE, user(199L));
        stubUser(user);
        when(courseRepository.findNotDeletedCourse(privateCourse.getId())).thenReturn(Optional.of(privateCourse));

        assertThatThrownBy(() -> runningSessionService.createRunningSession(
                35L,
                new CreateRunningSessionRequest(GHOST, 311L, 60L, 30.0, 176.0)
        ))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.COURSE_NOT_FOUND);

        verify(runningSessionRepository, never()).save(any(RunningSession.class));
    }

    @DisplayName("GHOST 모드에서 완료되지 않은 고스트 대상은 INVALID_GHOST_TARGET 예외를 던진다")
    @Test
    void throwsInvalidGhostTargetWhenGhostTargetIsNotCompleted() {
        User user = user(15L);
        Course course = course(305L, CourseStatus.OFFICIAL);
        RunningSession incompleteGhostTarget = runningSession(56L, false);
        stubUser(user);
        when(courseRepository.findNotDeletedCourse(course.getId())).thenReturn(Optional.of(course));
        when(runningSessionRepository.findByIdAndDeletedAtIsNull(incompleteGhostTarget.getId()))
                .thenReturn(Optional.of(incompleteGhostTarget));

        assertThatThrownBy(() -> runningSessionService.createRunningSession(
                15L,
                new CreateRunningSessionRequest(GHOST, 305L, 56L, 30.0, 176.0)
        ))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.INVALID_GHOST_TARGET);

        verify(runningSessionRepository, never()).save(any(RunningSession.class));
    }


    @DisplayName("레거시 업데이트 기반 종료 API와 요청 DTO는 제거되어야 한다")
    @Test
    void doesNotExposeLegacyUpdateCompletionApi() {
        assertThat(Arrays.stream(RunningSessionService.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("completeRunningSession"))
                .flatMap(method -> Arrays.stream(method.getParameterTypes()))
                .map(Class::getSimpleName)
                .toList()).doesNotContain("UpdateRunningSessionRequest");

        assertThatThrownBy(() -> Class.forName("kr.withrun.was.domain.running.dto.UpdateRunningSessionRequest"))
                .isInstanceOf(ClassNotFoundException.class);
    }

    @DisplayName("세션 소유 사용자와 요청 사용자 ID가 다르면 세션 없음으로 처리한다")
    @Test
    void treatsDifferentUserAsRunningSessionNotFound() {
        RunningSession runningSession = runningSession(1L, 2L, false, STARTED_AT);
        CompleteRunningSessionRequest request = validCompleteRequest();
        when(runningSessionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(runningSession));
        when(userRepository.findNotDeletedUser(1L)).thenReturn(Optional.of(user(1L)));

        assertThatThrownBy(() -> runningSessionService.completeRunningSession(1L, 1L, request))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.RUNNING_SESSION_NOT_FOUND);
    }

    @DisplayName("이미 종료된 세션은 중복 종료를 거부한다")
    @Test
    void rejectsAlreadyCompletedRunningSession() {
        RunningSession runningSession = runningSession(1L, 1L, true, STARTED_AT);
        CompleteRunningSessionRequest request = validCompleteRequest();
        when(runningSessionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(runningSession));
        when(userRepository.findNotDeletedUser(1L)).thenReturn(Optional.of(user(1L)));

        assertThatThrownBy(() -> runningSessionService.completeRunningSession(1L, 1L, request))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.RUNNING_SESSION_ALREADY_COMPLETED);
    }

    @DisplayName("존재하지 않는 세션은 종료할 수 없다")
    @Test
    void throwsRunningSessionNotFoundWhenSessionDoesNotExist() {
        CompleteRunningSessionRequest request = validCompleteRequest();
        when(runningSessionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> runningSessionService.completeRunningSession(1L, 1L, request))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.RUNNING_SESSION_NOT_FOUND);
    }

    @DisplayName("종료 시각은 요청값 대신 서버 현재 시각으로 반영한다")
    @Test
    void appliesCurrentTimeForEndedAt() {
        RunningSession runningSession = runningSession(1L, 1L, false, STARTED_AT);
        CompleteRunningSessionRequest request = validCompleteRequest();
        when(runningSessionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(runningSession));
        when(userRepository.findNotDeletedUser(1L)).thenReturn(Optional.of(user(1L)));
        when(runningGpsSampleRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(runningHealthSampleRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(runningSessionSplitRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime beforeComplete = LocalDateTime.now();
        RunningSessionDetailResponse response = runningSessionService.completeRunningSession(1L, 1L, request);
        LocalDateTime afterComplete = LocalDateTime.now();

        assertThat(response.endedAt()).isBetween(beforeComplete, afterComplete);
        assertThat(runningSession.getEndedAt()).isEqualTo(response.endedAt());
        assertThat(runningSession.getCompleteState()).isEqualTo(RunningSessionCompleteState.SUCCESS);
        assertThat(response.completeState()).isEqualTo(RunningSessionCompleteState.SUCCESS);
        verify(rewardPointGrantService).grantCompletedRunningRewards(eq(runningSession), any(UserCalendar.class), isNull());
    }

    @DisplayName("종료 요청의 completeState 값으로 세션 종료 상태를 반영한다")
    @Test
    void appliesRequestedCompleteState() {
        RunningSession runningSession = runningSession(11L, 1L, false, STARTED_AT);
        CompleteRunningSessionRequest request = completeRequestWithState(RunningSessionCompleteState.GIVEUP);
        when(runningSessionRepository.findByIdAndDeletedAtIsNull(11L)).thenReturn(Optional.of(runningSession));
        when(userRepository.findNotDeletedUser(1L)).thenReturn(Optional.of(user(1L)));
        when(runningGpsSampleRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(runningHealthSampleRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(runningSessionSplitRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RunningSessionDetailResponse response = runningSessionService.completeRunningSession(11L, 1L, request);

        assertThat(runningSession.getCompleteState()).isEqualTo(RunningSessionCompleteState.GIVEUP);
        assertThat(response.completeState()).isEqualTo(RunningSessionCompleteState.GIVEUP);
    }

    @DisplayName("상세 조회 응답은 러닝 세션 완료 여부를 포함한다")
    @Test
    void includesCompletionStateInRunningSessionDetailResponse() {
        RunningSession runningSession = runningSession(93L, 1L, true, STARTED_AT);
        when(runningSessionRepository.findByIdAndDeletedAtIsNull(93L)).thenReturn(Optional.of(runningSession));
        when(runningGpsSampleRepository.findByRunningSessionIdOrderBySampledAtAsc(93L)).thenReturn(List.of());
        when(runningHealthSampleRepository.findByRunningSessionIdOrderBySampledAtAsc(93L)).thenReturn(List.of());
        when(runningSessionSplitRepository.findByRunningSessionIdOrderBySplitIndexAsc(93L)).thenReturn(List.of());
        when(ghostRunningResultRepository.findByRunningSessionIdAndDeletedAtIsNull(93L)).thenReturn(Optional.empty());
        setField(runningSession, "snapshotImageUrl", "snapshots/93.png");

        RunningSessionDetailResponse response = runningSessionService.findRunningSessionDetail(93L, 1L);

        assertThat(response.completeState()).isEqualTo(RunningSessionCompleteState.SUCCESS);
        assertThat(response.snapshotImageUrl()).isEqualTo("signed::snapshots/93.png");
    }

    @DisplayName("상세 조회 응답의 GPS 샘플은 첫 sampledAt 기준 경과 초를 time으로 포함한다")
    @Test
    void includesElapsedTimeInRunningSessionDetailGpsSamples() {
        RunningSession runningSession = runningSession(193L, 1L, true, STARTED_AT);
        RunningGpsSample firstGpsSample = gpsSample(
                runningSession,
                1L,
                LocalDateTime.of(2026, 3, 18, 6, 31, 30),
                37.5665,
                126.9780,
                21.5,
                5.2,
                182.0,
                3.45,
                289,
                1250,
                (short) 174
        );
        RunningGpsSample secondGpsSample = gpsSample(
                runningSession,
                1L,
                LocalDateTime.of(2026, 3, 18, 6, 31, 40),
                37.5666,
                126.9781,
                22.0,
                5.0,
                183.0,
                3.55,
                281,
                1290,
                (short) 176
        );
        when(runningSessionRepository.findByIdAndDeletedAtIsNull(193L)).thenReturn(Optional.of(runningSession));
        when(runningGpsSampleRepository.findByRunningSessionIdOrderBySampledAtAsc(193L))
                .thenReturn(List.of(firstGpsSample, secondGpsSample));
        when(runningHealthSampleRepository.findByRunningSessionIdOrderBySampledAtAsc(193L)).thenReturn(List.of());
        when(runningSessionSplitRepository.findByRunningSessionIdOrderBySplitIndexAsc(193L)).thenReturn(List.of());
        when(ghostRunningResultRepository.findByRunningSessionIdAndDeletedAtIsNull(193L)).thenReturn(Optional.empty());

        RunningSessionDetailResponse response = runningSessionService.findRunningSessionDetail(193L, 1L);

        assertThat(response.gpsSamples()).containsExactly(
                new RunningGpsSampleResponse(
                        LocalDateTime.of(2026, 3, 18, 6, 31, 30),
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
                ),
                new RunningGpsSampleResponse(
                        LocalDateTime.of(2026, 3, 18, 6, 31, 40),
                        10,
                        37.5666,
                        126.9781,
                        22.0,
                        5.0,
                        183.0,
                        3.55,
                        281,
                        1290,
                        (short) 176
                )
        );
    }

    @DisplayName("상세 종료 응답은 주행 지표를 GPS 샘플에 담고 건강 샘플은 생체 정보만 유지한다")
    @Test
    void mapsRunningMetricsOntoGpsSamplesWhenCompletingRunningSession() {
        RunningSession runningSession = runningSession(92L, 1L, false, STARTED_AT);
        CompleteRunningSessionRequest request = validCompleteRequest();
        when(runningSessionRepository.findByIdAndDeletedAtIsNull(92L)).thenReturn(Optional.of(runningSession));
        when(userRepository.findNotDeletedUser(1L)).thenReturn(Optional.of(user(1L)));
        when(runningGpsSampleRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(runningHealthSampleRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(runningSessionSplitRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RunningSessionDetailResponse response = runningSessionService.completeRunningSession(92L, 1L, request);

        assertThat(response.gpsSamples()).containsExactly(new RunningGpsSampleResponse(
                LocalDateTime.of(2026, 3, 18, 6, 31, 30),
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
        ));
        assertThat(response.healthSamples()).containsExactly(new RunningHealthSampleResponse(
                LocalDateTime.of(2026, 3, 18, 6, 31, 30),
                (short) 152,
                84.5
        ));
    }

    @DisplayName("고스트 대상 없이 시작한 GHOST 세션도 종료되며 첫 기록을 리더보드에 등록한다")
    @Test
    void completesGhostRunningSessionWithoutGhostTarget() {
        User user = user(41L);
        Course course = course(401L, CourseStatus.OFFICIAL);
        RunningSession runningSession = runningSession(94L, user.getId(), false, STARTED_AT);
        setField(runningSession, "mode", GHOST);
        setField(runningSession, "course", course);
        CompleteRunningSessionRequest request = validCompleteRequest();
        when(runningSessionRepository.findByIdAndDeletedAtIsNull(runningSession.getId())).thenReturn(Optional.of(runningSession));
        when(userRepository.findNotDeletedUser(user.getId())).thenReturn(Optional.of(user));
        when(runningGpsSampleRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(runningHealthSampleRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(runningSessionSplitRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(courseDifficultyRepository.findDifficultyByCourseId(course.getId())).thenReturn(Optional.of(Difficulty.MEDIUM));
        when(ghostRunningResultService.calculateGhostRunningPoint(1925, Difficulty.MEDIUM)).thenReturn(1200);
        when(courseGhostLeaderboardRepository.findTopByUserIdAndCourseId(user.getId(), course.getId()))
                .thenReturn(Optional.empty());
        when(courseGhostLeaderboardRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RunningSessionDetailResponse response = runningSessionService.completeRunningSession(runningSession.getId(), user.getId(), request);

        assertThat(response.completeState()).isEqualTo(RunningSessionCompleteState.SUCCESS);
        assertThat(response.ghostRunningResult()).isNull();
        verify(ghostRunningResultRepository, never()).save(any());

        ArgumentCaptor<CourseGhostLeaderboard> leaderboardCaptor = ArgumentCaptor.forClass(CourseGhostLeaderboard.class);
        verify(courseGhostLeaderboardRepository).save(leaderboardCaptor.capture());
        CourseGhostLeaderboard savedLeaderboard = leaderboardCaptor.getValue();
        assertThat(savedLeaderboard.getUser().getId()).isEqualTo(user.getId());
        assertThat(savedLeaderboard.getCourse()).isEqualTo(course);
        assertThat(savedLeaderboard.getRunningSession()).isEqualTo(runningSession);
        assertThat(savedLeaderboard.getPoint()).isEqualTo(1200);
    }

    @DisplayName("GIVEUP 으로 종료한 GHOST 세션은 리더보드에 업서트하지 않는다")
    @Test
    void doesNotUpsertLeaderboardWhenGhostRunEndsWithGiveup() {
        User user = user(43L);
        Course course = course(403L, CourseStatus.OFFICIAL);
        RunningSession runningSession = runningSession(98L, user.getId(), false, STARTED_AT);
        setField(runningSession, "mode", GHOST);
        setField(runningSession, "course", course);
        CompleteRunningSessionRequest request = completeRequestWithState(RunningSessionCompleteState.GIVEUP);
        when(runningSessionRepository.findByIdAndDeletedAtIsNull(runningSession.getId())).thenReturn(Optional.of(runningSession));
        when(userRepository.findNotDeletedUser(user.getId())).thenReturn(Optional.of(user));
        when(runningGpsSampleRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(runningHealthSampleRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(runningSessionSplitRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RunningSessionDetailResponse response = runningSessionService.completeRunningSession(runningSession.getId(), user.getId(), request);

        assertThat(response.completeState()).isEqualTo(RunningSessionCompleteState.GIVEUP);
        verifyNoInteractions(courseGhostLeaderboardRepository);
        verifyNoInteractions(ghostRunningResultService);
    }

    @DisplayName("SUCCESS 가 아닌 GHOST 세션은 상대가 SUCCESS 여도 패배 결과로 저장한다")
    @Test
    void storesLoseGhostResultWhenGhostRunEndsWithoutSuccess() {
        User user = user(44L);
        Course course = course(404L, CourseStatus.OFFICIAL);
        RunningSession ghostTarget = completedRunningSession(99L);
        setField(ghostTarget, "course", course);
        setField(ghostTarget, "durationSec", 1900);
        setField(ghostTarget, "user", user(440L));
        RunningSession runningSession = runningSession(100L, user.getId(), false, STARTED_AT);
        setField(runningSession, "mode", GHOST);
        setField(runningSession, "course", course);
        setField(runningSession, "ghostTargetRunningSession", ghostTarget);
        CompleteRunningSessionRequest request = completeRequestWithState(RunningSessionCompleteState.GIVEUP);
        when(runningSessionRepository.findByIdAndDeletedAtIsNull(runningSession.getId())).thenReturn(Optional.of(runningSession));
        when(userRepository.findNotDeletedUser(user.getId())).thenReturn(Optional.of(user));
        when(runningGpsSampleRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(runningHealthSampleRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(runningSessionSplitRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(ghostRunningResultRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RunningSessionDetailResponse response = runningSessionService.completeRunningSession(runningSession.getId(), user.getId(), request);

        assertThat(response.completeState()).isEqualTo(RunningSessionCompleteState.GIVEUP);
        assertThat(response.ghostRunningResult()).isNotNull();
        assertThat(response.ghostRunningResult().resultStatus()).isEqualTo(GhostResultStatus.LOSE);
        assertThat(response.ghostRunningResult().point()).isZero();
        assertThat(response.ghostRunningResult().timeGapSec()).isEqualTo(25);
        assertThat(response.ghostRunningResult().distanceGapM()).isZero();
        verify(ghostRunningResultRepository).save(any());
        verifyNoInteractions(courseGhostLeaderboardRepository);
    }

    @DisplayName("더 높은 고스트 점수로 갱신되면 리더보드 최고 행의 세션도 함께 갱신한다")
    @Test
    void updatesLeaderboardRunningSessionWhenGhostPointImproves() {
        User user = user(42L);
        Course course = course(402L, CourseStatus.OFFICIAL);
        RunningSession ghostTarget = completedRunningSession(96L);
        setField(ghostTarget, "course", course);
        setField(ghostTarget, "durationSec", 1900);
        setField(ghostTarget, "user", user(420L));
        RunningSession runningSession = runningSession(95L, user.getId(), false, STARTED_AT);
        setField(runningSession, "mode", GHOST);
        setField(runningSession, "course", course);
        setField(runningSession, "ghostTargetRunningSession", ghostTarget);
        CourseGhostLeaderboard existingLeaderboard = leaderboard(701L, user, course, completedRunningSession(97L), 900);
        CompleteRunningSessionRequest request = validCompleteRequest();
        when(runningSessionRepository.findByIdAndDeletedAtIsNull(runningSession.getId())).thenReturn(Optional.of(runningSession));
        when(userRepository.findNotDeletedUser(user.getId())).thenReturn(Optional.of(user));
        when(runningGpsSampleRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(runningHealthSampleRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(runningSessionSplitRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(courseDifficultyRepository.findDifficultyByCourseId(course.getId())).thenReturn(Optional.of(Difficulty.MEDIUM));
        when(ghostRunningResultService.calculateGhostRunningPoint(1925, Difficulty.MEDIUM)).thenReturn(1200);
        when(ghostRunningResultService.calculateGhostRunningPoint(1900, Difficulty.MEDIUM)).thenReturn(1100);
        when(courseGhostLeaderboardRepository.findTopByUserIdAndCourseId(user.getId(), course.getId()))
                .thenReturn(Optional.of(existingLeaderboard));
        when(ghostRunningResultRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RunningSessionDetailResponse response = runningSessionService.completeRunningSession(runningSession.getId(), user.getId(), request);

        assertThat(response.ghostRunningResult()).isNotNull();
        assertThat(existingLeaderboard.getPoint()).isEqualTo(1200);
        assertThat(existingLeaderboard.getRunningSession()).isEqualTo(runningSession);
    }

    @DisplayName("상세 종료는 오늘 러닝을 캘린더에 기록한다")
    @Test
    void recordsRunInUserCalendarWhenCompletingDetailedRunningSession() {
        User user = user(21L);
        RunningSession runningSession = runningSession(91L, 21L, false, STARTED_AT);
        CompleteRunningSessionRequest request = validCompleteRequest();
        when(runningSessionRepository.findByIdAndDeletedAtIsNull(91L)).thenReturn(Optional.of(runningSession));
        when(userRepository.findNotDeletedUser(21L)).thenReturn(Optional.of(user));
        when(runningGpsSampleRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(runningHealthSampleRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(runningSessionSplitRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RunningSessionDetailResponse response = runningSessionService.completeRunningSession(91L, 21L, request);

        ArgumentCaptor<UserCalendar> calendarCaptor = ArgumentCaptor.forClass(UserCalendar.class);
        verify(userCalendarRepository).save(calendarCaptor.capture());
        UserCalendar savedCalendar = calendarCaptor.getValue();
        assertThat(savedCalendar.getUser().getId()).isEqualTo(user.getId());
        assertThat(savedCalendar.getCalendarDate()).isEqualTo(runningSession.getStartedAt().toLocalDate());
        assertThat(savedCalendar.getTotalDistanceM()).isEqualTo(5320);
        assertThat(savedCalendar.getTotalDurationSec()).isEqualTo(1925);
        assertThat(savedCalendar.getTotalCaloriesKcal()).isEqualTo(328);
        assertThat(savedCalendar.getFreeRunCount()).isEqualTo(1);
        assertThat(savedCalendar.getCourseRunCount()).isZero();
        assertThat(savedCalendar.getGhostRunCount()).isZero();
    }

    @DisplayName("고스트 최고 점수 갱신 시 리더보드 러닝 세션 ID도 최신 세션으로 갱신한다")
    @Test
    void updatesLeaderboardRunningSessionWhenGhostBestPointIsImproved() {
        User user = user(31L);
        Course course = course(701L, CourseStatus.OFFICIAL);
        RunningSession runningSession = runningSession(111L, 31L, false, STARTED_AT);
        setField(runningSession, "mode", GHOST);
        setField(runningSession, "course", course);

        RunningSession ghostTargetSession = runningSession(222L, 99L, true, STARTED_AT.minusDays(1));
        setField(ghostTargetSession, "course", course);
        setField(ghostTargetSession, "durationSec", 2100);
        setField(runningSession, "ghostTargetRunningSession", ghostTargetSession);

        RunningSession previousBestSession = runningSession(333L, 31L, true, STARTED_AT.minusDays(2));
        CourseGhostLeaderboard existingLeaderboard = CourseGhostLeaderboard.create(user, course, previousBestSession, 950);

        CompleteRunningSessionRequest request = validCompleteRequest();

        when(runningSessionRepository.findByIdAndDeletedAtIsNull(111L)).thenReturn(Optional.of(runningSession));
        when(userRepository.findNotDeletedUser(31L)).thenReturn(Optional.of(user));
        when(runningGpsSampleRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(runningHealthSampleRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(runningSessionSplitRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(courseDifficultyRepository.findDifficultyByCourseId(701L)).thenReturn(Optional.of(Difficulty.MEDIUM));
        when(ghostRunningResultService.calculateGhostRunningPoint(1925, Difficulty.MEDIUM)).thenReturn(1000);
        when(ghostRunningResultService.calculateGhostRunningPoint(2100, Difficulty.MEDIUM)).thenReturn(900);
        when(ghostRunningResultRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(courseGhostLeaderboardRepository.findTopByUserIdAndCourseId(31L, 701L)).thenReturn(Optional.of(existingLeaderboard));

        runningSessionService.completeRunningSession(111L, 31L, request);

        assertThat(existingLeaderboard.getPoint()).isEqualTo(1000);
        assertThat(existingLeaderboard.getRunningSession()).isEqualTo(runningSession);
        verify(courseGhostLeaderboardRepository, never()).save(any(CourseGhostLeaderboard.class));
    }

    @DisplayName("완료된 러닝 세션을 요청한 모드의 코스로 등록한다")
    @Test
    void registersCompletedRunningSessionCourseWithRequestedMode() {
        RunningSession runningSession = runningSession(81L, 7L, true, STARTED_AT);
        setField(runningSession, "distanceM", 10000);
        setField(runningSession, "elevationGainM", 120);
        setField(runningSession, "snapshotImageUrl", "snapshots/81.png");
        setField(runningSession, "endLatitude", 37.574501);
        setField(runningSession, "endLongitude", 126.989001);
        setField(runningSession, "coordinates", new Coordinates(
                List.of(37.574501, 37.566501),
                List.of(126.989001, 126.978001),
                List.of(1.0, 0.0)
        ));
        RunningGpsSample firstGpsSample = gpsSample(
                runningSession,
                7L,
                LocalDateTime.of(2026, 3, 12, 6, 30, 5),
                37.566501,
                126.978001,
                15.5,
                3.0,
                180.0,
                null,
                null,
                null,
                null
        );
        RunningGpsSample secondGpsSample = gpsSample(
                runningSession,
                7L,
                LocalDateTime.of(2026, 3, 12, 6, 30, 15),
                37.574501,
                126.989001,
                18.2,
                2.6,
                184.0,
                null,
                null,
                null,
                null
        );
        Coordinates expectedCoordinates = new Coordinates(List.of(
                new GeoPoint(37.566501, 126.978001, 15.5),
                new GeoPoint(37.574501, 126.989001, 18.2)
        ));
        RegisterRunningSessionCourseRequest request = new RegisterRunningSessionCourseRequest(
                "한강 야간 러닝 10K",
                "COMMUNITY",
                "MEDIUM",
                List.of("RIVERSIDE", "URBAN", "RIVERSIDE"),
                RouteType.LOOP,
                "https://cdn.withrun.app/course/snapshot.png?Expires=100&Signature=test&Key-Pair-Id=K123"
        );
        stubUser(user(7L));
        when(runningSessionRepository.findByIdAndDeletedAtIsNull(81L)).thenReturn(Optional.of(runningSession));
        when(runningGpsSampleRepository.findByRunningSessionIdOrderBySampledAtAsc(81L))
                .thenReturn(List.of(firstGpsSample, secondGpsSample));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> persistCourse(invocation.getArgument(0), 501L));

        RegisterRunningSessionCourseResponse response =
                runningSessionService.registerRunningSessionCourse(81L, 7L, request);

        assertThat(response.courseId()).isEqualTo(501L);
        assertThat(response.title()).isEqualTo("한강 야간 러닝 10K");
        assertThat(response.status()).isEqualTo(CourseStatus.COMMUNITY);
        assertThat(response.difficulty()).isEqualTo(new RegisterRunningSessionCourseResponse.DifficultyOption("MEDIUM", "보통"));
        assertThat(response.distanceM()).isEqualTo(10000);
        assertThat(response.elevationGainM()).isEqualTo(120);
        assertThat(response.snapshotImageUrl()).isEqualTo("signed::course/snapshot.png");
        assertThat(response.coordinates()).isEqualTo(expectedCoordinates);

        ArgumentCaptor<Course> courseCaptor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(courseCaptor.capture());
        verify(runningGpsSampleRepository).findByRunningSessionIdOrderBySampledAtAsc(81L);
        Course savedCourse = courseCaptor.getValue();
        assertThat(savedCourse.getStatus()).isEqualTo(CourseStatus.COMMUNITY);
        assertThat(savedCourse.getUser()).isEqualTo(runningSession.getUser());
        assertThat(savedCourse.getSnapshotImageUrl()).isEqualTo("course/snapshot.png");
        assertThat(savedCourse.getCoordinates()).isEqualTo(expectedCoordinates);
        assertThat(savedCourse.getCoordinates()).isNotEqualTo(runningSession.getCoordinates());

        ArgumentCaptor<CourseDifficulty> difficultyCaptor = ArgumentCaptor.forClass(CourseDifficulty.class);
        verify(courseDifficultyRepository).save(difficultyCaptor.capture());
        assertThat(difficultyCaptor.getValue().getDifficulty()).isEqualTo(Difficulty.MEDIUM);
        assertThat(difficultyCaptor.getValue().getCourse()).isEqualTo(savedCourse);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CourseTypeMap>> courseTypeMapsCaptor = ArgumentCaptor.forClass((Class) List.class);
        verify(courseTypeMapRepository).saveAll(courseTypeMapsCaptor.capture());
        List<CourseTypeMap> courseTypeMaps = courseTypeMapsCaptor.getValue();
        assertThat(courseTypeMaps).hasSize(2);
        assertThat(courseTypeMaps)
                .extracting(CourseTypeMap::getCourseType)
                .containsExactly(CourseType.RIVERSIDE, CourseType.URBAN);
        assertThat(courseTypeMaps)
                .extracting(CourseTypeMap::getCourse)
                .containsOnly(savedCourse);
        verify(navigationBundleGenerationService).generate(501L);

        InOrder inOrder = inOrder(
                courseRepository,
                navigationBundleGenerationService,
                courseDifficultyRepository,
                courseTypeMapRepository,
                courseSignalService
        );
        inOrder.verify(courseRepository).save(any(Course.class));
        inOrder.verify(navigationBundleGenerationService).generate(501L);
        inOrder.verify(courseDifficultyRepository).save(any(CourseDifficulty.class));
        inOrder.verify(courseTypeMapRepository).saveAll(any());
        inOrder.verify(courseSignalService).upsertCourseFeature(savedCourse, Difficulty.MEDIUM, List.of(CourseType.RIVERSIDE, CourseType.URBAN));
    }

    @DisplayName("공개 코스와 유사한 자유 러닝 코스는 중복 등록을 거부한다")
    @Test
    void rejectsRegisterCourseWhenPublicDuplicateCourseExists() {
        RunningSession runningSession = runningSession(89L, 7L, true, STARTED_AT);
        setField(runningSession, "distanceM", 10000);
        setField(runningSession, "startLatitude", 37.566501);
        setField(runningSession, "startLongitude", 126.978001);
        setField(runningSession, "endLatitude", 37.574501);
        setField(runningSession, "endLongitude", 126.989001);

        RegisterRunningSessionCourseRequest request = new RegisterRunningSessionCourseRequest(
                "중복 검증 코스",
                "COMMUNITY",
                "MEDIUM",
                List.of("RIVERSIDE", "URBAN"),
                RouteType.LOOP,
                null
        );

        stubUser(user(7L));
        when(runningSessionRepository.findByIdAndDeletedAtIsNull(89L)).thenReturn(Optional.of(runningSession));
        when(courseRepository.existsPublicDuplicateCourse(
                37.566501,
                126.978001,
                37.574501,
                126.989001,
                10000,
                150,
                0.12
        )).thenReturn(true);

        assertThatThrownBy(() -> runningSessionService.registerRunningSessionCourse(89L, 7L, request))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.COURSE_ALREADY_EXISTS);

        verify(runningGpsSampleRepository, never()).findByRunningSessionIdOrderBySampledAtAsc(89L);
        verify(courseRepository, never()).save(any(Course.class));
        verify(navigationBundleGenerationService, never()).generate(any(Long.class));
        verify(courseDifficultyRepository, never()).save(any(CourseDifficulty.class));
        verify(courseTypeMapRepository, never()).saveAll(any());
        verify(courseSignalService, never()).upsertCourseFeature(any(Course.class), any(Difficulty.class), any());
    }

    @DisplayName("Phase2 활성화 시 route_geom shape 유사도 중복 검사를 우선 적용한다")
    @Test
    void rejectsRegisterCourseWhenPublicDuplicateCourseExistsByRouteGeometry() {
        setField(runningSessionService, "duplicateCoursePhase2Enabled", true);
        setField(runningSessionService, "duplicateCourseShapeToleranceM", 45d);
        setField(runningSessionService, "duplicateCourseShapeMinOverlapRatio", 0.93d);
        setField(runningSessionService, "duplicateCourseShapeSegmentizeStepM", 15d);
        RunningSession runningSession = runningSession(90L, 7L, true, STARTED_AT);
        setField(runningSession, "distanceM", 10000);
        setField(runningSession, "startLatitude", 37.566501);
        setField(runningSession, "startLongitude", 126.978001);
        setField(runningSession, "endLatitude", 37.574501);
        setField(runningSession, "endLongitude", 126.989001);
        RunningGpsSample firstGpsSample = gpsSample(
                runningSession,
                7L,
                LocalDateTime.of(2026, 4, 7, 7, 0, 0),
                37.566501,
                126.978001,
                10.2,
                3.1,
                180.0,
                null,
                null,
                null,
                null
        );
        RunningGpsSample secondGpsSample = gpsSample(
                runningSession,
                7L,
                LocalDateTime.of(2026, 4, 7, 7, 0, 10),
                37.574501,
                126.989001,
                12.4,
                3.2,
                178.0,
                null,
                null,
                null,
                null
        );

        RegisterRunningSessionCourseRequest request = new RegisterRunningSessionCourseRequest(
                "Phase2 중복 검증 코스",
                "COMMUNITY",
                "MEDIUM",
                List.of("RIVERSIDE", "URBAN"),
                RouteType.LOOP,
                null
        );

        stubUser(user(7L));
        when(runningSessionRepository.findByIdAndDeletedAtIsNull(90L)).thenReturn(Optional.of(runningSession));
        when(runningGpsSampleRepository.findByRunningSessionIdOrderBySampledAtAsc(90L))
                .thenReturn(List.of(firstGpsSample, secondGpsSample));
        when(courseRepository.existsPublicDuplicateCourseByRouteGeometry(
                eq(37.566501),
                eq(126.978001),
                eq(37.574501),
                eq(126.989001),
                eq(10000),
                eq(150),
                eq(0.12),
                anyString(),
                eq(45d),
                eq(0.93d),
                eq(15d)
        )).thenReturn(true);

        assertThatThrownBy(() -> runningSessionService.registerRunningSessionCourse(90L, 7L, request))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.COURSE_ALREADY_EXISTS);

        ArgumentCaptor<String> routeLineStringCaptor = ArgumentCaptor.forClass(String.class);
        verify(courseRepository).existsPublicDuplicateCourseByRouteGeometry(
                eq(37.566501),
                eq(126.978001),
                eq(37.574501),
                eq(126.989001),
                eq(10000),
                eq(150),
                eq(0.12),
                routeLineStringCaptor.capture(),
                eq(45d),
                eq(0.93d),
                eq(15d)
        );
        assertThat(routeLineStringCaptor.getValue())
                .startsWith("LINESTRING(")
                .contains("126.978001 37.566501")
                .contains("126.989001 37.574501");
        verify(courseRepository, never()).existsPublicDuplicateCourse(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt(), anyInt(), anyDouble());
        verify(courseRepository, never()).save(any(Course.class));
        verify(navigationBundleGenerationService, never()).generate(any(Long.class));
        verify(courseDifficultyRepository, never()).save(any(CourseDifficulty.class));
        verify(courseTypeMapRepository, never()).saveAll(any());
        verify(courseSignalService, never()).upsertCourseFeature(any(Course.class), any(Difficulty.class), any());
    }

    @DisplayName("Phase2 활성화 상태에서 경로 LineString 생성이 불가능하면 Phase1 중복 검사로 폴백한다")
    @Test
    void fallsBackToPhase1WhenRouteLineStringCannotBeBuilt() {
        setField(runningSessionService, "duplicateCoursePhase2Enabled", true);
        RunningSession runningSession = runningSession(91L, 7L, true, STARTED_AT);
        setField(runningSession, "distanceM", 10000);
        setField(runningSession, "startLatitude", 37.566501);
        setField(runningSession, "startLongitude", 126.978001);
        setField(runningSession, "endLatitude", 37.574501);
        setField(runningSession, "endLongitude", 126.989001);

        RegisterRunningSessionCourseRequest request = new RegisterRunningSessionCourseRequest(
                "Phase2 폴백 코스",
                "COMMUNITY",
                "MEDIUM",
                List.of("RIVERSIDE", "URBAN"),
                RouteType.LOOP,
                null
        );

        stubUser(user(7L));
        when(runningSessionRepository.findByIdAndDeletedAtIsNull(91L)).thenReturn(Optional.of(runningSession));
        when(runningGpsSampleRepository.findByRunningSessionIdOrderBySampledAtAsc(91L)).thenReturn(List.of());
        when(courseRepository.existsPublicDuplicateCourse(
                37.566501,
                126.978001,
                37.574501,
                126.989001,
                10000,
                150,
                0.12
        )).thenReturn(true);

        assertThatThrownBy(() -> runningSessionService.registerRunningSessionCourse(91L, 7L, request))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.COURSE_ALREADY_EXISTS);

        verify(courseRepository, never()).existsPublicDuplicateCourseByRouteGeometry(
                anyDouble(),
                anyDouble(),
                anyDouble(),
                anyDouble(),
                anyInt(),
                anyInt(),
                anyDouble(),
                anyString(),
                anyDouble(),
                anyDouble(),
                anyDouble()
        );
        verify(courseRepository).existsPublicDuplicateCourse(
                37.566501,
                126.978001,
                37.574501,
                126.989001,
                10000,
                150,
                0.12
        );
        verify(courseRepository, never()).save(any(Course.class));
    }

    @DisplayName("완료된 러닝 세션을 PRIVATE 코스로 등록한다")
    @Test
    void registersCompletedRunningSessionCourseAsPrivateCourse() {
        RunningSession runningSession = runningSession(84L, 7L, true, STARTED_AT);
        setField(runningSession, "snapshotImageUrl", "snapshots/84.png");
        setField(runningSession, "coordinates", new Coordinates(
                List.of(37.566501, 37.565901),
                List.of(126.978001, 126.977501),
                List.of(0.0, 0.0)
        ));
        RunningGpsSample firstGpsSample = gpsSample(
                runningSession,
                7L,
                LocalDateTime.of(2026, 3, 12, 6, 30, 10),
                37.565901,
                126.977501,
                11.8,
                3.1,
                175.0,
                null,
                null,
                null,
                null
        );
        RunningGpsSample secondGpsSample = gpsSample(
                runningSession,
                7L,
                LocalDateTime.of(2026, 3, 12, 6, 30, 20),
                37.566501,
                126.978001,
                12.4,
                2.8,
                178.0,
                null,
                null,
                null,
                null
        );
        Coordinates expectedCoordinates = new Coordinates(List.of(
                new GeoPoint(37.565901, 126.977501, 11.8),
                new GeoPoint(37.566501, 126.978001, 12.4)
        ));
        RegisterRunningSessionCourseRequest request = new RegisterRunningSessionCourseRequest(
                "개인 보관 코스",
                "PRIVATE",
                "EASY",
                List.of("RIVERSIDE"),
                RouteType.LOOP,
                null
        );
        stubUser(user(7L));
        when(runningSessionRepository.findByIdAndDeletedAtIsNull(84L)).thenReturn(Optional.of(runningSession));
        when(runningGpsSampleRepository.findByRunningSessionIdOrderBySampledAtAsc(84L))
                .thenReturn(List.of(firstGpsSample, secondGpsSample));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> persistCourse(invocation.getArgument(0), 503L));

        RegisterRunningSessionCourseResponse response =
                runningSessionService.registerRunningSessionCourse(84L, 7L, request);

        assertThat(response.status()).isEqualTo(CourseStatus.PRIVATE);
        assertThat(response.coordinates()).isEqualTo(expectedCoordinates);

        ArgumentCaptor<Course> courseCaptor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(courseCaptor.capture());
        verify(runningGpsSampleRepository).findByRunningSessionIdOrderBySampledAtAsc(84L);
        assertThat(courseCaptor.getValue().getStatus()).isEqualTo(CourseStatus.PRIVATE);
        assertThat(courseCaptor.getValue().getCoordinates()).isEqualTo(expectedCoordinates);
        verify(navigationBundleGenerationService).generate(503L);
    }

    @DisplayName("코스 등록 후 번들 생성에서 코스를 찾지 못하면 COURSE_NOT_FOUND 를 반환한다")
    @Test
    void mapsCourseNotFoundWhenNavigationBundleGenerationFailsDuringRegisterCourse() {
        RunningSession runningSession = runningSession(86L, 7L, true, STARTED_AT);
        setField(runningSession, "coordinates", new Coordinates(
                List.of(37.566001),
                List.of(126.977001),
                List.of(0.0)
        ));
        RunningGpsSample gpsSample = gpsSample(
                runningSession,
                7L,
                LocalDateTime.of(2026, 3, 12, 6, 30, 25),
                37.566501,
                126.978001,
                13.1,
                2.7,
                179.0,
                null,
                null,
                null,
                null
        );
        RegisterRunningSessionCourseRequest request = new RegisterRunningSessionCourseRequest(
                "한강 아침 러닝",
                "COMMUNITY",
                "EASY",
                List.of("RIVERSIDE"),
                RouteType.LOOP,
                null
        );
        stubUser(user(7L));
        when(runningSessionRepository.findByIdAndDeletedAtIsNull(86L)).thenReturn(Optional.of(runningSession));
        when(runningGpsSampleRepository.findByRunningSessionIdOrderBySampledAtAsc(86L)).thenReturn(List.of(gpsSample));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> persistCourse(invocation.getArgument(0), 504L));
        when(navigationBundleGenerationService.generate(504L)).thenThrow(
                new NavigationBundleGenerationException(NavigationBundleFailureCode.COURSE_NOT_FOUND, "missing course")
        );

        assertThatThrownBy(() -> runningSessionService.registerRunningSessionCourse(86L, 7L, request))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.COURSE_NOT_FOUND);

        verify(courseDifficultyRepository, never()).save(any(CourseDifficulty.class));
        verify(courseTypeMapRepository, never()).saveAll(any());
        verify(courseSignalService, never()).upsertCourseFeature(any(Course.class), any(Difficulty.class), any());
    }

    @DisplayName("코스 등록 후 번들 생성의 일반 실패는 NAVIGATION_BUNDLE_FAILED 를 반환한다")
    @Test
    void mapsNavigationBundleFailedWhenNavigationBundleGenerationFailsDuringRegisterCourse() {
        RunningSession runningSession = runningSession(87L, 7L, true, STARTED_AT);
        setField(runningSession, "coordinates", new Coordinates(
                List.of(37.566001),
                List.of(126.977001),
                List.of(0.0)
        ));
        RunningGpsSample gpsSample = gpsSample(
                runningSession,
                7L,
                LocalDateTime.of(2026, 3, 12, 6, 30, 25),
                37.566501,
                126.978001,
                13.1,
                2.7,
                179.0,
                null,
                null,
                null,
                null
        );
        RegisterRunningSessionCourseRequest request = new RegisterRunningSessionCourseRequest(
                "한강 아침 러닝",
                "COMMUNITY",
                "EASY",
                List.of("RIVERSIDE"),
                RouteType.LOOP,
                null
        );
        stubUser(user(7L));
        when(runningSessionRepository.findByIdAndDeletedAtIsNull(87L)).thenReturn(Optional.of(runningSession));
        when(runningGpsSampleRepository.findByRunningSessionIdOrderBySampledAtAsc(87L)).thenReturn(List.of(gpsSample));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> persistCourse(invocation.getArgument(0), 505L));
        when(navigationBundleGenerationService.generate(505L)).thenThrow(
                new NavigationBundleGenerationException(NavigationBundleFailureCode.BUNDLE_STORAGE_FAILED, "storage failed")
        );

        assertThatThrownBy(() -> runningSessionService.registerRunningSessionCourse(87L, 7L, request))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.NAVIGATION_BUNDLE_FAILED);

        verify(courseDifficultyRepository, never()).save(any(CourseDifficulty.class));
        verify(courseTypeMapRepository, never()).saveAll(any());
        verify(courseSignalService, never()).upsertCourseFeature(any(Course.class), any(Difficulty.class), any());
    }

    @DisplayName("지원하지 않는 코스 모드는 등록할 수 없다")
    @Test
    void rejectsUnsupportedCourseModeWhenRegisteringRunningSessionCourse() {
        RunningSession runningSession = runningSession(85L, 7L, true, STARTED_AT);
        RegisterRunningSessionCourseRequest request = new RegisterRunningSessionCourseRequest(
                "잘못된 모드 코스",
                "OFFICIAL",
                "MEDIUM",
                List.of("RIVERSIDE"),
                RouteType.LOOP,
                null
        );
        stubUser(user(7L));
        when(runningSessionRepository.findByIdAndDeletedAtIsNull(85L)).thenReturn(Optional.of(runningSession));

        assertThatThrownBy(() -> runningSessionService.registerRunningSessionCourse(85L, 7L, request))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.INVALID_INPUT_VALUE);

        verify(courseRepository, never()).save(any(Course.class));
    }

    @DisplayName("요청 이미지 URL이 없으면 러닝 세션 스냅샷을 사용한다")
    @Test
    void fallsBackToRunningSessionSnapshotWhenRequestSnapshotIsMissing() {
        RunningSession runningSession = runningSession(82L, 7L, true, STARTED_AT);
        setField(runningSession, "snapshotImageUrl", "snapshots/82.png");
        setField(runningSession, "coordinates", new Coordinates(
                List.of(37.566001),
                List.of(126.977001),
                List.of(0.0)
        ));
        RunningGpsSample firstGpsSample = gpsSample(
                runningSession,
                7L,
                LocalDateTime.of(2026, 3, 12, 6, 30, 25),
                37.566501,
                126.978001,
                13.1,
                2.7,
                179.0,
                null,
                null,
                null,
                null
        );
        RunningGpsSample secondGpsSample = gpsSample(
                runningSession,
                7L,
                LocalDateTime.of(2026, 3, 12, 6, 30, 35),
                37.567101,
                126.978601,
                13.9,
                2.5,
                182.0,
                null,
                null,
                null,
                null
        );
        Coordinates expectedCoordinates = new Coordinates(List.of(
                new GeoPoint(37.566501, 126.978001, 13.1),
                new GeoPoint(37.567101, 126.978601, 13.9)
        ));
        RegisterRunningSessionCourseRequest request = new RegisterRunningSessionCourseRequest(
                "한강 아침 러닝",
                "COMMUNITY",
                "EASY",
                List.of("RIVERSIDE"),
                RouteType.LOOP,
                null
        );
        stubUser(user(7L));
        when(runningSessionRepository.findByIdAndDeletedAtIsNull(82L)).thenReturn(Optional.of(runningSession));
        when(runningGpsSampleRepository.findByRunningSessionIdOrderBySampledAtAsc(82L))
                .thenReturn(List.of(firstGpsSample, secondGpsSample));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> persistCourse(invocation.getArgument(0), 502L));

        RegisterRunningSessionCourseResponse response =
                runningSessionService.registerRunningSessionCourse(82L, 7L, request);

        assertThat(response.snapshotImageUrl()).isEqualTo("signed::snapshots/82.png");
        assertThat(response.coordinates()).isEqualTo(expectedCoordinates);
        verify(runningGpsSampleRepository).findByRunningSessionIdOrderBySampledAtAsc(82L);
    }

    @DisplayName("완료되지 않은 세션은 코스 등록을 거부한다")
    @Test
    void rejectsRegisterCourseWhenRunningSessionIsNotCompleted() {
        RunningSession runningSession = runningSession(83L, 7L, false, STARTED_AT);
        RegisterRunningSessionCourseRequest request = new RegisterRunningSessionCourseRequest(
                "등록 실패 코스",
                "COMMUNITY",
                "MEDIUM",
                List.of("RIVERSIDE"),
                RouteType.LOOP,
                null
        );
        stubUser(user(7L));
        when(runningSessionRepository.findByIdAndDeletedAtIsNull(83L)).thenReturn(Optional.of(runningSession));

        assertThatThrownBy(() -> runningSessionService.registerRunningSessionCourse(83L, 7L, request))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.RUNNING_SESSION_NOT_COMPLETED);

        verify(courseRepository, never()).save(any(Course.class));
        verify(courseDifficultyRepository, never()).save(any(CourseDifficulty.class));
        verify(courseTypeMapRepository, never()).saveAll(any());
    }

    @DisplayName("다른 사용자의 세션은 코스로 등록할 수 없다")
    @Test
    void rejectsRegisterCourseWhenRunningSessionBelongsToAnotherUser() {
        RunningSession runningSession = runningSession(88L, 99L, true, STARTED_AT);
        RegisterRunningSessionCourseRequest request = new RegisterRunningSessionCourseRequest(
                "권한 없는 코스 등록",
                "COMMUNITY",
                "MEDIUM",
                List.of("RIVERSIDE"),
                RouteType.LOOP,
                null
        );
        when(runningSessionRepository.findByIdAndDeletedAtIsNull(88L)).thenReturn(Optional.of(runningSession));
        when(userRepository.findNotDeletedUser(7L)).thenReturn(Optional.of(user(7L)));

        assertThatThrownBy(() -> runningSessionService.registerRunningSessionCourse(88L, 7L, request))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.RUNNING_SESSION_NOT_FOUND);

        verify(courseRepository, never()).save(any(Course.class));
    }

    @DisplayName("과거 러닝 세션 페이지를 커서와 시간대로 조립한다")
    @Test
    void returnsPastRunningSessionsWithCursorAndTimeSlot() {
        User user = user(7L);
        when(userRepository.findNotDeletedUser(user.getId())).thenReturn(Optional.of(user));
        when(runningSessionRepository.findPastRunningSessionHistoryRows(
                user.getId(),
                null,
                null,
                null,
                null,
                1
        )).thenReturn(List.of(
                new PastRunningSessionHistoryRow(
                        121L,
                        LocalDateTime.of(2026, 3, 16, 18, 30),
                        7000,
                        2100,
                        420,
                        "snapshots/121.png",
                        40,
                        RunningSessionCompleteState.SUCCESS,
                        RunningMode.GHOST,
                        GhostResultStatus.WIN
                ),
                new PastRunningSessionHistoryRow(
                        120L,
                        LocalDateTime.of(2026, 3, 15, 5, 45),
                        5000,
                        null,
                        310,
                        null,
                        15,
                        null,
                        RunningMode.FREE,
                        null
                )
        ));

        PastRunningSessionsResponse response = runningSessionService.findPastRunningSessions(
                user.getId(),
                new PastRunningSessionsRequest(null, null, null, 1, null)
        );

        assertThat(response.items()).containsExactly(
                new PastRunningSessionItemResponse(
                        121L,
                        LocalDateTime.of(2026, 3, 16, 18, 30),
                        7000,
                        2100,
                        420,
                        "signed::snapshots/121.png",
                        40,
                        RunningSessionCompleteState.SUCCESS,
                        RunningMode.GHOST,
                        GhostResultStatus.WIN,
                        TimeSlot.EVENING
                )
        );
        assertThat(response.hasMore()).isTrue();
        assertThat(response.nextCursor()).isNotBlank();
    }

    @DisplayName("서비스는 별도 객체가 계산한 날짜 범위를 리포지토리에 전달한다")
    @Test
    void passesExtractedDateRangeToRepository() {
        User user = user(7L);
        PastRunningSessionsRequest request = new PastRunningSessionsRequest(2026, 3, 16, 10, null);
        RunningSessionDateRange range = RunningSessionDateRange.of(request.year(), request.month(), request.day());
        when(userRepository.findNotDeletedUser(user.getId())).thenReturn(Optional.of(user));
        when(runningSessionRepository.findPastRunningSessionHistoryRows(
                user.getId(),
                range.startInclusive(),
                range.endExclusive(),
                null,
                null,
                10
        )).thenReturn(List.of());

        PastRunningSessionsResponse response = runningSessionService.findPastRunningSessions(user.getId(), request);

        assertThat(response.items()).isEmpty();
        assertThat(response.hasMore()).isFalse();
        assertThat(response.nextCursor()).isNull();
    }

    private void stubUser(User user) {
        when(userRepository.findNotDeletedUser(user.getId())).thenReturn(Optional.of(user));
    }

    private RunningSession persist(RunningSession runningSession, Long id) {
        setField(runningSession, "id", id);
        return runningSession;
    }

    private Course persistCourse(Course course, Long id) {
        setField(course, "id", id);
        return course;
    }

    private Course course(Long id, CourseStatus status) {
        return course(id, status, null);
    }

    private Course course(Long id, CourseStatus status, User owner) {
        Course course = Course.builder()
                .title("Course " + id)
                .status(status)
                .distanceM(5000)
                .elevationGainM(120)
                .snapshotImageUrl("snapshot-" + id)
                .startLatitude(37.5665)
                .startLongitude(126.9780)
                .endLatitude(37.5700)
                .endLongitude(126.9820)
                .coordinates(new Coordinates(List.of(37.5665), List.of(126.9780), List.of(12.0)))
                .user(owner)
                .build();
        setField(course, "id", id);
        return course;
    }

    private CourseGhostLeaderboard leaderboard(Long id, User user, Course course, RunningSession runningSession, Integer point) {
        CourseGhostLeaderboard leaderboard = CourseGhostLeaderboard.create(user, course, runningSession, point);
        setField(leaderboard, "id", id);
        return leaderboard;
    }

    private User user(Long id) {
        User user = instantiate(User.class);
        setField(user, "id", id);
        setField(user, "nickname", "runner-" + id);
        setField(user, "birthDate", LocalDate.of(1995, 3, 11));
        setField(user, "gender", Gender.MALE);
        setField(user, "height", 175.0);
        setField(user, "weight", 68.0);
        return user;
    }

    private RunningSession completedRunningSession(Long id) {
        return runningSession(id, true);
    }

    private RunningSession runningSession(Long id, boolean completedSuccessfully) {
        RunningSession runningSession = instantiate(RunningSession.class);
        setField(runningSession, "id", id);
        setField(
                runningSession,
                "completeState",
                completedSuccessfully ? RunningSessionCompleteState.SUCCESS : RunningSessionCompleteState.FAIL
        );
        if (completedSuccessfully) {
            setField(runningSession, "endedAt", STARTED_AT.plusMinutes(30));
        }
        return runningSession;
    }

    private RunningSession runningSession(Long id, Long userId, boolean completedSuccessfully, LocalDateTime startedAt) {
        RunningSession runningSession = instantiate(RunningSession.class);
        setField(runningSession, "id", id);
        setField(runningSession, "startedAt", startedAt);
        setField(runningSession, "distanceM", 0);
        setField(runningSession, "caloriesKcal", 0);
        setField(runningSession, "elevationGainM", 0);
        setField(runningSession, "startLatitude", 37.5);
        setField(runningSession, "startLongitude", 127.0);
        setField(runningSession, "isPublic", false);
        setField(
                runningSession,
                "completeState",
                completedSuccessfully ? RunningSessionCompleteState.SUCCESS : RunningSessionCompleteState.FAIL
        );
        if (completedSuccessfully) {
            setField(runningSession, "endedAt", startedAt.plusMinutes(30));
        }
        setField(runningSession, "user", user(userId));
        setField(runningSession, "mode", RunningMode.FREE);
        return runningSession;
    }

    private CompleteRunningSessionRequest validCompleteRequest() {
        return completeRequestWithState(RunningSessionCompleteState.SUCCESS);
    }

    private CompleteRunningSessionRequest completeRequestWithState(RunningSessionCompleteState completeState) {
        return new CompleteRunningSessionRequest(
                completeState,
                5320,
                328,
                37.5701,
                126.9812,
                2.92,
                1925,
                362,
                46,
                0,
                List.of(new CreateRunningGpsSampleRequest(
                        37.5665,
                        126.9780,
                        21.5,
                        5.2,
                        182.0,
                        3.45,
                        289,
                        1250,
                        (short) 174,
                        LocalDateTime.of(2026, 3, 18, 6, 31, 30)
                )),
                List.of(new CreateRunningHealthSampleRequest(
                        LocalDateTime.of(2026, 3, 18, 6, 31, 30),
                        (short) 152,
                        84.5
                )),
                List.of(new CreateRunningSessionSplitRequest(
                        1,
                        1000,
                        320,
                        320,
                        152,
                        12
                ))
        );
    }

    private List<String> recordComponentNames(Class<?> type) {
        return Arrays.stream(type.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName)
                .toList();
    }

    private Object invokeAccessor(Object target, String accessorName) {
        try {
            return target.getClass().getMethod(accessorName).invoke(target);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to invoke accessor " + accessorName, exception);
        }
    }

    private RunningGpsSample gpsSample(
            RunningSession runningSession,
            Long userId,
            LocalDateTime sampledAt,
            Double latitude,
            Double longitude,
            Double altitudeM,
            Double accuracyM,
            Double bearingDeg,
            Double speedMps,
            Integer paceSecPerKm,
            Integer distanceM,
            Short cadenceSpm
    ) {
        return RunningGpsSample.create(
                user(userId),
                runningSession,
                bearingDeg,
                accuracyM,
                altitudeM,
                longitude,
                latitude,
                speedMps,
                paceSecPerKm,
                distanceM,
                cadenceSpm,
                sampledAt
        );
    }

    private <T> T instantiate(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to instantiate " + type.getSimpleName(), exception);
        }
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
