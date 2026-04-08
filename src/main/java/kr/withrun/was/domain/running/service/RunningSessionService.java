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
import kr.withrun.was.domain.running.dto.GhostRunningResultResponse;
import kr.withrun.was.domain.running.dto.PastRunningSessionItemResponse;
import kr.withrun.was.domain.running.dto.PastRunningSessionsRequest;
import kr.withrun.was.domain.running.dto.PastRunningSessionsResponse;
import kr.withrun.was.domain.running.dto.RegisterRunningSessionCourseRequest;
import kr.withrun.was.domain.running.dto.RegisterRunningSessionCourseResponse;
import kr.withrun.was.domain.running.dto.RunningGpsSampleResponse;
import kr.withrun.was.domain.running.dto.RunningHealthSampleResponse;
import kr.withrun.was.domain.running.dto.RunningSessionDetailResponse;
import kr.withrun.was.domain.running.dto.RunningSessionSplitResponse;
import kr.withrun.was.domain.running.entity.GhostRunningResult;
import kr.withrun.was.domain.running.entity.RunningGpsSample;
import kr.withrun.was.domain.running.entity.RunningHealthSample;
import kr.withrun.was.domain.running.entity.RunningSession;
import kr.withrun.was.domain.running.entity.RunningSessionSplit;
import kr.withrun.was.domain.running.repository.GhostRunningResultRepository;
import kr.withrun.was.domain.running.repository.RunningGpsSampleRepository;
import kr.withrun.was.domain.running.repository.RunningHealthSampleRepository;
import kr.withrun.was.domain.running.repository.RunningSessionRepository;
import kr.withrun.was.domain.running.repository.RunningSessionSplitRepository;
import kr.withrun.was.domain.running.repository.query.dto.PastRunningSessionHistoryRow;
import kr.withrun.was.domain.running.type.GhostResultStatus;
import kr.withrun.was.domain.running.type.RunningMode;
import kr.withrun.was.domain.running.type.RunningSessionCompleteState;
import kr.withrun.was.domain.running.util.PastRunningSessionCursorCodec;
import kr.withrun.was.domain.running.vo.RunningSessionDateRange;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.entity.UserCalendar;
import kr.withrun.was.domain.user.repository.UserCalendarRepository;
import kr.withrun.was.domain.user.repository.UserRepository;
import kr.withrun.was.global.common.type.Difficulty;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RunningSessionService {

    private final RunningSessionRepository runningSessionRepository;
    private final UserRepository userRepository;
    private final UserCalendarRepository userCalendarRepository;
    private final CourseRepository courseRepository;
    private final CourseDifficultyRepository courseDifficultyRepository;
    private final CourseGhostLeaderboardRepository courseGhostLeaderboardRepository;
    private final CourseTypeMapRepository courseTypeMapRepository;
    private final RunningGpsSampleRepository runningGpsSampleRepository;
    private final RunningHealthSampleRepository runningHealthSampleRepository;
    private final RunningSessionSplitRepository runningSessionSplitRepository;
    private final GhostRunningResultRepository ghostRunningResultRepository;
    private final GhostRunningResultService ghostRunningResultService;
    private final RewardPointGrantService rewardPointGrantService;
    private final CourseSignalService courseSignalService;
    private final CloudFrontSignedUrlService cloudFrontSignedUrlService;
    private final NavigationBundleGenerationService navigationBundleGenerationService;
    private final CourseAccessPolicy courseAccessPolicy;

    @Value("${app.course-duplicate.endpoint-threshold-m:150}")
    private int duplicateCourseEndpointThresholdM = 150;

    @Value("${app.course-duplicate.distance-diff-ratio:0.12}")
    private double duplicateCourseDistanceDiffRatio = 0.12d;

    @Value("${app.course-duplicate.phase2-enabled:false}")
    private boolean duplicateCoursePhase2Enabled = false;

    @Value("${app.course-duplicate.shape-tolerance-m:${app.course-duplicate.shape-threshold-m:45}}")
    private double duplicateCourseShapeToleranceM = 45d;

    @Value("${app.course-duplicate.shape-min-overlap-ratio:0.93}")
    private double duplicateCourseShapeMinOverlapRatio = 0.93d;

    @Value("${app.course-duplicate.shape-segmentize-step-m:15}")
    private double duplicateCourseShapeSegmentizeStepM = 15d;

    @Transactional
    public CreateRunningSessionResponse createRunningSession(Long currentUserId, CreateRunningSessionRequest request) {
        RunningMode mode = request.mode();
        User user = getUser(currentUserId);
        Course course = getCourse(user, mode, request.courseId());
        RunningSession ghostTargetRunningSession = resolveGhostTargetRunningSession(
                user,
                mode,
                course,
                request.ghostTargetRunningSessionId()
        );

        RunningSession savedRunningSession = runningSessionRepository.save(RunningSession.start(
                user,
                mode,
                course,
                ghostTargetRunningSession,
                request.startLatitude(),
                request.startLongitude()
        ));

        List<RunningGpsSampleResponse> ghostTargetGpsSamples = getGhostTargetGpsSamples(mode, ghostTargetRunningSession);

        return CreateRunningSessionResponse.from(
                savedRunningSession,
                ghostTargetGpsSamples,
                course == null
                        ? null
                        : cloudFrontSignedUrlService.generateSignedUrl(course.getNavigationBundleUrl())
        );
    }

    @Transactional
    public RunningSessionDetailResponse completeRunningSession(
            Long runningSessionId,
            Long currentUserId,
            CompleteRunningSessionRequest request
    ) {
        RunningSession runningSession = getRunningSession(runningSessionId);
        User user = getUser(currentUserId);
        runningSession.validateOwner(user.getId());
        runningSession.validateCompletable();

        runningSession.complete(
                request.completeState(),
                request.distanceM(),
                request.caloriesKcal(),
                request.endLatitude(),
                request.endLongitude(),
                request.avgSpeedMps(),
                request.durationSec(),
                request.avgPaceSecPerKm(),
                request.elevationGainM()
        );
        recordCourseCompletionIfEligible(runningSession, user);
        UserCalendar userCalendar = upsertUserCalendar(runningSession);

        List<RunningGpsSample> savedGpsSamples = saveGpsSamples(runningSession, user, request.gpsSamples());
        List<RunningHealthSample> savedHealthSamples = saveHealthSamples(runningSession, user, request.healthSamples());
        List<RunningSessionSplit> savedSplits = saveSplits(runningSession, request.splits());

        GhostRunningResult ghostRunningResult = createGhostRunningResultIfNeeded(runningSession, request.distanceGapM());
        upsertCourseGhostLeaderboardIfNeeded(runningSession);
        rewardPointGrantService.grantCompletedRunningRewards(runningSession, userCalendar, ghostRunningResult);

        List<RunningGpsSampleResponse> gpsResponses = RunningGpsSampleResponse.from(savedGpsSamples);
        List<RunningHealthSampleResponse> healthResponses = savedHealthSamples.stream()
                .map(RunningHealthSampleResponse::from)
                .toList();
        List<RunningSessionSplitResponse> splitResponses = savedSplits.stream()
                .map(RunningSessionSplitResponse::from)
                .toList();

        GhostRunningResultResponse ghostRunningResultResponse = ghostRunningResult == null
                ? null
                : toGhostRunningResultResponse(ghostRunningResult);

        return RunningSessionDetailResponse.of(
                runningSession,
                gpsResponses,
                healthResponses,
                splitResponses,
                cloudFrontSignedUrlService.generateSignedUrl(runningSession.getSnapshotImageUrl()),
                ghostRunningResultResponse
        );
    }

    @Transactional
    public RegisterRunningSessionCourseResponse registerRunningSessionCourse(
            Long runningSessionId,
            Long currentUserId,
            RegisterRunningSessionCourseRequest request
    ) {
        RunningSession runningSession = getRunningSession(runningSessionId);
        User user = getUser(currentUserId);
        runningSession.validateOwner(user.getId());
        runningSession.validateCourseRegistrable();
        CourseStatus courseStatus = parseCourseStatus(request.mode());
        Difficulty difficulty = parseDifficulty(request.difficulty());
        List<CourseType> courseTypes = parseCourseTypes(request.courseTypes());
        Coordinates courseCoordinates = duplicateCoursePhase2Enabled
                ? buildCourseCoordinates(runningSessionId)
                : null;
        validateNoPublicDuplicateCourse(runningSession, courseCoordinates);
        if (courseCoordinates == null) {
            courseCoordinates = buildCourseCoordinates(runningSessionId);
        }

        Course course = courseRepository.save(Course.builder()
                .title(request.title())
                .status(courseStatus)
                .distanceM(runningSession.getDistanceM())
                .elevationGainM(runningSession.getElevationGainM())
                .snapshotImageUrl(resolveSnapshotImageUrl(request.snapshotImageUrl(), runningSession.getSnapshotImageUrl()))
                .startLatitude(runningSession.getStartLatitude())
                .startLongitude(runningSession.getStartLongitude())
                .endLatitude(runningSession.getEndLatitude())
                .endLongitude(runningSession.getEndLongitude())
                .coordinates(courseCoordinates)
                .routeType(request.routeType())
                .user(runningSession.getUser())
                .build());
        generateNavigationBundle(course.getId());
        courseDifficultyRepository.save(CourseDifficulty.create(course, difficulty));
        courseTypeMapRepository.saveAll(courseTypes.stream()
                .map(courseType -> CourseTypeMap.create(course, courseType))
                .toList());
        courseSignalService.upsertCourseFeature(course, difficulty, courseTypes);

        return RegisterRunningSessionCourseResponse.from(
                course,
                difficulty,
                cloudFrontSignedUrlService.generateSignedUrl(course.getSnapshotImageUrl())
        );
    }

    private void generateNavigationBundle(Long courseId) {
        try {
            navigationBundleGenerationService.generate(courseId);
        } catch (NavigationBundleGenerationException exception) {
            if (exception.getFailureCode() == NavigationBundleFailureCode.COURSE_NOT_FOUND) {
                throw new CustomException(ResponseCode.COURSE_NOT_FOUND);
            }
            throw new CustomException(ResponseCode.NAVIGATION_BUNDLE_FAILED);
        }
    }

    public RunningSessionDetailResponse findRunningSessionDetail(Long runningSessionId, Long currentUserId) {
        RunningSession runningSession = getRunningSession(runningSessionId);
        runningSession.validateOwner(currentUserId);

        List<RunningGpsSampleResponse> gpsSamples = RunningGpsSampleResponse.from(
                runningGpsSampleRepository.findByRunningSessionIdOrderBySampledAtAsc(runningSessionId)
        );
        List<RunningHealthSampleResponse> healthSamples = runningHealthSampleRepository.findByRunningSessionIdOrderBySampledAtAsc(runningSessionId)
                .stream()
                .map(RunningHealthSampleResponse::from)
                .toList();
        List<RunningSessionSplitResponse> splits = runningSessionSplitRepository.findByRunningSessionIdOrderBySplitIndexAsc(runningSessionId)
                .stream()
                .map(RunningSessionSplitResponse::from)
                .toList();

        GhostRunningResultResponse ghostRunningResult = ghostRunningResultRepository.findByRunningSessionIdAndDeletedAtIsNull(runningSessionId)
                .map(this::toGhostRunningResultResponse)
                .orElse(null);

        return RunningSessionDetailResponse.of(
                runningSession,
                gpsSamples,
                healthSamples,
                splits,
                cloudFrontSignedUrlService.generateSignedUrl(runningSession.getSnapshotImageUrl()),
                ghostRunningResult
        );
    }

    public PastRunningSessionsResponse findPastRunningSessions(Long currentUserId, PastRunningSessionsRequest request) {
        PastRunningSessionCursorCodec.CursorPayload payload = request.cursor() == null
                ? null
                : PastRunningSessionCursorCodec.decode(request.cursor());

        RunningSessionDateRange dateRange = RunningSessionDateRange.of(request.year(), request.month(), request.day());

        getUser(currentUserId);

        List<PastRunningSessionHistoryRow> rows = runningSessionRepository.findPastRunningSessionHistoryRows(
                currentUserId,
                dateRange.startInclusive(),
                dateRange.endExclusive(),
                payload == null ? null : payload.startedAt(),
                payload == null ? null : payload.runningSessionId(),
                request.pageSize()
        );

        boolean hasMore = rows.size() > request.pageSize();
        List<PastRunningSessionHistoryRow> pagedRows = hasMore
                ? List.copyOf(rows.subList(0, request.pageSize()))
                : List.copyOf(rows);
        List<PastRunningSessionItemResponse> items = pagedRows.stream()
                .map(row -> PastRunningSessionItemResponse.from(
                        row,
                        cloudFrontSignedUrlService.generateSignedUrl(row.snapshotImageUrl())
                ))
                .toList();
        String nextCursor = hasMore ? toCursor(pagedRows.getLast()) : null;

        return new PastRunningSessionsResponse(items, hasMore, nextCursor);
    }

    private GhostRunningResult createGhostRunningResultIfNeeded(RunningSession runningSession, Integer requestedDistanceGapM) {
        if (runningSession.getMode() != RunningMode.GHOST) {
            return null;
        }

        RunningSession targetSession = runningSession.getGhostTargetRunningSession();
        if (targetSession == null) {
            return null;
        }

        if (targetSession.getCompleteState() != RunningSessionCompleteState.SUCCESS) {
            throw new CustomException(ResponseCode.INVALID_GHOST_TARGET);
        }

        if (runningSession.getDurationSec() == null || targetSession.getDurationSec() == null) {
            throw new CustomException(ResponseCode.INVALID_INPUT_VALUE);
        }

        if (requestedDistanceGapM == null) {
            throw new CustomException(ResponseCode.INVALID_INPUT_VALUE);
        }

        int timeGapSec = Math.abs(runningSession.getDurationSec() - targetSession.getDurationSec());
        int distanceGapM = requestedDistanceGapM;

        if (runningSession.getCompleteState() != RunningSessionCompleteState.SUCCESS) {
            GhostRunningResult ghostRunningResult = GhostRunningResult.create(
                    GhostResultStatus.LOSE,
                    0,
                    timeGapSec,
                    distanceGapM,
                    runningSession,
                    targetSession,
                    targetSession.getUser()
            );

            return ghostRunningResultRepository.save(ghostRunningResult);
        }

        Difficulty difficulty = resolveGhostDifficulty(runningSession, targetSession);
        int point = ghostRunningResultService.calculateGhostRunningPoint(runningSession.getDurationSec(), difficulty);
        int targetPoint = ghostRunningResultService.calculateGhostRunningPoint(targetSession.getDurationSec(), difficulty);

        GhostResultStatus resultStatus = resolveGhostResultStatus(point, targetPoint);

        GhostRunningResult ghostRunningResult = GhostRunningResult.create(
                resultStatus,
                point,
                timeGapSec,
                distanceGapM,
                runningSession,
                targetSession,
                targetSession.getUser()
        );

        GhostRunningResult savedGhostRunningResult = ghostRunningResultRepository.save(ghostRunningResult);
        return savedGhostRunningResult;
    }

    private void upsertCourseGhostLeaderboardIfNeeded(RunningSession runningSession) {
        if (runningSession.getMode() != RunningMode.GHOST) {
            return;
        }

        if (runningSession.getCompleteState() != RunningSessionCompleteState.SUCCESS) {
            return;
        }

        Difficulty difficulty = resolveGhostDifficulty(runningSession, runningSession.getGhostTargetRunningSession());
        int point = ghostRunningResultService.calculateGhostRunningPoint(runningSession.getDurationSec(), difficulty);
        upsertCourseGhostLeaderboard(runningSession, point);
    }

    private UserCalendar upsertUserCalendar(RunningSession runningSession) {
        LocalDate calendarDate = runningSession.getStartedAt().toLocalDate();
        UserCalendar userCalendar = userCalendarRepository.findByUserIdAndCalendarDateAndDeletedAtIsNull(
                        runningSession.getUser().getId(),
                        calendarDate
                )
                .orElseGet(() -> UserCalendar.create(runningSession.getUser(), calendarDate));

        userCalendar.recordCompletedRun(
                runningSession.getMode(),
                runningSession.getDistanceM(),
                runningSession.getDurationSec(),
                runningSession.getCaloriesKcal(),
                runningSession.getSnapshotImageUrl()
        );
        userCalendarRepository.save(userCalendar);
        return userCalendar;
    }

    private Difficulty resolveGhostDifficulty(RunningSession runningSession, RunningSession targetSession) {
        Long courseId = runningSession.getCourse() != null
                ? runningSession.getCourse().getId()
                : targetSession == null || targetSession.getCourse() == null ? null : targetSession.getCourse().getId();

        if (courseId == null) {
            throw new CustomException(ResponseCode.COURSE_NOT_FOUND);
        }

        return courseDifficultyRepository.findDifficultyByCourseId(courseId)
                .orElseThrow(() -> new CustomException(ResponseCode.COURSE_DIFFICULTY_NOT_FOUND));
    }

    private GhostResultStatus resolveGhostResultStatus(int point, int targetPoint) {
        if (point > targetPoint) {
            return GhostResultStatus.WIN;
        }
        if (point < targetPoint) {
            return GhostResultStatus.LOSE;
        }
        return GhostResultStatus.DRAW;
    }

    private void upsertCourseGhostLeaderboard(RunningSession runningSession, int point) {
        Course leaderboardCourse = resolveLeaderboardCourse(runningSession);
        CourseGhostLeaderboard existingRow = courseGhostLeaderboardRepository
                .findTopByUserIdAndCourseId(
                        runningSession.getUser().getId(),
                        leaderboardCourse.getId()
                )
                .orElse(null);

        if (existingRow == null) {
            courseGhostLeaderboardRepository.save(CourseGhostLeaderboard.create(
                    runningSession.getUser(),
                    leaderboardCourse,
                    runningSession,
                    point
            ));
            return;
        }

        int existingPoint = existingRow.getPoint() == null ? 0 : existingRow.getPoint();
        if (point > existingPoint) {
            existingRow.updatePointAndRunningSession(point, runningSession);
        }
    }

    private Course resolveLeaderboardCourse(RunningSession runningSession) {
        if (runningSession.getCourse() != null) {
            return runningSession.getCourse();
        }

        RunningSession targetSession = runningSession.getGhostTargetRunningSession();
        if (targetSession != null && targetSession.getCourse() != null) {
            return targetSession.getCourse();
        }

        throw new CustomException(ResponseCode.COURSE_NOT_FOUND);
    }

    private List<RunningGpsSample> saveGpsSamples(
            RunningSession runningSession,
            User user,
            List<CreateRunningGpsSampleRequest> requests
    ) {
        List<RunningGpsSample> samples = requests.stream()
                .map(request -> RunningGpsSample.create(
                        user,
                        runningSession,
                        request.bearingDeg(),
                        request.accuracyM(),
                        request.altitudeM(),
                        request.longitude(),
                        request.latitude(),
                        request.speedMps(),
                        request.paceSecPerKm(),
                        request.distanceM(),
                        request.cadenceSpm(),
                        request.sampledAt()
                ))
                .toList();
        return runningGpsSampleRepository.saveAll(samples);
    }

    private List<RunningHealthSample> saveHealthSamples(
            RunningSession runningSession,
            User user,
            List<CreateRunningHealthSampleRequest> requests
    ) {
        List<RunningHealthSample> samples = requests.stream()
                .map(request -> RunningHealthSample.create(
                        user,
                        runningSession,
                        request.heartRate(),
                        request.caloriesKcal(),
                        request.sampledAt()
                ))
                .toList();
        return runningHealthSampleRepository.saveAll(samples);
    }

    private List<RunningSessionSplit> saveSplits(RunningSession runningSession, List<CreateRunningSessionSplitRequest> requests) {
        List<RunningSessionSplit> splits = requests.stream()
                .map(request -> RunningSessionSplit.create(
                        runningSession,
                        request.splitIndex(),
                        request.splitDistanceM(),
                        request.splitDurationSec(),
                        request.splitPaceSecPerKm(),
                        request.avgHeartRate(),
                        request.elevationGainM()
                ))
                .toList();
        return runningSessionSplitRepository.saveAll(splits);
    }

    private GhostRunningResultResponse toGhostRunningResultResponse(GhostRunningResult ghostRunningResult) {
        return new GhostRunningResultResponse(
                ghostRunningResult.getId(),
                ghostRunningResult.getRunningSession().getId(),
                ghostRunningResult.getGhostTargetRunningSession().getId(),
                ghostRunningResult.getTargetUser() == null ? null : ghostRunningResult.getTargetUser().getId(),
                ghostRunningResult.getResultStatus(),
                ghostRunningResult.getPoint(),
                ghostRunningResult.getTimeGapSec(),
                ghostRunningResult.getDistanceGapM(),
                ghostRunningResult.getCreatedAt()
        );
    }

    private User getUser(Long userId) {
        return userRepository.findNotDeletedUser(userId)
                .orElseThrow(() -> new CustomException(ResponseCode.USER_NOT_FOUND));
    }

    private List<RunningGpsSampleResponse> getGhostTargetGpsSamples(
            RunningMode mode,
            RunningSession ghostTargetRunningSession
    ) {
        if (mode != RunningMode.GHOST) {
            return null;
        }

        if (ghostTargetRunningSession == null) {
            return List.of();
        }

        return RunningGpsSampleResponse.from(
                runningGpsSampleRepository.findByRunningSessionIdOrderBySampledAtAsc(ghostTargetRunningSession.getId())
        );
    }

    private RunningSession resolveGhostTargetRunningSession(
            User user,
            RunningMode mode,
            Course course,
            Long ghostTargetRunningSessionId
    ) {
        RunningSession explicitGhostTarget = getGhostTargetRunningSession(mode, ghostTargetRunningSessionId);
        if (explicitGhostTarget != null || mode != RunningMode.GHOST) {
            return explicitGhostTarget;
        }

        return getLeaderboardGhostTargetRunningSession(user, course);
    }

    private RunningSession getLeaderboardGhostTargetRunningSession(User user, Course course) {
        if (user == null || course == null) {
            return null;
        }

        return courseGhostLeaderboardRepository.findTopByUserIdAndCourseId(user.getId(), course.getId())
                .map(CourseGhostLeaderboard::getRunningSession)
                .filter(runningSession -> !runningSession.isDeleted())
                .filter(runningSession -> runningSession.getCompleteState() == RunningSessionCompleteState.SUCCESS)
                .orElse(null);
    }

    private Course getCourse(User user, RunningMode mode, Long courseId) {
        if (mode == RunningMode.FREE) {
            return null;
        }

        if (courseId == null) {
            throw new CustomException(ResponseCode.INVALID_INPUT_VALUE);
        }

        return courseAccessPolicy.getAccessibleCourse(courseRepository, courseId, user == null ? null : user.getId());
    }

    private RunningSession getGhostTargetRunningSession(RunningMode mode, Long ghostTargetRunningSessionId) {
        if (mode != RunningMode.GHOST) {
            return null;
        }

        if (ghostTargetRunningSessionId == null) {
            return null;
        }

        RunningSession ghostTargetRunningSession = runningSessionRepository.findByIdAndDeletedAtIsNull(ghostTargetRunningSessionId)
                .orElseThrow(() -> new CustomException(ResponseCode.GHOST_TARGET_NOT_FOUND));

        if (ghostTargetRunningSession.getCompleteState() != RunningSessionCompleteState.SUCCESS) {
            throw new CustomException(ResponseCode.INVALID_GHOST_TARGET);
        }

        return ghostTargetRunningSession;
    }

    private RunningSession getRunningSession(Long runningSessionId) {
        return runningSessionRepository.findByIdAndDeletedAtIsNull(runningSessionId)
                .orElseThrow(() -> new CustomException(ResponseCode.RUNNING_SESSION_NOT_FOUND));
    }

    private Difficulty parseDifficulty(String rawDifficulty) {
        try {
            return Difficulty.valueOf(rawDifficulty);
        } catch (IllegalArgumentException exception) {
            throw new CustomException(ResponseCode.INVALID_INPUT_VALUE);
        }
    }

    private CourseStatus parseCourseStatus(String rawCourseStatus) {
        try {
            CourseStatus courseStatus = CourseStatus.valueOf(rawCourseStatus);
            if (courseStatus == CourseStatus.COMMUNITY || courseStatus == CourseStatus.PRIVATE) {
                return courseStatus;
            }
        } catch (IllegalArgumentException exception) {
            throw new CustomException(ResponseCode.INVALID_INPUT_VALUE);
        }

        throw new CustomException(ResponseCode.INVALID_INPUT_VALUE);
    }

    private List<CourseType> parseCourseTypes(List<String> rawCourseTypes) {
        LinkedHashSet<CourseType> courseTypes = new LinkedHashSet<>();
        for (String rawCourseType : rawCourseTypes) {
            courseTypes.add(parseCourseType(rawCourseType));
        }
        return List.copyOf(courseTypes);
    }

    private CourseType parseCourseType(String rawCourseType) {
        try {
            return CourseType.valueOf(rawCourseType);
        } catch (IllegalArgumentException exception) {
            throw new CustomException(ResponseCode.INVALID_INPUT_VALUE);
        }
    }

    private String resolveSnapshotImageUrl(String requestedSnapshotImageUrl, String runningSessionSnapshotImageUrl) {
        if (requestedSnapshotImageUrl == null || requestedSnapshotImageUrl.isBlank()) {
            return cloudFrontSignedUrlService.normalizeObjectKey(runningSessionSnapshotImageUrl);
        }
        return cloudFrontSignedUrlService.normalizeObjectKey(requestedSnapshotImageUrl);
    }

    private Coordinates buildCourseCoordinates(Long runningSessionId) {
        List<GeoPoint> points = runningGpsSampleRepository.findByRunningSessionIdOrderBySampledAtAsc(runningSessionId)
                .stream()
                .map(sample -> new GeoPoint(sample.getLatitude(), sample.getLongitude(), sample.getAltitudeM()))
                .toList();
        return new Coordinates(points);
    }

    private void validateNoPublicDuplicateCourse(RunningSession runningSession, Coordinates courseCoordinates) {
        if (runningSession.getStartLatitude() == null
                || runningSession.getStartLongitude() == null
                || runningSession.getEndLatitude() == null
                || runningSession.getEndLongitude() == null
                || runningSession.getDistanceM() == null) {
            return;
        }

        if (duplicateCoursePhase2Enabled) {
            String routeLineStringWkt = toRouteLineStringWkt(courseCoordinates);
            if (routeLineStringWkt != null) {
                boolean duplicateExistsByShape = courseRepository.existsPublicDuplicateCourseByRouteGeometry(
                        runningSession.getStartLatitude(),
                        runningSession.getStartLongitude(),
                        runningSession.getEndLatitude(),
                        runningSession.getEndLongitude(),
                        runningSession.getDistanceM(),
                        duplicateCourseEndpointThresholdM,
                        duplicateCourseDistanceDiffRatio,
                        routeLineStringWkt,
                        duplicateCourseShapeToleranceM,
                        duplicateCourseShapeMinOverlapRatio,
                        duplicateCourseShapeSegmentizeStepM
                );
                if (duplicateExistsByShape) {
                    throw new CustomException(ResponseCode.COURSE_ALREADY_EXISTS);
                }
                return;
            }
        }

        boolean duplicateExists = courseRepository.existsPublicDuplicateCourse(
                runningSession.getStartLatitude(),
                runningSession.getStartLongitude(),
                runningSession.getEndLatitude(),
                runningSession.getEndLongitude(),
                runningSession.getDistanceM(),
                duplicateCourseEndpointThresholdM,
                duplicateCourseDistanceDiffRatio
        );

        if (duplicateExists) {
            throw new CustomException(ResponseCode.COURSE_ALREADY_EXISTS);
        }
    }

    private String toRouteLineStringWkt(Coordinates coordinates) {
        if (coordinates == null || coordinates.values().isEmpty()) {
            return null;
        }

        StringBuilder lineStringBuilder = new StringBuilder("LINESTRING(");
        int validPointCount = 0;
        Double previousLatitude = null;
        Double previousLongitude = null;

        for (GeoPoint point : coordinates.values()) {
            if (point == null || point.latitude() == null || point.longitude() == null) {
                continue;
            }

            double latitude = point.latitude();
            double longitude = point.longitude();
            if (previousLatitude != null
                    && previousLongitude != null
                    && Double.compare(previousLatitude, latitude) == 0
                    && Double.compare(previousLongitude, longitude) == 0) {
                continue;
            }

            if (validPointCount > 0) {
                lineStringBuilder.append(", ");
            }
            lineStringBuilder.append(longitude).append(" ").append(latitude);
            validPointCount++;
            previousLatitude = latitude;
            previousLongitude = longitude;
        }

        if (validPointCount < 2) {
            return null;
        }
        lineStringBuilder.append(")");
        return lineStringBuilder.toString();
    }

    private String toCursor(PastRunningSessionHistoryRow row) {
        return PastRunningSessionCursorCodec.encode(row.startedAt(), row.runningSessionId());
    }

    private void recordCourseCompletionIfEligible(RunningSession runningSession, User user) {
        if (runningSession.getCourse() == null || user == null) {
            return;
        }
        courseSignalService.recordCompletion(
                runningSession.getCourse(),
                user,
                runningSession.getId()
        );
    }
}
