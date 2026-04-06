package kr.withrun.was.domain.course.entity;

import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.domain.course.vo.Coordinates;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.global.common.type.Difficulty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("코스 엔티티 팩토리")
class CourseEntityFactoryTest {

    @DisplayName("빌더 API로 코스를 생성한다")
    @Test
    void buildsCourseWithReadableBuilder() {
        Coordinates coordinates = new Coordinates(
                List.of(37.5665, 37.5668),
                List.of(126.9780, 126.9791),
                List.of(12.0, 15.5)
        );
        LocalDateTime promotedAt = LocalDateTime.of(2026, 3, 11, 9, 0);
        User user = newUser();

        Course course = Course.builder()
                .title("Han River Loop")
                .status(CourseStatus.OFFICIAL)
                .distanceM(10000)
                .elevationGainM(35)
                .snapshotImageUrl("https://example.com/course.jpg")
                .startLatitude(37.5665)
                .startLongitude(126.9780)
                .endLatitude(37.5701)
                .endLongitude(126.9825)
                .coordinates(coordinates)
                .promotedAt(promotedAt)
                .user(user)
                .build();

        assertThat(course.getTitle()).isEqualTo("Han River Loop");
        assertThat(course.getStatus()).isEqualTo(CourseStatus.OFFICIAL);
        assertThat(course.getDistanceM()).isEqualTo(10000);
        assertThat(course.getElevationGainM()).isEqualTo(35);
        assertThat(course.getSnapshotImageUrl()).isEqualTo("https://example.com/course.jpg");
        assertThat(course.getStartLatitude()).isEqualTo(37.5665);
        assertThat(course.getStartLongitude()).isEqualTo(126.9780);
        assertThat(course.getEndLatitude()).isEqualTo(37.5701);
        assertThat(course.getEndLongitude()).isEqualTo(126.9825);
        assertThat(course.getCoordinates()).isEqualTo(coordinates);
        assertThat(course.getPromotedAt()).isEqualTo(promotedAt);
        assertThat(course.getUser()).isSameAs(user);
    }

    @DisplayName("팩토리 메서드로 연관 엔티티를 생성한다")
    @Test
    void createsAssociatedEntitiesThroughFactories() {
        Course course = Course.builder()
                .title("Test Course")
                .status(CourseStatus.OFFICIAL)
                .distanceM(5000)
                .elevationGainM(20)
                .startLatitude(37.5)
                .startLongitude(127.0)
                .coordinates(new Coordinates(List.of(37.5), List.of(127.0), List.of(8.0)))
                .user(newUser())
                .build();
        User user = newUser();

        CourseDifficulty courseDifficulty = CourseDifficulty.create(course, Difficulty.MEDIUM);
        CourseTypeMap courseTypeMap = CourseTypeMap.create(course, CourseType.PARK);
        CourseBookmark courseBookmark = CourseBookmark.create(course, user);
        CourseLike courseLike = CourseLike.create(course, user);
        CourseReview courseReview = CourseReview.create(course, user, 5, Difficulty.HARD);
        CourseReviewCourseType reviewCourseType = CourseReviewCourseType.create(courseReview, CourseType.RIVERSIDE);

        assertThat(courseDifficulty.getCourse()).isSameAs(course);
        assertThat(courseDifficulty.getDifficulty()).isEqualTo(Difficulty.MEDIUM);

        assertThat(courseTypeMap.getCourse()).isSameAs(course);
        assertThat(courseTypeMap.getCourseType()).isEqualTo(CourseType.PARK);

        assertThat(courseBookmark.getCourse()).isSameAs(course);
        assertThat(courseBookmark.getUser()).isSameAs(user);

        assertThat(courseLike.getCourse()).isSameAs(course);
        assertThat(courseLike.getUser()).isSameAs(user);

        assertThat(courseReview.getCourse()).isSameAs(course);
        assertThat(courseReview.getUser()).isSameAs(user);
        assertThat(courseReview.getRating()).isEqualTo(5);
        assertThat(courseReview.getDifficulty()).isEqualTo(Difficulty.HARD);

        assertThat(reviewCourseType.getCourseReview()).isSameAs(courseReview);
        assertThat(reviewCourseType.getCourseType()).isEqualTo(CourseType.RIVERSIDE);
    }

    private User newUser() {
        try {
            Constructor<User> constructor = User.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to instantiate test user", exception);
        }
    }
}
