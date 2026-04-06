package kr.withrun.was.domain.course.service;

import kr.withrun.was.domain.course.dto.NearbyCoursesRequest;
import kr.withrun.was.domain.course.dto.NearbyCoursesResponse;
import kr.withrun.was.domain.course.dto.PreferredDistanceRange;
import kr.withrun.was.domain.course.repository.CourseBookmarkRepository;
import kr.withrun.was.domain.course.repository.CourseDifficultyRepository;
import kr.withrun.was.domain.course.repository.CourseGhostLeaderboardRepository;
import kr.withrun.was.domain.course.repository.CourseLikeRepository;
import kr.withrun.was.domain.course.repository.CourseRepository;
import kr.withrun.was.domain.course.repository.CourseReviewRepository;
import kr.withrun.was.domain.course.repository.CourseTypeMapRepository;
import kr.withrun.was.domain.course.repository.query.dto.NearbyRecommendationCandidateRow;
import kr.withrun.was.domain.file.service.CloudFrontSignedUrlService;
import kr.withrun.was.domain.course.service.recommendation.CourseRecommendationScore;
import kr.withrun.was.domain.course.service.recommendation.HybridRecommendationService;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.domain.course.type.NearbyCourseSortBy;
import kr.withrun.was.domain.course.type.RouteType;
import kr.withrun.was.domain.user.repository.UserRepository;
import kr.withrun.was.domain.course.vo.Coordinates;
import kr.withrun.was.global.common.type.Difficulty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("주변 코스 조회 서비스")
class NearbyCourseQueryServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseDifficultyRepository courseDifficultyRepository;

    @Mock
    private CourseTypeMapRepository courseTypeMapRepository;

    @Mock
    private CourseBookmarkRepository courseBookmarkRepository;

    @Mock
    private CourseLikeRepository courseLikeRepository;

    @Mock
    private CourseReviewRepository courseReviewRepository;

    @Mock
    private CourseGhostLeaderboardRepository courseGhostLeaderboardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HybridRecommendationService hybridRecommendationService;

    @Mock
    private CloudFrontSignedUrlService cloudFrontSignedUrlService;

    private CourseService courseService;

    @BeforeEach
    void setUp() {
        courseService = new CourseService(
                courseRepository,
                courseDifficultyRepository,
                courseTypeMapRepository,
                courseBookmarkRepository,
                courseLikeRepository,
                courseReviewRepository,
                courseGhostLeaderboardRepository,
                userRepository,
                hybridRecommendationService,
                cloudFrontSignedUrlService,
                new CourseAccessPolicy()
        );
        when(cloudFrontSignedUrlService.generateSignedUrl(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> {
                    Object value = invocation.getArgument(0);
                    return value == null ? null : "signed::" + value;
                });
    }

    @Test
    @DisplayName("후보 코스에 하이브리드 점수를 적용해 추천 코스 응답을 반환한다")
    void returnsPagedNearbyCoursesFromHybridScoredCandidates() {
        NearbyCoursesRequest request = request(3000, 0, 2);
        List<NearbyRecommendationCandidateRow> rows = List.of(
                candidateRow(10L, CourseType.RIVERSIDE, Difficulty.EASY, 37.5700, 126.9820, "snapshot-10"),
                candidateRow(20L, CourseType.PARK, Difficulty.MEDIUM, 37.5710, 126.9830, "snapshot-20")
        );

        when(courseRepository.findNearbyRecommendationCandidates(
                request.preferredDistanceMs(),
                request.targetLatitude(),
                request.targetLongitude(),
                request.latitude(),
                request.longitude(),
                request.radiusM()
        )).thenReturn(rows);
        when(hybridRecommendationService.scoreNearbyCourses(
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq(request.radiusM()),
                org.mockito.ArgumentMatchers.anyList()
        )).thenReturn(Map.of(
                10L, new CourseRecommendationScore(10L, 0.5, 0.5, 0.5, 0.0, 0.5, 0.40),
                20L, new CourseRecommendationScore(20L, 0.7, 0.7, 0.7, 0.0, 0.7, 0.80)
        ));
        when(courseLikeRepository.countByCourseIds(List.of(20L, 10L))).thenReturn(Map.of(10L, 3L, 20L, 7L));
        when(courseBookmarkRepository.countByCourseIds(List.of(20L, 10L))).thenReturn(Map.of(10L, 1L, 20L, 2L));

        NearbyCoursesResponse response = courseService.findRecommendedNearbyCourses(request, null);

        assertThat(response.page()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(2L);
        assertThat(response.totalPages()).isEqualTo(1);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.items()).hasSize(2);
        assertThat(response.items().getFirst().courseId()).isEqualTo(20L);
        assertThat(response.items().getFirst().routeType()).isEqualTo(RouteType.LOOP);
        assertThat(response.items().getFirst().isRecommended()).isTrue();
        assertThat(response.items().getFirst().likeCount()).isEqualTo(7L);
        assertThat(response.items().getFirst().bookmarkCount()).isEqualTo(2L);
        assertThat(response.items().getFirst().snapshotImageUrl()).isEqualTo("signed::snapshot-20");
        assertThat(response.items().get(1).isRecommended()).isTrue();
    }

    @Test
    @DisplayName("추천 코스 조회는 요청 페이지와 무관하게 추천 목록만 반환한다")
    void returnsEmptyPageWhenPageOutOfRange() {
        NearbyCoursesRequest request = request(3000, 1, 3);
        List<NearbyRecommendationCandidateRow> rows = List.of(
                candidateRow(10L, CourseType.RIVERSIDE, Difficulty.EASY, 37.5700, 126.9820, "snapshot-10")
        );

        when(courseRepository.findNearbyRecommendationCandidates(
                request.preferredDistanceMs(),
                request.targetLatitude(),
                request.targetLongitude(),
                request.latitude(),
                request.longitude(),
                request.radiusM()
        )).thenReturn(rows);
        when(hybridRecommendationService.scoreNearbyCourses(
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq(request.radiusM()),
                org.mockito.ArgumentMatchers.anyList()
        )).thenReturn(Map.of(
                10L, new CourseRecommendationScore(10L, 0.5, 0.5, 0.5, 0.0, 0.5, 0.50)
        ));

        NearbyCoursesResponse response = courseService.findRecommendedNearbyCourses(request, null);

        assertThat(response.page()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(1);
        assertThat(response.items()).hasSize(1);
        assertThat(response.hasNext()).isFalse();
    }

    @Test
    @DisplayName("인기순 정렬 요청도 추천 코스 계산은 정상 처리한다")
    void supportsPopularSortForNearbyCourses() {
        NearbyCoursesRequest request = request(3000, NearbyCourseSortBy.POPULAR, 0, 2);
        List<NearbyRecommendationCandidateRow> rows = List.of(
                candidateRow(20L, CourseType.PARK, Difficulty.MEDIUM, 37.5700, 126.9820, "snapshot-20")
        );

        when(courseRepository.findNearbyRecommendationCandidates(
                request.preferredDistanceMs(),
                request.targetLatitude(),
                request.targetLongitude(),
                request.latitude(),
                request.longitude(),
                request.radiusM()
        )).thenReturn(rows);
        when(hybridRecommendationService.scoreNearbyCourses(
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq(request.radiusM()),
                org.mockito.ArgumentMatchers.anyList()
        )).thenReturn(Map.of(
                20L, new CourseRecommendationScore(20L, 0.7, 0.7, 0.7, 0.0, 0.7, 0.70)
        ));
        when(courseLikeRepository.countByCourseIds(List.of(20L))).thenReturn(Map.of(20L, 7L));
        when(courseBookmarkRepository.countByCourseIds(List.of(20L))).thenReturn(Map.of(20L, 2L));

        NearbyCoursesResponse response = courseService.findRecommendedNearbyCourses(request, null);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().courseId()).isEqualTo(20L);
        assertThat(response.items().getFirst().isRecommended()).isTrue();
        assertThat(response.items().getFirst().likeCount()).isEqualTo(7L);
        assertThat(response.items().getFirst().bookmarkCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("추천 코스 응답은 repository 가 private 후보를 넘겨도 private 코스를 제외한다")
    void excludesPrivateCourseFromRecommendedResponseEvenWhenRepositoryReturnsIt() {
        NearbyCoursesRequest request = request(3000, 0, 3);
        List<NearbyRecommendationCandidateRow> rows = List.of(
                candidateRow(10L, CourseStatus.PRIVATE, CourseType.RIVERSIDE, Difficulty.EASY, 37.5700, 126.9820, "snapshot-10"),
                candidateRow(20L, CourseStatus.OFFICIAL, CourseType.PARK, Difficulty.MEDIUM, 37.5710, 126.9830, "snapshot-20")
        );

        when(courseRepository.findNearbyRecommendationCandidates(
                request.preferredDistanceMs(),
                request.targetLatitude(),
                request.targetLongitude(),
                request.latitude(),
                request.longitude(),
                request.radiusM()
        )).thenReturn(rows);
        when(hybridRecommendationService.scoreNearbyCourses(
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq(request.radiusM()),
                org.mockito.ArgumentMatchers.anyList()
        )).thenReturn(Map.of(
                10L, new CourseRecommendationScore(10L, 0.5, 0.5, 0.5, 0.0, 0.5, 0.90),
                20L, new CourseRecommendationScore(20L, 0.5, 0.5, 0.5, 0.0, 0.5, 0.70)
        ));
        when(courseLikeRepository.countByCourseIds(List.of(20L))).thenReturn(Map.of(20L, 7L));
        when(courseBookmarkRepository.countByCourseIds(List.of(20L))).thenReturn(Map.of(20L, 2L));

        NearbyCoursesResponse response = courseService.findRecommendedNearbyCourses(request, null);

        assertThat(response.items()).extracting(item -> item.courseId()).containsExactly(20L);
    }

    private NearbyCoursesRequest request(int radiusM, Integer page, Integer size) {
        return request(radiusM, NearbyCourseSortBy.DISTANCE, page, size);
    }

    private NearbyCoursesRequest request(int radiusM, NearbyCourseSortBy sortBy, Integer page, Integer size) {
        return new NearbyCoursesRequest(
                37.5665,
                126.9780,
                37.5700,
                126.9820,
                radiusM,
                List.of(new PreferredDistanceRange(1, 5000)),
                null,
                sortBy,
                page,
                size
        );
    }

    private NearbyRecommendationCandidateRow candidateRow(
            Long courseId,
            CourseStatus status,
            CourseType courseType,
            Difficulty difficulty,
            double latitude,
            double longitude,
            String snapshotImageUrl
    ) {
        return new NearbyRecommendationCandidateRow(
                courseId,
                "Course " + courseId,
                status,
                RouteType.LOOP,
                5000,
                120,
                latitude,
                longitude,
                37.5700,
                126.9820,
                snapshotImageUrl,
                new Coordinates(
                        List.of(latitude, latitude + 0.0001),
                        List.of(longitude, longitude + 0.0001),
                        List.of(0.0, 0.0)
                ),
                difficulty,
                courseType,
                250,
                120
        );
    }

    private NearbyRecommendationCandidateRow candidateRow(
            Long courseId,
            CourseType courseType,
            Difficulty difficulty,
            double latitude,
            double longitude,
            String snapshotImageUrl
    ) {
        return candidateRow(courseId, CourseStatus.OFFICIAL, courseType, difficulty, latitude, longitude, snapshotImageUrl);
    }

    private void setField(Object target, String fieldName, Object value) {
        Class<?> currentClass = target.getClass();
        while (currentClass != null) {
            try {
                java.lang.reflect.Field field = currentClass.getDeclaredField(fieldName);
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
