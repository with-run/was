package kr.withrun.was.domain.course.repository;

import jakarta.persistence.EntityManager;
import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.entity.CourseReview;
import kr.withrun.was.domain.course.repository.query.CourseReviewCustomRepository;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.vo.Coordinates;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.type.Gender;
import kr.withrun.was.global.common.type.Difficulty;
import kr.withrun.was.global.config.JpaAuditingConfig;
import kr.withrun.was.global.config.QuerydslConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
        "spring.config.import=",
        "spring.cloud.aws.parameterstore.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:course-review-repository;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({JpaAuditingConfig.class, QuerydslConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("코스 리뷰 리포지토리")
class CourseReviewRepositoryTest {

    @Autowired
    private CourseReviewRepository courseReviewRepository;

    @Autowired
    private EntityManager entityManager;

    @DisplayName("활성 리뷰 평점의 평균을 QueryDSL fragment로 조회한다")
    @Test
    void findsAverageRatingByCourseId() {
        assertThat(courseReviewRepository).isInstanceOf(CourseReviewCustomRepository.class);

        Course course = saveCourse("review-course");
        User firstUser = saveUser("runner-a");
        User secondUser = saveUser("runner-b");
        entityManager.persist(CourseReview.create(course, firstUser, 4, Difficulty.MEDIUM));
        entityManager.persist(CourseReview.create(course, secondUser, 5, Difficulty.HARD));
        entityManager.flush();
        entityManager.clear();

        assertThat(courseReviewRepository.findAverageRatingByCourseId(course.getId())).isEqualTo(4.5);
    }

    @DisplayName("삭제된 리뷰는 평균 계산에서 제외한다")
    @Test
    void excludesDeletedReviewsFromAverage() {
        Course course = saveCourse("deleted-review-course");
        User firstUser = saveUser("runner-c");
        User secondUser = saveUser("runner-d");
        entityManager.persist(CourseReview.create(course, firstUser, 4, Difficulty.MEDIUM));
        CourseReview deletedReview = CourseReview.create(course, secondUser, 1, Difficulty.EASY);
        deletedReview.delete();
        entityManager.persist(deletedReview);
        entityManager.flush();
        entityManager.clear();

        assertThat(courseReviewRepository.findAverageRatingByCourseId(course.getId())).isEqualTo(4.0);
    }

    @DisplayName("리뷰 중복 여부 조회는 삭제된 리뷰도 포함한다")
    @Test
    void includesDeletedReviewsInDuplicateCheck() {
        assertThat(courseReviewRepository).isInstanceOf(CourseReviewCustomRepository.class);

        Course course = saveCourse("duplicate-check-course");
        User user = saveUser("runner-e");
        CourseReview deletedReview = CourseReview.create(course, user, 3, Difficulty.MEDIUM);
        deletedReview.delete();
        entityManager.persist(deletedReview);
        entityManager.flush();
        entityManager.clear();

        assertThat(courseReviewRepository.existsByCourseIdAndUserId(course.getId(), user.getId()))
                .isTrue();
    }

    @DisplayName("삭제된 리뷰가 있어도 같은 사용자와 코스로 새 리뷰를 다시 저장할 수 없다")
    @Test
    void preventsSavingReviewHistoryAgainAfterDelete() {
        Course course = saveCourse("review-history-course");
        User user = saveUser("runner-history");
        CourseReview deletedReview = CourseReview.create(course, user, 3, Difficulty.MEDIUM);
        deletedReview.delete();
        courseReviewRepository.saveAndFlush(deletedReview);

        CourseReview rewrittenReview = CourseReview.create(course, user, 5, Difficulty.HARD);

        assertThatThrownBy(() -> courseReviewRepository.saveAndFlush(rewrittenReview))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @DisplayName("활성 리뷰 중복 여부 조회는 커스텀 QueryDSL fragment에 정의된다")
    @Test
    void declaresDuplicateCheckInCustomRepositoryFragment() throws NoSuchMethodException {
        assertThat(CourseReviewCustomRepository.class.getMethod(
                "existsByCourseIdAndUserId",
                Long.class,
                Long.class
        )).isNotNull();
    }

    @DisplayName("새 리뷰가 저장되면 평균 평점 조회에 즉시 반영된다")
    @Test
    void reflectsNewReviewInAverageRatingImmediately() {
        Course course = saveCourse("updated-average-course");
        User firstUser = saveUser("runner-f");
        User secondUser = saveUser("runner-g");
        entityManager.persist(CourseReview.create(course, firstUser, 4, Difficulty.MEDIUM));
        entityManager.flush();

        assertThat(courseReviewRepository.findAverageRatingByCourseId(course.getId())).isEqualTo(4.0);

        entityManager.persist(CourseReview.create(course, secondUser, 2, Difficulty.EASY));
        entityManager.flush();
        entityManager.clear();

        assertThat(courseReviewRepository.findAverageRatingByCourseId(course.getId())).isEqualTo(3.0);
    }

    private Course saveCourse(String title) {
        Course course = Course.builder()
                .title(title)
                .status(CourseStatus.OFFICIAL)
                .distanceM(5000)
                .elevationGainM(40)
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

    private User saveUser(String nickname) {
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
