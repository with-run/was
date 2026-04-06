package kr.withrun.was.domain.course.repository;

import jakarta.persistence.EntityManager;
import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.entity.UserCourseInteraction;
import kr.withrun.was.domain.course.repository.query.dto.CourseCollaborativeScoreRow;
import kr.withrun.was.domain.course.type.CourseInteractionType;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.vo.Coordinates;
import kr.withrun.was.domain.user.entity.User;
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
        "spring.datasource.url=jdbc:h2:mem:user-course-interaction;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({JpaAuditingConfig.class, QuerydslConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("사용자 코스 상호작용 리포지토리")
class UserCourseInteractionRepositoryTest {

    @Autowired
    private UserCourseInteractionRepository userCourseInteractionRepository;

    @Autowired
    private EntityManager entityManager;

    @DisplayName("최근 상호작용 코스 조회는 private 코스를 제외한다")
    @Test
    void excludesPrivateCoursesFromRecentInteractedCourseIds() {
        User user = persistUser("runner-a");
        Course publicCourse = persistCourse("public-course", CourseStatus.OFFICIAL);
        Course privateCourse = persistCourse("private-course", CourseStatus.PRIVATE);

        persistInteraction(user, publicCourse, LocalDateTime.of(2026, 3, 20, 9, 0), "like", "1");
        persistInteraction(user, privateCourse, LocalDateTime.of(2026, 3, 20, 10, 0), "bookmark", "2");
        entityManager.flush();
        entityManager.clear();

        List<Long> courseIds = userCourseInteractionRepository.findRecentInteractedCourseIds(user.getId(), 10);

        assertThat(courseIds).containsExactly(publicCourse.getId());
    }

    @DisplayName("협업 점수 조회는 private seed 와 private candidate 를 모두 제외한다")
    @Test
    void excludesPrivateCoursesFromCollaborativeScoreRows() {
        User currentUser = persistUser("current-user");
        User neighborByPublicSeed = persistUser("neighbor-public");
        User neighborByPrivateSeed = persistUser("nbr-private-seed");
        User neighborForPrivateCandidate = persistUser("nbr-private-cand");

        Course publicSeed = persistCourse("public-seed", CourseStatus.OFFICIAL);
        Course privateSeed = persistCourse("private-seed", CourseStatus.PRIVATE);
        Course publicCandidate = persistCourse("public-candidate", CourseStatus.COMMUNITY);
        Course privateCandidate = persistCourse("private-candidate", CourseStatus.PRIVATE);

        persistInteraction(neighborByPublicSeed, publicSeed, LocalDateTime.of(2026, 3, 20, 9, 0), "seed-public", "1");
        persistInteraction(neighborByPublicSeed, publicCandidate, LocalDateTime.of(2026, 3, 20, 9, 5), "candidate-public", "2");

        persistInteraction(neighborByPrivateSeed, privateSeed, LocalDateTime.of(2026, 3, 20, 9, 10), "seed-private", "3");
        persistInteraction(neighborByPrivateSeed, publicCandidate, LocalDateTime.of(2026, 3, 20, 9, 15), "candidate-public", "4");

        persistInteraction(neighborForPrivateCandidate, publicSeed, LocalDateTime.of(2026, 3, 20, 9, 20), "seed-public", "5");
        persistInteraction(neighborForPrivateCandidate, privateCandidate, LocalDateTime.of(2026, 3, 20, 9, 25), "candidate-private", "6");

        entityManager.flush();
        entityManager.clear();

        List<CourseCollaborativeScoreRow> rows = userCourseInteractionRepository.findCollaborativeScoreRows(
                currentUser.getId(),
                List.of(publicSeed.getId(), privateSeed.getId()),
                List.of(publicCandidate.getId(), privateCandidate.getId())
        );

        assertThat(rows).containsExactly(new CourseCollaborativeScoreRow(publicCandidate.getId(), 1L));
    }

    private User persistUser(String nickname) {
        User user = User.createPendingSocialUser(nickname);
        setField(user, "birthDate", LocalDate.of(1995, 3, 11));
        entityManager.persist(user);
        return user;
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

    private void persistInteraction(User user, Course course, LocalDateTime occurredAt, String source, String sourceRef) {
        entityManager.persist(UserCourseInteraction.create(
                user,
                course,
                CourseInteractionType.BOOKMARK,
                1,
                occurredAt,
                source,
                sourceRef
        ));
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
