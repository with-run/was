package kr.withrun.was.domain.course.repository;

import jakarta.persistence.EntityManager;
import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.entity.CourseGhostLeaderboard;
import kr.withrun.was.domain.course.repository.query.dto.CourseGhostLeaderboardRankRow;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.vo.Coordinates;
import kr.withrun.was.domain.course.vo.GeoPoint;
import kr.withrun.was.domain.running.entity.RunningSession;
import kr.withrun.was.domain.running.type.RunningMode;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.type.Gender;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.config.import=",
        "spring.cloud.aws.parameterstore.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:course-ghost-leaderboard;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({JpaAuditingConfig.class, QuerydslConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("코스 고스트 리더보드 리포지토리")
class CourseGhostLeaderboardRepositoryTest {

    @Autowired
    private CourseGhostLeaderboardRepository courseGhostLeaderboardRepository;

    @Autowired
    private EntityManager entityManager;

    @DisplayName("코스 기준 전체 고스트 리더보드를 정렬과 rank를 유지한 채 조회한다")
    @Test
    void findsAllRankedRowsByCourseId() {
        Course course = persistCourse("Leader Course");
        User userA = persistUser("runner-a");
        User userB = persistUser("runner-b");
        User userC = persistUser("runner-c");

        persistLeaderboard(course, userA, 500, LocalDateTime.of(2026, 3, 18, 10, 0));
        persistLeaderboard(course, userB, 300, LocalDateTime.of(2026, 3, 18, 10, 1));
        persistLeaderboard(course, userC, 300, LocalDateTime.of(2026, 3, 18, 10, 2));

        entityManager.flush();
        entityManager.clear();

        List<CourseGhostLeaderboardRankRow> rows = courseGhostLeaderboardRepository.findRankedRowsByCourseId(course.getId());

        assertThat(rows).hasSize(3);
        assertThat(rows).extracting(CourseGhostLeaderboardRankRow::point).containsExactly(500, 300, 300);
        assertThat(rows).extracting(CourseGhostLeaderboardRankRow::rank).containsExactly(1L, 2L, 2L);
        assertThat(rows).extracting(CourseGhostLeaderboardRankRow::nickname).containsExactly("runner-a", "runner-b", "runner-c");
    }

    private Course persistCourse(String title) {
        Course course = Course.builder()
                .title(title)
                .status(CourseStatus.OFFICIAL)
                .distanceM(5000)
                .elevationGainM(100)
                .snapshotImageUrl("https://example.com/course.jpg")
                .startLatitude(37.5665)
                .startLongitude(126.9780)
                .endLatitude(37.5700)
                .endLongitude(126.9820)
                .coordinates(new Coordinates(List.of(
                        new GeoPoint(37.5665, 126.9780, 10.0),
                        new GeoPoint(37.5700, 126.9820, 15.0)
                )))
                .build();
        entityManager.persist(course);
        return course;
    }

    private User persistUser(String nickname) {
        User user = instantiate(User.class);
        setField(user, "nickname", nickname);
        setField(user, "birthDate", LocalDate.of(1995, 3, 11));
        setField(user, "gender", Gender.MALE);
        setField(user, "height", 175.0);
        setField(user, "weight", 68.0);
        entityManager.persist(user);
        return user;
    }

    private CourseGhostLeaderboard persistLeaderboard(
            Course course,
            User user,
            int point,
            LocalDateTime startedAt
    ) {
        RunningSession runningSession = RunningSession.start(
                user,
                RunningMode.COURSE,
                course,
                null,
                37.5,
                127.0
        );
        entityManager.persist(runningSession);

        CourseGhostLeaderboard leaderboard = CourseGhostLeaderboard.create(user, course, runningSession, point);
        entityManager.persist(leaderboard);
        return leaderboard;
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
