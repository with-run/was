package kr.withrun.was.domain.course.repository;

import jakarta.persistence.EntityManager;
import kr.withrun.was.domain.course.dto.PreferredDistanceRange;
import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.entity.CourseDifficulty;
import kr.withrun.was.domain.course.entity.CourseTypeMap;
import kr.withrun.was.domain.course.repository.query.dto.NearbyRecommendationCandidateRow;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.domain.course.type.NearbyCourseSortBy;
import kr.withrun.was.domain.course.type.NearbyGhostCourseSortBy;
import kr.withrun.was.domain.course.vo.Coordinates;
import kr.withrun.was.domain.course.vo.GeoPoint;
import kr.withrun.was.global.common.type.Difficulty;
import kr.withrun.was.global.config.QuerydslConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.config.import=",
        "spring.cloud.aws.parameterstore.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:course-row;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import(QuerydslConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("코스 리포지토리 Row")
class CourseRepositoryRowTest {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EntityManager entityManager;

    @DisplayName("좌표 정보가 저장과 Row 조회를 거쳐도 유지된다")
    @Test
    void preservesCoordinatesThroughPersistenceAndRowQuery() {
        Coordinates coordinates = new Coordinates(
                List.of(
                        new GeoPoint(37.5665, 126.9780, 12.0),
                        new GeoPoint(37.5668, 126.9791, 15.5)
                )
        );
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 11, 1, 0);

        Course course = Course.builder()
                .title("Row Course")
                .status(CourseStatus.OFFICIAL)
                .distanceM(10000)
                .elevationGainM(40)
                .snapshotImageUrl("snapshot")
                .startLatitude(37.5665)
                .startLongitude(126.9780)
                .endLatitude(37.5700)
                .endLongitude(126.9820)
                .coordinates(coordinates)
                .build();
        setCreatedAt(course, createdAt);
        course = courseRepository.save(course);

        CourseDifficulty courseDifficulty = CourseDifficulty.create(course, Difficulty.MEDIUM);
        CourseTypeMap courseTypeMap = CourseTypeMap.create(course, CourseType.PARK);
        setCreatedAt(courseDifficulty, createdAt);
        setCreatedAt(courseTypeMap, createdAt);

        entityManager.persist(courseDifficulty);
        entityManager.persist(courseTypeMap);
        entityManager.flush();
        entityManager.clear();

        List<NearbyRecommendationCandidateRow> results = findNearbyRecommendationCandidates(null);

