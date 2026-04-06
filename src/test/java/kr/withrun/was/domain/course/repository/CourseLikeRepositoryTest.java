package kr.withrun.was.domain.course.repository;

import jakarta.persistence.EntityManager;
import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.entity.CourseLike;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.vo.Coordinates;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.type.Gender;
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
        "spring.datasource.url=jdbc:h2:mem:course-like-repository;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import(QuerydslConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("코스 좋아요 리포지토리")
class CourseLikeRepositoryTest {

    @Autowired
    private CourseLikeRepository courseLikeRepository;

    @Autowired
    private EntityManager entityManager;

    @DisplayName("코스별 좋아요 row 수를 집계한다")
    @Test
    void countsRowsForCourse() {
        User user = saveUser(1L, "runner-a");
        User anotherUser = saveUser(2L, "runner-b");
        Course targetCourse = saveCourse(1L, "target-course");
        Course otherCourse = saveCourse(2L, "other-course");

        saveCourseLike(targetCourse, user);
        saveCourseLike(targetCourse, anotherUser);
        saveCourseLike(targetCourse, saveUser(3L, "runner-c"));
        saveCourseLike(otherCourse, user);
        entityManager.flush();
        entityManager.clear();

        long likeCount = courseLikeRepository.countByCourseId(targetCourse.getId());

        assertThat(likeCount).isEqualTo(3L);
    }

    private User saveUser(long createdAtHour, String nickname) {
        User user = instantiate(User.class);
        setField(user, "nickname", nickname);
        setField(user, "birthDate", LocalDate.of(1999, 1, 1));
        setField(user, "gender", Gender.MALE);
        setField(user, "height", 175.0);
        setField(user, "weight", 65.0);
        setBaseField(user, "createdAt", LocalDateTime.of(2026, 3, 11, (int) createdAtHour, 0));
        entityManager.persist(user);
        return user;
    }

    private Course saveCourse(long createdAtHour, String title) {
        Course course = Course.builder()
                .title(title)
                .status(CourseStatus.COMMUNITY)
                .distanceM(8000)
                .elevationGainM(150)
                .snapshotImageUrl("snapshot")
                .startLatitude(37.5)
                .startLongitude(127.0)
                .endLatitude(37.6)
                .endLongitude(127.1)
                .coordinates(new Coordinates(List.of(37.5, 37.6), List.of(127.0, 127.1), List.of(10.0, 12.0)))
                .build();
        setBaseField(course, "createdAt", LocalDateTime.of(2026, 3, 11, (int) createdAtHour, 0));
        entityManager.persist(course);
        return course;
    }

    private void saveCourseLike(Course course, User user) {
        CourseLike courseLike = CourseLike.create(course, user);
        entityManager.persist(courseLike);
    }

    private <T> T instantiate(Class<T> type) {
        try {
            java.lang.reflect.Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to instantiate " + type.getSimpleName(), exception);
        }
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to set field " + fieldName, exception);
        }
    }

    private void setBaseField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getSuperclass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to set base field " + fieldName, exception);
        }
    }
}
