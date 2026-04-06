package kr.withrun.was.domain.course.repository;

import jakarta.persistence.EntityManager;
import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.repository.query.CourseCustomRepository;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.vo.Coordinates;
import kr.withrun.was.global.config.JpaAuditingConfig;
import kr.withrun.was.global.config.QuerydslConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.config.import=",
        "spring.cloud.aws.parameterstore.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:course-repository;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({JpaAuditingConfig.class, QuerydslConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("코스 리포지토리")
class CourseRepositoryTest {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EntityManager entityManager;

    @DisplayName("삭제되지 않은 코스 조회 메서드는 QueryDSL fragment에 선언되어 있다")
    @Test
    void declaresFindNotDeletedCourseInQueryFragment() {
        assertThat(courseRepository).isInstanceOf(CourseCustomRepository.class);

        Method method = findMethod("findNotDeletedCourse", Long.class);

        assertThat(method.getReturnType()).isEqualTo(java.util.Optional.class);
    }

    @DisplayName("삭제되지 않은 코스를 조회한다")
    @Test
    void findsNotDeletedCourse() {
        String title = "Active Course";
        persistCourse(title);
        entityManager.flush();
        Long courseId = findCourseIdByTitle(title);
        entityManager.clear();

        Course foundCourse = courseRepository.findNotDeletedCourse(courseId)
                .orElseThrow();

        assertThat(readId(foundCourse)).isEqualTo(courseId);
    }

    @DisplayName("삭제된 코스는 조회하지 않는다")
    @Test
    void doesNotFindDeletedCourse() {
        String title = "Deleted Course";
        Course course = persistCourse(title);
        course.delete();
        entityManager.flush();
        Long courseId = findCourseIdByTitle(title);
        entityManager.clear();

        assertThat(courseRepository.findNotDeletedCourse(courseId)).isEmpty();
    }

    private Course persistCourse(String title) {
        Course course = instantiateCourse();
        setField(course, "title", title);
        setField(course, "status", CourseStatus.OFFICIAL);
        setField(course, "distanceM", 5000);
        setField(course, "elevationGainM", 40);
        setField(course, "snapshotImageUrl", "snapshot");
        setField(course, "startLatitude", 37.5665);
        setField(course, "startLongitude", 126.9780);
        setField(course, "endLatitude", 37.5700);
        setField(course, "endLongitude", 126.9820);
        setField(course, "coordinates", new Coordinates(List.of(37.5665), List.of(126.9780), List.of(10.0)));
        entityManager.persist(course);
        return course;
    }

    private Course instantiateCourse() {
        try {
            Constructor<Course> constructor = Course.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to instantiate course", exception);
        }
    }

    private Method findMethod(String name, Class<?>... parameterTypes) {
        try {
            return CourseCustomRepository.class.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException exception) {
            throw new AssertionError("Expected method not found: " + name, exception);
        }
    }

    private Long readId(Course course) {
        return (Long) entityManager.getEntityManagerFactory()
                .getPersistenceUnitUtil()
                .getIdentifier(course);
    }

    private Long findCourseIdByTitle(String title) {
        Number courseId = (Number) entityManager.createNativeQuery(
                        "select course_id from courses where title = ?"
                )
                .setParameter(1, title)
                .getSingleResult();
        return courseId.longValue();
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
