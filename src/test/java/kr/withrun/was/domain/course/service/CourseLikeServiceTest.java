package kr.withrun.was.domain.course.service;

import kr.withrun.was.domain.course.dto.CourseLikeStatusResponse;
import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.repository.CourseLikeRepository;
import kr.withrun.was.domain.course.repository.CourseRepository;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.vo.Coordinates;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.repository.UserRepository;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("코스 좋아요 서비스")
class CourseLikeServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseLikeRepository courseLikeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private User user;

    @Mock
    private CourseSignalService courseSignalService;

    private CourseLikeService courseLikeService;

    @BeforeEach
    void setUp() {
        courseLikeService = new CourseLikeService(courseRepository, courseLikeRepository, userRepository, courseSignalService, new CourseAccessPolicy());
    }

    @DisplayName("서비스는 클래스 기본 readOnly와 쓰기 메서드 트랜잭션을 함께 사용한다")
    @Test
    void usesReadOnlyAtClassLevelAndWritableMethods() throws NoSuchMethodException {
        Transactional classTransactional = CourseLikeService.class.getAnnotation(Transactional.class);
        Transactional likeTransactional = CourseLikeService.class
                .getDeclaredMethod("likeCourse", Long.class, Long.class)
                .getAnnotation(Transactional.class);
        Transactional unlikeTransactional = CourseLikeService.class
                .getDeclaredMethod("unlikeCourse", Long.class, Long.class)
                .getAnnotation(Transactional.class);

        assertThat(classTransactional).isNotNull();
        assertThat(classTransactional.readOnly()).isTrue();
        assertThat(likeTransactional).isNotNull();
        assertThat(likeTransactional.readOnly()).isFalse();
        assertThat(unlikeTransactional).isNotNull();
        assertThat(unlikeTransactional.readOnly()).isFalse();
    }

    @DisplayName("좋아요 이력이 없으면 새 좋아요를 생성한다")
    @Test
    void createsNewLikeWhenHistoryDoesNotExist() {
        Course course = communityCourse();
        givenActiveCourseAndUser(course);
        given(courseLikeRepository.findByCourseIdAndUserId(1L, 2L)).willReturn(Optional.empty());
        given(courseLikeRepository.save(any(kr.withrun.was.domain.course.entity.CourseLike.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(courseLikeRepository.countByCourseId(1L)).willReturn(1L);

        CourseLikeStatusResponse response = courseLikeService.likeCourse(1L, 2L);

        assertThat(response).isEqualTo(new CourseLikeStatusResponse(1L, true, 1L, CourseStatus.COMMUNITY));
        verify(courseLikeRepository).save(any(kr.withrun.was.domain.course.entity.CourseLike.class));
    }

    @DisplayName("기존 좋아요 row가 있으면 추가 생성 없이 기준 수를 넘기면 코스를 공식 코스로 승격한다")
    @Test
    void keepsExistingLikeAndPromotesCommunityCourse() {
        Course course = communityCourse();

        givenActiveCourseAndUser(course);
        given(courseLikeRepository.findByCourseIdAndUserId(1L, 2L)).willReturn(Optional.of(mockExistingLike()));
        given(courseLikeRepository.countByCourseId(1L)).willReturn(10L);

        CourseLikeStatusResponse response = courseLikeService.likeCourse(1L, 2L);

        assertThat(response).isEqualTo(new CourseLikeStatusResponse(1L, true, 10L, CourseStatus.OFFICIAL));
        assertThat(course.getStatus()).isEqualTo(CourseStatus.OFFICIAL);
        assertThat(course.getPromotedAt()).isNotNull();
        verify(courseLikeRepository, never()).save(any(kr.withrun.was.domain.course.entity.CourseLike.class));
    }

    @DisplayName("이미 좋아요 상태여도 추가 레코드 없이 멱등하게 true를 유지한다")
    @Test
    void likeIsIdempotentWhenAlreadyLiked() {
        Course course = communityCourse();

        givenActiveCourseAndUser(course);
        given(courseLikeRepository.findByCourseIdAndUserId(1L, 2L)).willReturn(Optional.of(mockExistingLike()));
        given(courseLikeRepository.countByCourseId(1L)).willReturn(3L);

        CourseLikeStatusResponse response = courseLikeService.likeCourse(1L, 2L);

        assertThat(response).isEqualTo(new CourseLikeStatusResponse(1L, true, 3L, CourseStatus.COMMUNITY));
        verify(courseLikeRepository, never()).save(any(kr.withrun.was.domain.course.entity.CourseLike.class));
    }

    @DisplayName("좋아요 이력이 없으면 취소 요청에도 레코드를 만들지 않고 멱등하게 false를 반환한다")
    @Test
    void unlikeIsIdempotentWhenHistoryDoesNotExist() {
        Course course = communityCourse();
        givenActiveCourseAndUser(course);
        given(courseLikeRepository.findByCourseIdAndUserId(1L, 2L)).willReturn(Optional.empty());
        given(courseLikeRepository.countByCourseId(1L)).willReturn(0L);

        CourseLikeStatusResponse response = courseLikeService.unlikeCourse(1L, 2L);

        assertThat(response).isEqualTo(new CourseLikeStatusResponse(1L, false, 0L, CourseStatus.COMMUNITY));
        verify(courseLikeRepository, never()).save(any(kr.withrun.was.domain.course.entity.CourseLike.class));
    }

    @DisplayName("좋아요 취소는 기존 좋아요 row를 삭제하되 공식 코스를 강등하지 않는다")
    @Test
    void deletesLikeWithoutDemotingOfficialCourse() {
        LocalDateTime promotedAt = LocalDateTime.of(2026, 3, 11, 18, 0);
        Course course = Course.builder()
                .title("Official Course")
                .status(CourseStatus.OFFICIAL)
                .distanceM(10000)
                .elevationGainM(200)
                .snapshotImageUrl("snapshot")
                .startLatitude(37.5)
                .startLongitude(127.0)
                .endLatitude(37.6)
                .endLongitude(127.1)
                .coordinates(new Coordinates(List.of(37.5, 37.6), List.of(127.0, 127.1), List.of(10.0, 12.0)))
                .promotedAt(promotedAt)
                .build();
        kr.withrun.was.domain.course.entity.CourseLike courseLike = mockExistingLike();

        givenActiveCourseAndUser(course);
        given(courseLikeRepository.findByCourseIdAndUserId(1L, 2L)).willReturn(Optional.of(courseLike));
        given(courseLikeRepository.countByCourseId(1L)).willReturn(9L);

        CourseLikeStatusResponse response = courseLikeService.unlikeCourse(1L, 2L);

        assertThat(response).isEqualTo(new CourseLikeStatusResponse(1L, false, 9L, CourseStatus.OFFICIAL));
        assertThat(course.getStatus()).isEqualTo(CourseStatus.OFFICIAL);
        assertThat(course.getPromotedAt()).isEqualTo(promotedAt);
        verify(courseLikeRepository).delete(courseLike);
    }

    @DisplayName("삭제되었거나 없는 코스에는 좋아요를 등록할 수 없다")
    @Test
    void throwsCourseNotFoundWhenCourseIsMissing() {
        given(courseRepository.findNotDeletedCourse(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> courseLikeService.likeCourse(1L, 2L))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.COURSE_NOT_FOUND);

        verify(userRepository, never()).findNotDeletedUser(any());
        verify(courseLikeRepository, never()).findByCourseIdAndUserId(eq(1L), eq(2L));
    }

    @DisplayName("owner 는 private 코스에 좋아요를 남길 수 있다")
    @Test
    void likesOwnedPrivateCourse() {
        long courseId = 91L;
        long userId = 2L;
        User owner = ownerUser(userId);
        Course course = privateCourse(courseId, owner);
        given(courseRepository.findNotDeletedCourse(courseId)).willReturn(Optional.of(course));
        given(userRepository.findNotDeletedUser(userId)).willReturn(Optional.of(user));
        given(courseLikeRepository.findByCourseIdAndUserId(courseId, userId)).willReturn(Optional.empty());
        given(courseLikeRepository.save(any(kr.withrun.was.domain.course.entity.CourseLike.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(courseLikeRepository.countByCourseId(courseId)).willReturn(1L);

        CourseLikeStatusResponse response = courseLikeService.likeCourse(courseId, userId);

        assertThat(response.courseId()).isEqualTo(courseId);
        assertThat(response.isLiked()).isTrue();
    }

    @DisplayName("non-owner 는 private 코스에 좋아요를 남길 수 없다")
    @Test
    void throwsCourseNotFoundWhenLikingPrivateCourseOwnedByAnotherUser() {
        long courseId = 92L;
        Course course = privateCourse(courseId, ownerUser(99L));
        given(courseRepository.findNotDeletedCourse(courseId)).willReturn(Optional.of(course));

        assertThatThrownBy(() -> courseLikeService.likeCourse(courseId, 2L))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.COURSE_NOT_FOUND);

        verify(userRepository, never()).findNotDeletedUser(any());
    }

    @DisplayName("owner 는 private 코스의 좋아요를 취소할 수 있다")
    @Test
    void unlikesOwnedPrivateCourse() {
        long courseId = 93L;
        long userId = 2L;
        User owner = ownerUser(userId);
        Course course = privateCourse(courseId, owner);
        kr.withrun.was.domain.course.entity.CourseLike courseLike = mockExistingLike();
        given(courseRepository.findNotDeletedCourse(courseId)).willReturn(Optional.of(course));
        given(userRepository.findNotDeletedUser(userId)).willReturn(Optional.of(user));
        given(courseLikeRepository.findByCourseIdAndUserId(courseId, userId)).willReturn(Optional.of(courseLike));
        given(courseLikeRepository.countByCourseId(courseId)).willReturn(0L);

        CourseLikeStatusResponse response = courseLikeService.unlikeCourse(courseId, userId);

        assertThat(response.courseId()).isEqualTo(courseId);
        assertThat(response.isLiked()).isFalse();
        verify(courseLikeRepository).delete(courseLike);
    }

    @DisplayName("non-owner 는 private 코스의 좋아요를 취소할 수 없다")
    @Test
    void throwsCourseNotFoundWhenUnlikingPrivateCourseOwnedByAnotherUser() {
        long courseId = 94L;
        Course course = privateCourse(courseId, ownerUser(199L));
        given(courseRepository.findNotDeletedCourse(courseId)).willReturn(Optional.of(course));

        assertThatThrownBy(() -> courseLikeService.unlikeCourse(courseId, 2L))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.COURSE_NOT_FOUND);

        verify(userRepository, never()).findNotDeletedUser(any());
    }

    private void givenActiveCourseAndUser(Course course) {
        given(courseRepository.findNotDeletedCourse(1L)).willReturn(Optional.of(course));
        given(userRepository.findNotDeletedUser(2L)).willReturn(Optional.of(user));
    }

    private Course communityCourse() {
        return Course.builder()
                .title("Community Course")
                .status(CourseStatus.COMMUNITY)
                .distanceM(8000)
                .elevationGainM(120)
                .snapshotImageUrl("snapshot")
                .startLatitude(37.5)
                .startLongitude(127.0)
                .endLatitude(37.6)
                .endLongitude(127.1)
                .coordinates(new Coordinates(List.of(37.5, 37.6), List.of(127.0, 127.1), List.of(10.0, 12.0)))
                .build();
    }

    private Course privateCourse(Long id, User owner) {
        Course course = Course.builder()
                .title("Private Course")
                .status(CourseStatus.PRIVATE)
                .distanceM(8000)
                .elevationGainM(120)
                .snapshotImageUrl("snapshot")
                .startLatitude(37.5)
                .startLongitude(127.0)
                .endLatitude(37.6)
                .endLongitude(127.1)
                .coordinates(new Coordinates(List.of(37.5, 37.6), List.of(127.0, 127.1), List.of(10.0, 12.0)))
                .build();
        setField(course, "id", id);
        setField(course, "user", owner);
        return course;
    }

    private User ownerUser(Long id) {
        User owner = org.mockito.Mockito.mock(User.class);
        given(owner.getId()).willReturn(id);
        return owner;
    }

    private void setField(Object target, String fieldName, Object value) {
        Class<?> currentClass = target.getClass();
        while (currentClass != null) {
            try {
                java.lang.reflect.Field field = currentClass.getDeclaredField(fieldName);
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

    private kr.withrun.was.domain.course.entity.CourseLike mockExistingLike() {
        return org.mockito.Mockito.mock(kr.withrun.was.domain.course.entity.CourseLike.class);
    }
}
