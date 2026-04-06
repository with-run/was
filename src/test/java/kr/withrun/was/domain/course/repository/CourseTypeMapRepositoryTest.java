package kr.withrun.was.domain.course.repository;

import jakarta.persistence.EntityManager;
import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.entity.CourseTypeMap;
import kr.withrun.was.domain.course.repository.query.CourseTypeMapCustomRepository;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.domain.course.vo.Coordinates;
import kr.withrun.was.global.config.JpaAuditingConfig;
import kr.withrun.was.global.config.QuerydslConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.config.import=",
        "spring.cloud.aws.parameterstore.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:course-type-map-repository;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({JpaAuditingConfig.class, QuerydslConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("코스 타입 리포지토리")
class CourseTypeMapRepositoryTest {

    @Autowired
    private CourseTypeMapRepository courseTypeMapRepository;

    @Autowired
    private EntityManager entityManager;

    @DisplayName("활성 코스 타입 목록을 중복 없이 QueryDSL fragment로 조회한다")
    @Test
    void findsDistinctActiveCourseTypesByCourseId() {
        assertThat(courseTypeMapRepository).isInstanceOf(CourseTypeMapCustomRepository.class);

        Course course = saveCourse("course-type-course");
        entityManager.persist(CourseTypeMap.create(course, CourseType.PARK));
        entityManager.persist(CourseTypeMap.create(course, CourseType.RIVERSIDE));
        entityManager.persist(CourseTypeMap.create(course, CourseType.PARK));
        entityManager.flush();
        entityManager.clear();

        assertThat(courseTypeMapRepository.findCourseTypesByCourseId(course.getId()))
                .containsExactlyInAnyOrder(CourseType.PARK, CourseType.RIVERSIDE);
    }

    @DisplayName("삭제된 코스 타입 매핑은 조회하지 않는다")
    @Test
    void doesNotFindDeletedCourseTypeMaps() {
        Course course = saveCourse("deleted-course-type-course");
        CourseTypeMap deletedCourseTypeMap = CourseTypeMap.create(course, CourseType.TRACK);
        deletedCourseTypeMap.delete();
        entityManager.persist(deletedCourseTypeMap);
        entityManager.flush();
        entityManager.clear();

        assertThat(courseTypeMapRepository.findCourseTypesByCourseId(course.getId())).isEmpty();
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
}
