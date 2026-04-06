package kr.withrun.was.domain.course.repository;

import jakarta.persistence.EntityManager;
import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.entity.CourseBookmark;
import kr.withrun.was.domain.course.entity.CourseDifficulty;
import kr.withrun.was.domain.course.entity.CourseLike;
import kr.withrun.was.domain.course.entity.CourseTypeMap;
import kr.withrun.was.domain.course.repository.query.CourseBookmarkCustomRepository;
import kr.withrun.was.domain.course.repository.query.dto.BookmarkedCourseRow;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.domain.course.vo.Coordinates;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.type.Gender;
import kr.withrun.was.global.common.entity.BaseEntity;
import kr.withrun.was.global.common.type.Difficulty;
import kr.withrun.was.global.config.JpaAuditingConfig;
import kr.withrun.was.global.config.QuerydslConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.config.import=",
        "spring.cloud.aws.parameterstore.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:course-bookmark;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({JpaAuditingConfig.class, QuerydslConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("코스 북마크 리포지토리")
class CourseBookmarkRepositoryTest {

    @Autowired
    private CourseBookmarkRepository courseBookmarkRepository;

    @Autowired
    private EntityManager entityManager;

    @DisplayName("코스와 사용자 ID로 활성 북마크를 조회한다")
    @Test
    void findsActiveBookmarkByCourseIdAndUserId() {
        assertThat(courseBookmarkRepository).isInstanceOf(CourseBookmarkCustomRepository.class);
        assertThat(CourseBookmark.class.getSuperclass()).isNotEqualTo(BaseEntity.class);

        User user = persistUser("runner-a");
        Course course = persistCourse("bookmark-course-a", CourseStatus.OFFICIAL);
        CourseBookmark bookmark = courseBookmarkRepository.save(CourseBookmark.create(course, user));

        entityManager.flush();
        entityManager.clear();

        CourseBookmark foundBookmark = courseBookmarkRepository
                .findByCourseIdAndUserId(course.getId(), user.getId())
                .orElseThrow();

        assertThat(foundBookmark.getId()).isEqualTo(bookmark.getId());
        assertThat(foundBookmark.getCreatedAt()).isNotNull();
    }

    @DisplayName("비공개 코스 북마크도 코스와 사용자 ID로 조회한다")
    @Test
    void findsPrivateBookmarkByCourseIdAndUserId() {
        User user = persistUser("runner-private");
        Course privateCourse = persistCourse("bookmark-course-private", CourseStatus.PRIVATE);
        CourseBookmark bookmark = courseBookmarkRepository.save(CourseBookmark.create(privateCourse, user));

        entityManager.flush();
        entityManager.clear();

        CourseBookmark foundBookmark = courseBookmarkRepository.findByCourseIdAndUserId(privateCourse.getId(), user.getId())
                .orElseThrow();

        assertThat(foundBookmark.getId()).isEqualTo(bookmark.getId());
    }