        assertThat(results).hasSize(1);
        NearbyRecommendationCandidateRow row = results.getFirst();
        assertThat(row.courseId()).isEqualTo(course.getId());
        assertThat(row.coordinates()).isEqualTo(coordinates);
        assertThat(row.difficulty()).isEqualTo(Difficulty.MEDIUM);
        assertThat(row.courseType()).isEqualTo(CourseType.PARK);
    }

    @DisplayName("객체 배열 형태 좌표 JSON도 Row 조회 시 Coordinates로 역직렬화한다")
    @Test
    void deserializesArrayShapedCoordinatesFromDatabase() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 11, 2, 0);

        Course course = Course.builder()
                .title("Array Course")
                .status(CourseStatus.OFFICIAL)
                .distanceM(10000)
                .elevationGainM(40)
                .snapshotImageUrl("snapshot")
                .startLatitude(37.5665)
                .startLongitude(126.9780)
                .endLatitude(37.5700)
                .endLongitude(126.9820)
                .coordinates(new Coordinates(List.of(new GeoPoint(0.0, 0.0, 0.0))))
                .build();
        setCreatedAt(course, createdAt);
        course = courseRepository.save(course);

        CourseDifficulty courseDifficulty = CourseDifficulty.create(course, Difficulty.MEDIUM);
        CourseTypeMap courseTypeMap = CourseTypeMap.create(course, CourseType.PARK);
        setCreatedAt(courseDifficulty, createdAt);
        setCreatedAt(courseTypeMap, createdAt);

        entityManager.persist(courseDifficulty);
        entityManager.persist(courseTypeMap);
        entityManager.flush();

        updateCoordinatesJson(
                course.getId(),
                "[{\"latitude\":37.5665,\"longitude\":126.9780,\"elevationM\":12.0},{\"latitude\":37.5668,\"longitude\":126.9791,\"elevationM\":15.5}]"
        );
        entityManager.clear();

        List<NearbyRecommendationCandidateRow> results = findNearbyRecommendationCandidates(null);

        assertThat(results).hasSize(1);
        NearbyRecommendationCandidateRow row = results.getFirst();
        assertThat(row.coordinates()).isEqualTo(new Coordinates(
                List.of(
                        new GeoPoint(37.5665, 126.9780, 12.0),
                        new GeoPoint(37.5668, 126.9791, 15.5)
                )
        ));
    }

    @DisplayName("선호 거리 목록과 오차 200m에 맞는 공식 코스 후보만 조회한다")
    @Test
    void filtersNearbyCourseCandidatesByPreferredDistances() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 11, 3, 0);

        Course matchedCourse = persistCourse("Matched Course", 5000, CourseStatus.OFFICIAL, createdAt);
        Course lowerBoundaryCourse = persistCourse("Lower Boundary Course", 4800, CourseStatus.OFFICIAL, createdAt);
        persistCourse("Outside Course", 5201, CourseStatus.OFFICIAL, createdAt);
        Course communityCourse = persistCourse("Community Course", 5000, CourseStatus.COMMUNITY, createdAt);
        persistCourse("Private Course", 5000, CourseStatus.PRIVATE, createdAt);

        List<NearbyRecommendationCandidateRow> results =
                findNearbyRecommendationCandidates(List.of(new PreferredDistanceRange(4800, 5200)));

        assertThat(results)
                .extracting(NearbyRecommendationCandidateRow::courseId)
                .containsExactlyInAnyOrder(matchedCourse.getId(), lowerBoundaryCourse.getId(), communityCourse.getId());
    }

    @DisplayName("주변 코스 row 조회와 count 는 private 코스를 제외한다")
    @Test
    void excludesPrivateCoursesFromNearbyCourseRowsAndCount() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 11, 5, 0);
        Course officialCourse = persistCourse("Official Nearby", 5000, CourseStatus.OFFICIAL, createdAt);
        Course communityCourse = persistCourse("Community Nearby", 5000, CourseStatus.COMMUNITY, createdAt);
        persistCourse("Private Nearby", 5000, CourseStatus.PRIVATE, createdAt);

        List<Long> rowCourseIds = courseRepository.findNearbyCoursePageRows(
                        List.of(new PreferredDistanceRange(4800, 5200)),
                        37.5665,
                        126.9780,
                        37.5665,
                        126.9780,
                        null,
                        NearbyCourseSortBy.DISTANCE,
                        3000,
                        0,
                        10
                ).stream()
                .map(row -> row.courseId())
                .toList();

        long totalCount = courseRepository.countNearbyCourses(
                List.of(new PreferredDistanceRange(4800, 5200)),
                37.5665,
                126.9780,
                null,
                3000
        );

        assertThat(rowCourseIds).containsExactlyInAnyOrder(officialCourse.getId(), communityCourse.getId());
        assertThat(totalCount).isEqualTo(2L);
    }

    @DisplayName("주변 코스 row 조회에서 status=COMMUNITY면 COMMUNITY만 조회한다")
    @Test
    void filtersNearbyCourseRowsByCommunityStatus() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 11, 5, 30);
        persistCourse("Official Nearby Filtered", 5000, CourseStatus.OFFICIAL, createdAt);
        Course communityCourse = persistCourse("Community Nearby Filtered", 5000, CourseStatus.COMMUNITY, createdAt);
        persistCourse("Private Nearby Filtered", 5000, CourseStatus.PRIVATE, createdAt);

        List<Long> rowCourseIds = courseRepository.findNearbyCoursePageRows(
                        List.of(new PreferredDistanceRange(4800, 5200)),
                        37.5665,
                        126.9780,
                        37.5665,
                        126.9780,
                        CourseStatus.COMMUNITY,
                        NearbyCourseSortBy.DISTANCE,
                        3000,
                        0,
                        10
                ).stream()
                .map(row -> row.courseId())
                .toList();

        long totalCount = courseRepository.countNearbyCourses(
                List.of(new PreferredDistanceRange(4800, 5200)),
                37.5665,
                126.9780,
                CourseStatus.COMMUNITY,
                3000
        );

        assertThat(rowCourseIds).containsExactly(communityCourse.getId());
        assertThat(totalCount).isEqualTo(1L);
    }

    @DisplayName("주변 코스 row 조회에서 status=PRIVATE여도 PRIVATE는 조회하지 않는다")
    @Test
    void excludesPrivateCoursesEvenWhenPrivateStatusIsRequested() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 11, 5, 45);
        persistCourse("Official Nearby Visible", 5000, CourseStatus.OFFICIAL, createdAt);
        persistCourse("Community Nearby Visible", 5000, CourseStatus.COMMUNITY, createdAt);
        persistCourse("Private Nearby Hidden", 5000, CourseStatus.PRIVATE, createdAt);

        List<Long> rowCourseIds = courseRepository.findNearbyCoursePageRows(
                        List.of(new PreferredDistanceRange(4800, 5200)),
                        37.5665,
                        126.9780,
                        37.5665,
                        126.9780,
                        CourseStatus.PRIVATE,
                        NearbyCourseSortBy.DISTANCE,
                        3000,
                        0,
                        10
                ).stream()
                .map(row -> row.courseId())
                .toList();

        long totalCount = courseRepository.countNearbyCourses(
                List.of(new PreferredDistanceRange(4800, 5200)),
                37.5665,
                126.9780,
                CourseStatus.PRIVATE,
                3000
        );

        assertThat(rowCourseIds).isEmpty();
        assertThat(totalCount).isEqualTo(0L);
    }

    @DisplayName("주변 고스트 코스 row 조회와 count 는 private 코스를 제외한다")
    @Test
    void includesOnlyOfficialCoursesForNearbyGhostCourseRowsAndCount() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 11, 6, 0);
        Course officialCourse = persistCourse("Official Ghost", 5000, CourseStatus.OFFICIAL, createdAt);
        persistCourse("Community Ghost", 5000, CourseStatus.COMMUNITY, createdAt);
        persistCourse("Private Ghost", 5000, CourseStatus.PRIVATE, createdAt);

        List<Long> rowCourseIds = courseRepository.findNearbyGhostCoursePageRows(
                        List.of(new PreferredDistanceRange(4800, 5200)),
                        37.5665,
                        126.9780,
                        37.5665,
                        126.9780,
                        NearbyGhostCourseSortBy.DISTANCE,
                        3000,
                        0,
                        10
                ).stream()
                .map(row -> row.courseId())
                .toList();

        long totalCount = courseRepository.countNearbyGhostCourses(
                List.of(new PreferredDistanceRange(4800, 5200)),
                37.5665,
                126.9780,
                3000
        );

        assertThat(rowCourseIds).containsExactly(officialCourse.getId());
        assertThat(totalCount).isEqualTo(1L);
    }

    @DisplayName("복수 선호 거리 중 하나에 맞으면 공식 코스 후보로 조회한다")
    @Test
    void filtersNearbyCourseCandidatesByAnyPreferredDistance() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 11, 4, 0);

        Course shortCourse = persistCourse("Short Course", 3000, CourseStatus.OFFICIAL, createdAt);
        Course longCourse = persistCourse("Long Course", 9800, CourseStatus.OFFICIAL, createdAt);
        persistCourse("Unmatched Course", 7000, CourseStatus.OFFICIAL, createdAt);

        List<NearbyRecommendationCandidateRow> results =
                findNearbyRecommendationCandidates(List.of(
                        new PreferredDistanceRange(2800, 3200),
                        new PreferredDistanceRange(9600, 10000)
                ));

        assertThat(results)
                .extracting(NearbyRecommendationCandidateRow::courseId)
                .containsExactlyInAnyOrder(shortCourse.getId(), longCourse.getId());
    }

    @DisplayName("중복 코스 후보 조회는 공개 코스(OFFICIAL/COMMUNITY)만 대상으로 판정한다")
    @Test
    void detectsPublicDuplicateCourseOnlyForOfficialAndCommunity() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 4, 7, 10, 0);
        persistCourse("Official Duplicate Candidate", 5000, CourseStatus.OFFICIAL, createdAt);
        persistCourse("Community Duplicate Candidate", 5000, CourseStatus.COMMUNITY, createdAt);
        persistCourse("Private Duplicate Candidate", 5000, CourseStatus.PRIVATE, createdAt);

        boolean duplicateExists = courseRepository.existsPublicDuplicateCourse(
                37.5665,
                126.9780,
                37.5700,
                126.9820,
                5000,
                150,
                0.12
        );

        assertThat(duplicateExists).isTrue();
    }

    @DisplayName("중복 코스 후보 조회는 private 코스만 존재하면 중복으로 판정하지 않는다")
    @Test
    void doesNotDetectDuplicateCourseWhenOnlyPrivateMatches() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 4, 7, 10, 30);
        persistCourse("Private Duplicate Candidate", 5000, CourseStatus.PRIVATE, createdAt);

        boolean duplicateExists = courseRepository.existsPublicDuplicateCourse(
                37.5665,
                126.9780,
                37.5700,
                126.9820,
                5000,
                150,
                0.12
        );

        assertThat(duplicateExists).isFalse();
    }

    private List<NearbyRecommendationCandidateRow> findNearbyRecommendationCandidates(
            List<PreferredDistanceRange> preferredDistanceMs
    ) {
        return courseRepository.findNearbyRecommendationCandidates(
                preferredDistanceMs,
                37.5665,
                126.9780,
                37.5665,
                126.9780,
                3000
        );
    }

    private Course persistCourse(
            String title,
            int distanceM,
            CourseStatus status,
            LocalDateTime createdAt
    ) {
        Course course = Course.builder()
                .title(title)
                .status(status)
                .distanceM(distanceM)
                .elevationGainM(40)
                .snapshotImageUrl("snapshot-" + title)
                .startLatitude(37.5665)
                .startLongitude(126.9780)
                .endLatitude(37.5700)
                .endLongitude(126.9820)
                .coordinates(new Coordinates(List.of(new GeoPoint(37.5665, 126.9780, 12.0))))
                .build();
        setCreatedAt(course, createdAt);
        course = courseRepository.save(course);

        CourseDifficulty courseDifficulty = CourseDifficulty.create(course, Difficulty.MEDIUM);
        CourseTypeMap courseTypeMap = CourseTypeMap.create(course, CourseType.PARK);
        setCreatedAt(courseDifficulty, createdAt);
        setCreatedAt(courseTypeMap, createdAt);

        entityManager.persist(courseDifficulty);
        entityManager.persist(courseTypeMap);
        entityManager.flush();
        entityManager.clear();
        return course;
    }

    private void setCreatedAt(Object entity, LocalDateTime createdAt) {
        try {
            Field field = entity.getClass().getSuperclass().getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(entity, createdAt);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to set createdAt", exception);
        }
    }

    private void updateCoordinatesJson(Long courseId, String coordinatesJson) {
        entityManager.createNativeQuery(
                        "update courses set coordinates = JSON '"
                                + coordinatesJson.replace("'", "''")
                                + "' where course_id = ?"
                )
                .setParameter(1, courseId)
                .executeUpdate();
        entityManager.flush();
    }
}
