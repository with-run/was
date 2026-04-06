package kr.withrun.was.domain.course.service;

import kr.withrun.was.domain.auth.security.AuthenticatedUser;
import kr.withrun.was.domain.course.dto.CourseDetailResponse;
import kr.withrun.was.domain.course.dto.CourseFilterResponse;
import kr.withrun.was.domain.course.dto.CourseGhostDetailResponse;
import kr.withrun.was.domain.course.dto.CourseGhostLeaderboardItemResponse;
import kr.withrun.was.domain.course.dto.CourseGhostLeaderboardRequest;
import kr.withrun.was.domain.course.dto.CourseGhostLeaderboardResponse;
import kr.withrun.was.domain.course.dto.CourseNavigationMetaResponse;
import kr.withrun.was.domain.course.dto.CourseRegisterMetaResponse;
import kr.withrun.was.domain.course.dto.CourseSurveyMetaResponse;
import kr.withrun.was.domain.course.dto.NearbyCourseItemResponse;
import kr.withrun.was.domain.course.dto.NearbyCoursesRequest;
import kr.withrun.was.domain.course.dto.NearbyCoursesResponse;
import kr.withrun.was.domain.course.dto.NearbyGhostCoursesRequest;
import kr.withrun.was.domain.course.dto.NearbyGhostCoursesResponse;
import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.repository.CourseBookmarkRepository;
import kr.withrun.was.domain.course.repository.CourseDifficultyRepository;
import kr.withrun.was.domain.course.repository.CourseGhostLeaderboardRepository;
import kr.withrun.was.domain.course.repository.CourseLikeRepository;
import kr.withrun.was.domain.course.repository.CourseRepository;
import kr.withrun.was.domain.course.repository.CourseReviewRepository;
import kr.withrun.was.domain.course.repository.CourseTypeMapRepository;
import kr.withrun.was.domain.course.repository.query.dto.CourseGhostLeaderboardRankRow;
import kr.withrun.was.domain.course.repository.query.dto.NearbyCoursePageRow;
import kr.withrun.was.domain.course.repository.query.dto.NearbyRecommendationCandidateRow;
import kr.withrun.was.domain.file.service.CloudFrontSignedUrlService;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleManeuverSampleAction;
import kr.withrun.was.domain.course.service.recommendation.CourseRecommendationScore;
import kr.withrun.was.domain.course.service.recommendation.HybridRecommendationService;
import kr.withrun.was.domain.course.service.recommendation.NearbyRecommendationInput;
import kr.withrun.was.domain.course.type.CourseDistanceType;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.domain.course.type.RouteType;
import kr.withrun.was.domain.course.util.CourseGhostLeaderboardCursorCodec;
import kr.withrun.was.domain.user.repository.UserRepository;
import kr.withrun.was.global.common.type.Difficulty;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CourseService {

    private static final int RECOMMENDED_NEARBY_LIMIT = 3;

    private final CourseRepository courseRepository;
    private final CourseDifficultyRepository courseDifficultyRepository;
    private final CourseTypeMapRepository courseTypeMapRepository;
    private final CourseBookmarkRepository courseBookmarkRepository;
    private final CourseLikeRepository courseLikeRepository;
    private final CourseReviewRepository courseReviewRepository;
    private final CourseGhostLeaderboardRepository courseGhostLeaderboardRepository;
    private final UserRepository userRepository;
    private final HybridRecommendationService hybridRecommendationService;
    private final CloudFrontSignedUrlService cloudFrontSignedUrlService;
    private final CourseAccessPolicy courseAccessPolicy;

    public NearbyCoursesResponse findNearbyCourses(NearbyCoursesRequest request, AuthenticatedUser user) {
        Long currentUserId = user == null ? null : user.userId();
        List<NearbyCoursePageRow> pageRows = courseRepository.findNearbyCoursePageRows(
                request.preferredDistanceMs(),
                request.targetLatitude(),
                request.targetLongitude(),
                request.latitude(),
                request.longitude(),
                request.status(),
                request.sortBy(),
                request.radiusM(),
                request.page(),
                request.size()
        );
        long totalElements = courseRepository.countNearbyCourses(
                request.preferredDistanceMs(),
                request.targetLatitude(),
                request.targetLongitude(),
                request.status(),
                request.radiusM()
        );
        boolean hasNext = (long) (request.page() + 1) * request.size() < totalElements;
        int totalPages = totalElements == 0 ? 0 : (int) ((totalElements + request.size() - 1) / request.size());
        List<NearbyCourseItemResponse> items = enrichNearbyCourseItemsWithInteraction(
                pageRows.stream()
                        .map(this::toNearbyCourseItemResponse)
                        .toList(),
                currentUserId
        );

        return new NearbyCoursesResponse(
                items,
                request.page(),
                request.size(),
                totalElements,
                totalPages,
                hasNext
        );
    }

    public NearbyCoursesResponse findRecommendedNearbyCourses(NearbyCoursesRequest request, AuthenticatedUser user) {
        Long userId = user == null ? null : user.userId();
        List<NearbyCourseItemResponse> scoredItems = findScoredNearbyCourseItems(request, userId, RECOMMENDED_NEARBY_LIMIT);
        List<NearbyCourseItemResponse> items = enrichNearbyCourseItemsWithInteraction(
                scoredItems.stream()
                        .limit(RECOMMENDED_NEARBY_LIMIT)
                        .toList(),
                userId
        );
        long totalElements = items.size();
        int totalPages = totalElements == 0 ? 0 : 1;
        boolean hasNext = false;
        int size = items.size();

        return new NearbyCoursesResponse(
                items,
                0,
                size,
                totalElements,
                totalPages,
                hasNext
        );
    }

    public NearbyGhostCoursesResponse findNearbyGhostCourses(NearbyGhostCoursesRequest request, AuthenticatedUser user) {
        Long currentUserId = user == null ? null : user.userId();
        List<NearbyCoursePageRow> pageRows = courseRepository.findNearbyGhostCoursePageRows(
                request.preferredDistanceMs(),
                request.targetLatitude(),
                request.targetLongitude(),
                request.latitude(),
                request.longitude(),
                request.sortBy(),
                request.radiusM(),
                request.page(),
                request.size()
        );
        long totalElements = courseRepository.countNearbyGhostCourses(
                request.preferredDistanceMs(),
                request.targetLatitude(),
                request.targetLongitude(),
                request.radiusM()
        );
        boolean hasNext = (long) (request.page() + 1) * request.size() < totalElements;
        int totalPages = totalElements == 0 ? 0 : (int) ((totalElements + request.size() - 1) / request.size());
        List<NearbyCourseItemResponse> items = enrichNearbyCourseItemsWithInteraction(
                pageRows.stream()
                        .map(this::toNearbyCourseItemResponse)
                        .toList(),
                currentUserId
        );

        return new NearbyGhostCoursesResponse(
                items,
                request.page(),
                request.size(),
                totalElements,
                totalPages,
                hasNext
        );
    }

    public CourseRegisterMetaResponse findCourseRegisterMeta() {
        return new CourseRegisterMetaResponse(
                Arrays.stream(CourseType.values())
                        .map(CourseRegisterMetaResponse.CourseTypeOption::from)
                        .toList(),
                List.of(CourseStatus.COMMUNITY, CourseStatus.PRIVATE).stream()
                        .map(CourseRegisterMetaResponse.ModeOption::from)
                        .toList(),
                Arrays.stream(RouteType.values())
                        .map(CourseRegisterMetaResponse.RouteTypeOption::from)
                        .toList(),
                Arrays.stream(Difficulty.values())
                        .map(CourseRegisterMetaResponse.DifficultyOption::from)
                        .toList()
        );
    }

    public CourseSurveyMetaResponse findSurveyMeta() {
        return new CourseSurveyMetaResponse(
                Arrays.stream(CourseType.values())
                        .map(CourseSurveyMetaResponse.CourseTypeOption::from)
                        .toList(),
                Arrays.stream(Difficulty.values())
                        .map(CourseSurveyMetaResponse.DifficultyOption::from)
                        .toList()
        );
    }

    public CourseFilterResponse findCourseFilters() {
        return new CourseFilterResponse(
                Arrays.stream(CourseDistanceType.values())
                        .map(CourseFilterResponse.CourseDistanceTypeOption::from)
                        .toList(),
                Arrays.stream(CourseType.values())
                        .map(CourseFilterResponse.CourseTypeOption::from)
                        .toList(),
                Arrays.stream(Difficulty.values())
                        .map(CourseFilterResponse.DifficultyOption::from)
                        .toList()
        );
    }

    public CourseNavigationMetaResponse findCourseNavigationMeta() {
        return new CourseNavigationMetaResponse(
                Arrays.stream(NavigationBundleManeuverSampleAction.values())
                        .map(CourseNavigationMetaResponse.NavigationOption::from)
                        .toList()
        );
    }

    public CourseDetailResponse findCourseDetail(Long courseId, Long currentUserId) {
        Course course = getCourse(courseId, currentUserId);

        return new CourseDetailResponse(
                course.getId(),
                course.getTitle(),
                course.getStatus(),
                course.getRouteType(),
                CourseDetailResponse.DifficultyOption.from(courseDifficultyRepository.findDifficultyByCourseId(courseId)
                        .orElse(null)),
                course.getDistanceM(),
                course.getElevationGainM(),
                cloudFrontSignedUrlService.generateSignedUrl(course.getSnapshotImageUrl()),
                course.getStartLatitude(),
                course.getStartLongitude(),
                course.getEndLatitude(),
                course.getEndLongitude(),
                course.getCoordinates(),
                courseTypeMapRepository.findCourseTypesByCourseId(courseId).stream()
                        .map(CourseDetailResponse.CourseTypeOption::from)
                        .toList(),
                courseLikeRepository.countByCourseId(courseId),
                isLiked(courseId, currentUserId),
                isBookmarked(courseId, currentUserId),
                courseReviewRepository.findAverageRatingByCourseId(courseId)
        );
    }

    public CourseGhostDetailResponse findCourseGhostDetail(Long courseId, Long userId) {
        Course course = getCourse(courseId, userId);

        return CourseGhostDetailResponse.of(
                course,
                courseDifficultyRepository.findDifficultyByCourseId(courseId)
                        .orElse(null),
                courseTypeMapRepository.findCourseTypesByCourseId(courseId),
                courseLikeRepository.countByCourseId(courseId),
                isLiked(courseId, userId),
                isBookmarked(courseId, userId),
                courseReviewRepository.findAverageRatingByCourseId(courseId),
                cloudFrontSignedUrlService.generateSignedUrl(course.getSnapshotImageUrl()),
                findMyGhostRecord(courseId, userId)
        );
    }

    public CourseGhostLeaderboardResponse findCourseGhostLeaderboard(Long courseId, Long currentUserId, CourseGhostLeaderboardRequest request) {
        Course course = getCourse(courseId, currentUserId);

        List<CourseGhostLeaderboardRankRow> rows = courseGhostLeaderboardRepository.findRankedRowsByCourseId(courseId);
        int startIndex = resolveGhostLeaderboardStartIndex(rows, request.cursor());
        List<CourseGhostLeaderboardRankRow> remainingRows = rows.subList(startIndex, rows.size());
        boolean hasMore = remainingRows.size() > request.size();
        List<CourseGhostLeaderboardRankRow> pageRows = List.copyOf(remainingRows.subList(0, Math.min(request.size(), remainingRows.size())));
        String nextCursor = hasMore ? toGhostLeaderboardCursor(pageRows.getLast()) : null;

        List<CourseGhostLeaderboardItemResponse> items = pageRows.stream()
                .map(row -> new CourseGhostLeaderboardItemResponse(
                        row.leaderboardId(),
                        row.userId(),
                        row.nickname(),
                        row.runningSessionId(),
                        row.point(),
                        row.rank(),
                        row.createdAt()
                ))
                .toList();

        return new CourseGhostLeaderboardResponse(items, course.getRouteType(), hasMore, nextCursor);
    }

    private Course getCourse(Long courseId, Long currentUserId) {
        return courseAccessPolicy.getAccessibleCourse(courseRepository, courseId, currentUserId);
    }

    private CourseGhostDetailResponse.MyRecord findMyGhostRecord(Long courseId, Long userId) {
        if (userId == null) {
            return null;
        }

        return courseGhostLeaderboardRepository.findTopByUserIdAndCourseId(userId, courseId)
                .map(leaderboard -> new CourseGhostDetailResponse.MyRecord(
                        leaderboard.getId(),
                        leaderboard.getRunningSession().getId(),
                        leaderboard.getRunningSession().getDurationSec(),
                        leaderboard.getPoint(),
                        findRank(courseId, leaderboard.getId()),
                        leaderboard.getCreatedAt()
                ))
                .orElse(null);
    }

    private boolean isLiked(Long courseId, Long currentUserId) {
        if (currentUserId == null) {
            return false;
        }

        return courseLikeRepository.findByCourseIdAndUserId(courseId, currentUserId).isPresent();
    }

    private boolean isBookmarked(Long courseId, Long currentUserId) {
        if (currentUserId == null) {
            return false;
        }

        return courseBookmarkRepository.findByCourseIdAndUserId(courseId, currentUserId).isPresent();
    }

    private Long findRank(Long courseId, Long leaderboardId) {
        return courseGhostLeaderboardRepository.findRankedRowsByCourseId(courseId).stream()
                .filter(row -> row.leaderboardId().equals(leaderboardId))
                .map(CourseGhostLeaderboardRankRow::rank)
                .findFirst()
                .orElse(null);
    }

    private int resolveGhostLeaderboardStartIndex(List<CourseGhostLeaderboardRankRow> rows, String cursor) {
        if (cursor == null) {
            return 0;
        }

        CourseGhostLeaderboardCursorCodec.CursorPayload payload = CourseGhostLeaderboardCursorCodec.decode(cursor);
        for (int index = 0; index < rows.size(); index++) {
            if (isStrictlyAfter(rows.get(index), payload)) {
                return index;
            }
        }

        return rows.size();
    }

    private boolean isStrictlyAfter(
            CourseGhostLeaderboardRankRow row,
            CourseGhostLeaderboardCursorCodec.CursorPayload payload
    ) {
        return row.point() < payload.point()
                || (row.point().equals(payload.point()) && row.leaderboardId() > payload.leaderboardId());
    }

    private String toGhostLeaderboardCursor(CourseGhostLeaderboardRankRow row) {
        return CourseGhostLeaderboardCursorCodec.encode(row.point(), row.leaderboardId());
    }

    private List<NearbyCourseItemResponse> findScoredNearbyCourseItems(
            NearbyCoursesRequest request,
            Long userId,
            int recommendedLimit
    ) {
        List<NearbyCourseCandidate> candidates = findNearbyCourseCandidates(request);
        if (candidates.isEmpty()) {
            return List.of();
        }

        /**
         * jwt + security 설정 완료 후 userId를 request 또는 @AuthenticationPrincipal에서 꺼내 받도록 구현
         */
        Map<Long, CourseRecommendationScore> scoreByCourseId = hybridRecommendationService.scoreNearbyCourses(
                userId,
                request.radiusM(),
                candidates.stream()
                        .map(this::toNearbyRecommendationInput)
                        .toList()
        );

        List<NearbyCourseCandidate> sortedCandidates = candidates.stream()
                .sorted(Comparator
                        .comparingDouble((NearbyCourseCandidate candidate) -> resolveFinalScore(scoreByCourseId, candidate.courseId()))
                        .reversed()
                        .thenComparingInt(candidate -> candidate.distanceFromUserM() == null
                                ? Integer.MAX_VALUE
                                : candidate.distanceFromUserM())
                        .thenComparing(candidate -> candidate.courseId() == null
                                ? Long.MAX_VALUE
                                : candidate.courseId()))
                .toList();

        Set<Long> recommendedCourseIds = resolveRecommendedCourseIds(sortedCandidates, recommendedLimit);

        return sortedCandidates.stream()
                .map(candidate -> toNearbyCourseItemResponse(candidate, recommendedCourseIds.contains(candidate.courseId())))
                .toList();
    }

    private List<NearbyCourseCandidate> findNearbyCourseCandidates(NearbyCoursesRequest request) {
        List<NearbyRecommendationCandidateRow> rows = courseRepository.findNearbyRecommendationCandidates(
                request.preferredDistanceMs(),
                request.targetLatitude(),
                request.targetLongitude(),
                request.latitude(),
                request.longitude(),
                request.radiusM()
        );
        if (rows.isEmpty()) {
            return List.of();
        }

        Map<Long, List<NearbyRecommendationCandidateRow>> rowsByCourseId = rows.stream()
                .filter(row -> isPublicFeedStatus(row.status()))
                .filter(row -> row.courseId() != null)
                .collect(Collectors.groupingBy(
                        NearbyRecommendationCandidateRow::courseId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return rowsByCourseId.values().stream()
                .map(this::toNearbyCourseCandidate)
                .filter(Objects::nonNull)
                .toList();
    }

    private boolean isPublicFeedStatus(CourseStatus status) {
        return status == CourseStatus.OFFICIAL || status == CourseStatus.COMMUNITY;
    }

    private NearbyCourseCandidate toNearbyCourseCandidate(
            List<NearbyRecommendationCandidateRow> groupedRows
    ) {
        if (groupedRows == null || groupedRows.isEmpty()) {
            return null;
        }

        NearbyRecommendationCandidateRow representative = groupedRows.getFirst();

        Difficulty difficulty = groupedRows.stream()
                .map(NearbyRecommendationCandidateRow::difficulty)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        List<CourseType> courseTypes = groupedRows.stream()
                .map(NearbyRecommendationCandidateRow::courseType)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        return new NearbyCourseCandidate(
                representative.courseId(),
                representative.title(),
                representative.status(),
                representative.routeType(),
                representative.distanceM(),
                representative.elevationGainM(),
                representative.startLatitude(),
                representative.startLongitude(),
                representative.endLatitude(),
                representative.endLongitude(),
                representative.snapshotImageUrl(),
                difficulty,
                courseTypes,
                representative.distanceFromTargetM(),
                representative.distanceFromUserM()
        );
    }

    private NearbyRecommendationInput toNearbyRecommendationInput(NearbyCourseCandidate candidate) {
        return new NearbyRecommendationInput(
                candidate.courseId(),
                candidate.distanceM(),
                candidate.distanceFromUserM(),
                candidate.difficulty(),
                candidate.courseTypes()
        );
    }

    private double resolveFinalScore(
            Map<Long, CourseRecommendationScore> scoreByCourseId,
            Long courseId
    ) {
        if (courseId == null) {
            return 0.0;
        }

        CourseRecommendationScore score = scoreByCourseId.get(courseId);
        if (score == null) {
            return 0.0;
        }

        return score.finalScore();
    }

    private Set<Long> resolveRecommendedCourseIds(
            List<NearbyCourseCandidate> sortedCandidates,
            int recommendedLimit
    ) {
        int recommendedCount = Math.max(0, Math.min(recommendedLimit, sortedCandidates.size()));
        if (recommendedCount == 0) {
            return Set.of();
        }

        return sortedCandidates.stream()
                .limit(recommendedCount)
                .map(NearbyCourseCandidate::courseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<NearbyCourseItemResponse> toPagedNearbyCourseItems(
            List<NearbyCourseItemResponse> sortedItems,
            int page,
            int size
    ) {
        int fromIndex = (int) Math.min((long) page * size, sortedItems.size());
        int toIndex = Math.min(fromIndex + size, sortedItems.size());
        if (fromIndex >= toIndex) {
            return List.of();
        }
        return sortedItems.subList(fromIndex, toIndex);
    }

    private NearbyCourseItemResponse toNearbyCourseItemResponse(NearbyCoursePageRow row) {
        return new NearbyCourseItemResponse(
                row.courseId(),
                row.title(),
                row.status(),
                row.routeType(),
                row.distanceM(),
                row.elevationGainM(),
                row.startLatitude(),
                row.startLongitude(),
                row.endLatitude(),
                row.endLongitude(),
                row.distanceFromTargetM(),
                row.distanceFromUserM(),
                NearbyCourseItemResponse.DifficultyOption.from(row.difficulty()),
                courseTypeMapRepository.findCourseTypesByCourseId(row.courseId()).stream()
                        .map(NearbyCourseItemResponse.CourseTypeOption::from)
                        .toList(),
                false,
                0L,
                0L,
                false,
                false,
                toSnapshotImageUrl(row.snapshotImageUrl())
        );
    }

    private NearbyCourseItemResponse toNearbyCourseItemResponse(
            NearbyCourseCandidate candidate,
            boolean isRecommended
    ) {
        return new NearbyCourseItemResponse(
                candidate.courseId(),
                candidate.title(),
                candidate.status(),
                candidate.routeType(),
                candidate.distanceM(),
                candidate.elevationGainM(),
                candidate.startLatitude(),
                candidate.startLongitude(),
                candidate.endLatitude(),
                candidate.endLongitude(),
                candidate.distanceFromTargetM(),
                candidate.distanceFromUserM(),
                NearbyCourseItemResponse.DifficultyOption.from(candidate.difficulty()),
                candidate.courseTypes().stream()
                        .map(NearbyCourseItemResponse.CourseTypeOption::from)
                        .toList(),
                isRecommended,
                0L,
                0L,
                false,
                false,
                toSnapshotImageUrl(candidate.snapshotImageUrl())
        );
    }

    private List<NearbyCourseItemResponse> enrichNearbyCourseItemsWithInteraction(
            List<NearbyCourseItemResponse> items,
            Long currentUserId
    ) {
        if (items.isEmpty()) return items;

        List<Long> courseIds = items.stream()
                .map(NearbyCourseItemResponse::courseId)
                .toList();

        Map<Long, Long> likeCountsByCourseIds = courseLikeRepository.countByCourseIds(courseIds);
        Map<Long, Long> bookmarkCountsByCourseIds = courseBookmarkRepository.countByCourseIds(courseIds);

        return items.stream()
                .map(item -> new NearbyCourseItemResponse(
                        item.courseId(),
                        item.title(),
                        item.status(),
                        item.routeType(),
                        item.distanceM(),
                        item.elevationGainM(),
                        item.startLatitude(),
                        item.startLongitude(),
                        item.endLatitude(),
                        item.endLongitude(),
                        item.distanceFromTargetM(),
                        item.distanceFromUserM(),
                        item.difficulty(),
                        item.courseTypes(),
                        item.isRecommended(),
                        likeCountsByCourseIds.getOrDefault(item.courseId(), 0L),
                        bookmarkCountsByCourseIds.getOrDefault(item.courseId(), 0L),
                        isLiked(item.courseId(), currentUserId),
                        isBookmarked(item.courseId(), currentUserId),
                        item.snapshotImageUrl()
                ))
                .toList();
    }

    private String toSnapshotImageUrl(String snapshotImagePath) {
        return cloudFrontSignedUrlService.generateSignedUrl(snapshotImagePath);
    }

    private record NearbyCourseCandidate(
            Long courseId,
            String title,
            CourseStatus status,
            RouteType routeType,
            Integer distanceM,
            Integer elevationGainM,
            Double startLatitude,
            Double startLongitude,
            Double endLatitude,
            Double endLongitude,
            String snapshotImageUrl,
            Difficulty difficulty,
            List<CourseType> courseTypes,
            Integer distanceFromTargetM,
            Integer distanceFromUserM
    ) {
    }
}