    @DisplayName("코스와 사용자 ID로 북마크를 물리 삭제한다")
    @Test
    void deletesBookmarkPhysicallyByCourseIdAndUserId() {
        User user = persistUser("runner-b");
        Course course = persistCourse("bookmark-course-b", CourseStatus.OFFICIAL);
        courseBookmarkRepository.save(CourseBookmark.create(course, user));
        entityManager.flush();

        courseBookmarkRepository.deleteBookmark(course.getId(), user.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(courseBookmarkRepository.findByCourseIdAndUserId(course.getId(), user.getId())).isEmpty();
        assertThat(countBookmarkRows()).isZero();
    }

    @DisplayName("물리 삭제 후 같은 코스와 사용자로 다시 북마크할 수 있다")
    @Test
    void allowsAddRemoveAddWithoutUniqueConstraintProblems() {
        User user = persistUser("runner-c");
        Course course = persistCourse("bookmark-course-c", CourseStatus.OFFICIAL);
        courseBookmarkRepository.save(CourseBookmark.create(course, user));
        entityManager.flush();

        courseBookmarkRepository.deleteBookmark(course.getId(), user.getId());
        entityManager.flush();

        CourseBookmark recreatedBookmark = courseBookmarkRepository.save(CourseBookmark.create(course, user));
        entityManager.flush();
        entityManager.clear();

        assertThat(recreatedBookmark.getId()).isNotNull();
        assertThat(courseBookmarkRepository.findByCourseIdAndUserId(course.getId(), user.getId())).isPresent();
        assertThat(countBookmarkRows()).isEqualTo(1);
    }

    @DisplayName("사용자 북마크 코스 row를 생성 시각 역순과 북마크 ID 역순으로 조회하고 삭제된 데이터는 제외한다")
    @Test
    void findsBookmarkedCourseRowsOrderedAndFiltered() {
        User user = persistUser("runner-d");
        User likedByFirst = persistUser("runner-d-like-1");
        User likedBySecond = persistUser("runner-d-like-2");
        LocalDateTime sharedCreatedAt = LocalDateTime.of(2026, 3, 14, 10, 30);

        Course firstCourse = persistCourse("bookmark-course-d1", CourseStatus.OFFICIAL);
        Course secondCourse = persistCourse("bookmark-course-d2", CourseStatus.COMMUNITY);
        Course deletedCourse = persistCourse("bookmark-course-d3", CourseStatus.OFFICIAL);
        Course privateCourse = persistCourse("bookmark-course-d4", CourseStatus.PRIVATE);
        deletedCourse.delete();

        persistDifficulty(firstCourse, Difficulty.MEDIUM, false);
        persistDifficulty(secondCourse, Difficulty.HARD, false);
        persistDifficulty(deletedCourse, Difficulty.EASY, false);

        persistCourseType(firstCourse, CourseType.RIVERSIDE, false);
        persistCourseType(firstCourse, CourseType.PARK, false);
        persistCourseType(firstCourse, CourseType.OTHER, true);
        persistCourseType(secondCourse, CourseType.URBAN, false);
        persistCourseType(deletedCourse, CourseType.TRACK, false);
        persistCourseType(privateCourse, CourseType.MOUNTAIN_TRAIL, false);

        persistLike(firstCourse, likedByFirst);
        persistLike(secondCourse, user);
        persistLike(secondCourse, likedBySecond);

        CourseBookmark firstBookmark = persistBookmark(firstCourse, user, sharedCreatedAt);
        CourseBookmark secondBookmark = persistBookmark(secondCourse, user, sharedCreatedAt);
        persistBookmark(deletedCourse, user, LocalDateTime.of(2026, 3, 14, 11, 0));
        CourseBookmark privateBookmark = persistBookmark(privateCourse, user, LocalDateTime.of(2026, 3, 14, 12, 0));

        entityManager.flush();
        entityManager.clear();

        List<BookmarkedCourseRow> rows = courseBookmarkRepository.findBookmarkedCourseRows(user.getId(), null, null, 10);

        assertThat(rows).hasSize(3);
        assertThat(rows)
                .extracting(BookmarkedCourseRow::bookmarkId)
                .containsExactly(secondBookmark.getId(), firstBookmark.getId(), firstBookmark.getId());
        assertThat(rows)
                .extracting(BookmarkedCourseRow::courseId)
                .containsExactly(secondCourse.getId(), firstCourse.getId(), firstCourse.getId());
        assertThat(rows)
                .extracting(BookmarkedCourseRow::courseType)
                .containsExactlyInAnyOrder(CourseType.URBAN, CourseType.RIVERSIDE, CourseType.PARK);
        assertThat(rows)
                .extracting(BookmarkedCourseRow::status)
                .containsExactly(CourseStatus.COMMUNITY, CourseStatus.OFFICIAL, CourseStatus.OFFICIAL);
        assertThat(rows)
                .extracting(BookmarkedCourseRow::difficulty)
                .containsExactly(Difficulty.HARD, Difficulty.MEDIUM, Difficulty.MEDIUM);
        assertThat(rows)
                .extracting(BookmarkedCourseRow::likeCount)
                .containsExactly(2L, 1L, 1L);
        assertThat(rows)
                .extracting(BookmarkedCourseRow::isLiked)
                .containsExactly(true, false, false);
        assertThat(rows)
                .extracting(BookmarkedCourseRow::bookmarkId)
                .doesNotContain(privateBookmark.getId());
    }

    @DisplayName("사용자 북마크 코스 row는 page size + 1개의 북마크까지만 조회한다")
    @Test
    void limitsBookmarkedCourseRowsByBookmarkCount() {
        User user = persistUser("runner-e");

        Course newestCourse = persistCourse("bookmark-course-e1", CourseStatus.OFFICIAL);
        Course middleCourse = persistCourse("bookmark-course-e2", CourseStatus.COMMUNITY);
        Course oldestCourse = persistCourse("bookmark-course-e3", CourseStatus.OFFICIAL);

        persistDifficulty(newestCourse, Difficulty.HARD, false);
        persistDifficulty(middleCourse, Difficulty.MEDIUM, false);
        persistDifficulty(oldestCourse, Difficulty.EASY, false);

        persistCourseType(newestCourse, CourseType.RIVERSIDE, false);
        persistCourseType(newestCourse, CourseType.PARK, false);
        persistCourseType(middleCourse, CourseType.URBAN, false);
        persistCourseType(oldestCourse, CourseType.TRACK, false);

        CourseBookmark oldestBookmark = persistBookmark(oldestCourse, user, LocalDateTime.of(2026, 3, 14, 9, 0));
        CourseBookmark middleBookmark = persistBookmark(middleCourse, user, LocalDateTime.of(2026, 3, 14, 10, 0));
        CourseBookmark newestBookmark = persistBookmark(newestCourse, user, LocalDateTime.of(2026, 3, 14, 11, 0));

        entityManager.flush();
        entityManager.clear();

        List<BookmarkedCourseRow> rows = courseBookmarkRepository.findBookmarkedCourseRows(user.getId(), null, null, 1);

        assertThat(rows).hasSize(3);
        assertThat(rows)
                .extracting(BookmarkedCourseRow::bookmarkId)
                .containsExactly(newestBookmark.getId(), newestBookmark.getId(), middleBookmark.getId());
        assertThat(rows)
                .extracting(BookmarkedCourseRow::bookmarkId)
                .doesNotContain(oldestBookmark.getId());
    }

    private long countBookmarkRows() {
        Number count = (Number) entityManager.createNativeQuery("select count(*) from course_bookmarks")
                .getSingleResult();
        return count.longValue();
    }

    private User persistUser(String nickname) {
        User user = instantiateUser();
        setField(user, "nickname", nickname);
        setField(user, "birthDate", LocalDate.of(1995, 3, 11));
        setField(user, "gender", Gender.MALE);
        setField(user, "height", 175.0);
        setField(user, "weight", 68.0);
        entityManager.persist(user);
        return user;
    }

    private User instantiateUser() {
        try {
            java.lang.reflect.Constructor<User> constructor = User.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to instantiate user", exception);
        }
    }

    private Course persistCourse(String title, CourseStatus status) {
        Course course = Course.builder()
                .title(title)
                .status(status)
                .distanceM(5000)
                .elevationGainM(80)
                .snapshotImageUrl("snapshot")
                .startLatitude(37.5665)
                .startLongitude(126.9780)
                .endLatitude(37.5700)
                .endLongitude(126.9820)
                .coordinates(new Coordinates(List.of(37.5665), List.of(126.9780), List.of(10.0)))
                .build();
        entityManager.persist(course);
        return course;
    }

    private CourseDifficulty persistDifficulty(Course course, Difficulty difficulty, boolean deleted) {
        CourseDifficulty courseDifficulty = CourseDifficulty.create(course, difficulty);
        if (deleted) {
            courseDifficulty.delete();
        }
        entityManager.persist(courseDifficulty);
        return courseDifficulty;
    }

    private CourseTypeMap persistCourseType(Course course, CourseType courseType, boolean deleted) {
        CourseTypeMap courseTypeMap = CourseTypeMap.create(course, courseType);
        if (deleted) {
            courseTypeMap.delete();
        }
        entityManager.persist(courseTypeMap);
        return courseTypeMap;
    }

    private CourseBookmark persistBookmark(Course course, User user, LocalDateTime createdAt) {
        CourseBookmark bookmark = CourseBookmark.create(course, user);
        setField(bookmark, "createdAt", createdAt);
        entityManager.persist(bookmark);
        return bookmark;
    }

    private CourseLike persistLike(Course course, User user) {
        CourseLike courseLike = CourseLike.create(course, user);
        entityManager.persist(courseLike);
        return courseLike;
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
