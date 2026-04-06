package kr.withrun.was.domain.course.dto;

import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.domain.course.type.RouteType;
import kr.withrun.was.domain.course.vo.Coordinates;
import kr.withrun.was.global.common.type.Difficulty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("코스 고스트 상세 응답 DTO")
class CourseGhostDetailResponseTest {

    @DisplayName("팩토리 메서드는 이미 조회된 코스와 부가 정보를 응답으로 투영한다")
    @Test
    void projectsResolvedValuesIntoGhostDetailResponse() {
        Course course = course(12L);
        CourseGhostDetailResponse.MyRecord myRecord = new CourseGhostDetailResponse.MyRecord(
                205L,
                2002L,
                1320,
                980,
                14L,
                LocalDateTime.of(2026, 3, 20, 7, 10)
        );

        CourseGhostDetailResponse response = CourseGhostDetailResponse.of(
                course,
                Difficulty.MEDIUM,
                List.of(CourseType.RIVERSIDE, CourseType.PARK),
                128L,
                false,
                false,
                4.3,
                myRecord
        );

        assertThat(response.courseId()).isEqualTo(12L);
        assertThat(response.title()).isEqualTo("Ghost Course 12");
        assertThat(response.status()).isEqualTo(CourseStatus.OFFICIAL);
        assertThat(response.routeType()).isEqualTo(RouteType.LOOP);
        assertThat(response.difficulty()).isEqualTo(new CourseGhostDetailResponse.DifficultyOption("MEDIUM", "보통"));
        assertThat(response.courseTypes()).containsExactly(
                new CourseGhostDetailResponse.CourseTypeOption("RIVERSIDE", "강변"),
                new CourseGhostDetailResponse.CourseTypeOption("PARK", "공원")
        );
        assertThat(response.likeCount()).isEqualTo(128L);
        assertThat(response.averageRating()).isEqualTo(4.3);
        assertThat(response.myRecord()).isEqualTo(myRecord);
    }

    private Course course(Long id) {
        Course course = Course.builder()
                .title("Ghost Course " + id)
                .status(CourseStatus.OFFICIAL)
                .distanceM(5200)
                .elevationGainM(48)
                .snapshotImageUrl("https://cdn.withrun.kr/courses/" + id + ".png")
                .startLatitude(37.5661)
                .startLongitude(126.9738)
                .endLatitude(37.5702)
                .endLongitude(126.9814)
                .coordinates(new Coordinates(List.of(37.5661, 37.5702), List.of(126.9738, 126.9814), List.of(12.3, 15.8)))
                .build();
        setField(course, "id", id);
        setField(course, "routeType", RouteType.LOOP);
        return course;
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
