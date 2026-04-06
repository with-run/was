package kr.withrun.was.domain.course.service;

import kr.withrun.was.domain.course.dto.CreateCourseReviewRequest;
import kr.withrun.was.domain.course.dto.CreateCourseReviewResponse;
import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.entity.CourseReview;
import kr.withrun.was.domain.course.entity.CourseReviewCourseType;
import kr.withrun.was.domain.course.repository.CourseRepository;
import kr.withrun.was.domain.course.repository.CourseReviewCourseTypeRepository;
import kr.withrun.was.domain.course.repository.CourseReviewRepository;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.domain.course.vo.Coordinates;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.repository.UserRepository;
import kr.withrun.was.domain.user.type.Gender;
import kr.withrun.was.global.common.type.Difficulty;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("코스 리뷰 서비스")
class CourseReviewServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseReviewRepository courseReviewRepository;

    @Mock
    private CourseReviewCourseTypeRepository courseReviewCourseTypeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CourseSignalService courseSignalService;

    private CourseReviewService courseReviewService;

    @BeforeEach
    void setUp() {
        courseReviewService = new CourseReviewService(
                courseRepository,
                courseReviewRepository,
                courseReviewCourseTypeRepository,
                userRepository,
                courseSignalService,
                new CourseAccessPolicy()
        );
    }


    @DisplayName("코스 리뷰를 생성하면 평점, 난이도, 코스 타입을 저장하고 응답을 반환한다")
    @Test
    void createsCourseReviewAndReturnsSavedReview() {
        long courseId = 101L;
        long userId = 1L;
        Course course = course(courseId);
        User user = user(userId);
        CreateCourseReviewRequest request = new CreateCourseReviewRequest(5, List.of("RIVERSIDE", "URBAN"), "MEDIUM");
        CourseReview savedReview = review(501L, course, user, 5, Difficulty.MEDIUM, LocalDateTime.of(2026, 3, 8, 21, 0));

        when(courseRepository.findNotDeletedCourse(courseId)).thenReturn(Optional.of(course));
        when(userRepository.findNotDeletedUser(userId)).thenReturn(Optional.of(user));
        when(courseReviewRepository.existsByCourseIdAndUserId(courseId, userId)).thenReturn(false);
        when(courseReviewRepository.save(any(CourseReview.class))).thenReturn(savedReview);

        CreateCourseReviewResponse response = courseReviewService.createCourseReview(courseId, userId, request);

        assertThat(response.courseReviewId()).isEqualTo(getField(savedReview, "id", Long.class));
        assertThat(response.courseId()).isEqualTo(courseId);
        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.courseTypes()).containsExactly(
                new CreateCourseReviewResponse.CourseTypeOption("RIVERSIDE", "강변"),
                new CreateCourseReviewResponse.CourseTypeOption("URBAN", "도심")
        );
        assertThat(response.submittedDifficulty()).isEqualTo(new CreateCourseReviewResponse.DifficultyOption("MEDIUM", "보통"));
        assertThat(response.createdAt()).isEqualTo(getField(savedReview, "createdAt", LocalDateTime.class));

        ArgumentCaptor<List<CourseReviewCourseType>> courseTypesCaptor = ArgumentCaptor.forClass(List.class);
        verify(courseReviewCourseTypeRepository).saveAll(courseTypesCaptor.capture());
        assertThat(courseTypesCaptor.getValue())
                .extracting(courseReviewCourseType -> getField(courseReviewCourseType, "courseType", CourseType.class))
                .containsExactly(CourseType.RIVERSIDE, CourseType.URBAN);
    }

    @DisplayName("이미 작성한 리뷰가 있으면 REVIEW_ALREADY_EXISTS 예외를 던진다")
    @Test
    void throwsReviewAlreadyExistsWhenDuplicateReviewIsRequested() {
        long courseId = 102L;
        long userId = 2L;
        Course course = course(courseId);
        User user = user(userId);
        CreateCourseReviewRequest request = new CreateCourseReviewRequest(4, List.of("PARK"), "EASY");

        when(courseRepository.findNotDeletedCourse(courseId)).thenReturn(Optional.of(course));
        when(userRepository.findNotDeletedUser(userId)).thenReturn(Optional.of(user));
        when(courseReviewRepository.existsByCourseIdAndUserId(courseId, userId)).thenReturn(true);

        assertThatThrownBy(() -> courseReviewService.createCourseReview(courseId, userId, request))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.REVIEW_ALREADY_EXISTS);

        verify(courseReviewRepository, never()).save(any(CourseReview.class));
        verify(courseReviewCourseTypeRepository, never()).saveAll(any());
    }

    @DisplayName("리뷰 저장 중 유니크 제약 위반이 발생하면 REVIEW_ALREADY_EXISTS 예외로 변환한다")
    @Test
    void throwsReviewAlreadyExistsWhenUniqueConstraintIsViolatedDuringSave() {
        long courseId = 105L;
        long userId = 5L;
        Course course = course(courseId);
        User user = user(userId);
        CreateCourseReviewRequest request = new CreateCourseReviewRequest(4, List.of("PARK"), "EASY");

        when(courseRepository.findNotDeletedCourse(courseId)).thenReturn(Optional.of(course));
        when(userRepository.findNotDeletedUser(userId)).thenReturn(Optional.of(user));
        when(courseReviewRepository.existsByCourseIdAndUserId(courseId, userId)).thenReturn(false);
        when(courseReviewRepository.save(any(CourseReview.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate review"));

        assertThatThrownBy(() -> courseReviewService.createCourseReview(courseId, userId, request))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.REVIEW_ALREADY_EXISTS);

        verify(courseReviewCourseTypeRepository, never()).saveAll(any());
    }

    @DisplayName("유효하지 않은 코스 타입이 포함되면 잘못된 입력 예외를 던진다")
    @Test
    void throwsInvalidInputValueWhenCourseTypeIsInvalid() {
        CreateCourseReviewRequest request = new CreateCourseReviewRequest(5, List.of("RIVERSIDE", "BEACH"), "MEDIUM");

        assertThatThrownBy(() -> courseReviewService.createCourseReview(103L, 3L, request))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.INVALID_INPUT_VALUE);
    }

    @DisplayName("유효하지 않은 난이도가 들어오면 잘못된 입력 예외를 던진다")
    @Test
    void throwsInvalidInputValueWhenDifficultyIsInvalid() {
        CreateCourseReviewRequest request = new CreateCourseReviewRequest(5, List.of("RIVERSIDE"), "VERY_HARD");

        assertThatThrownBy(() -> courseReviewService.createCourseReview(104L, 4L, request))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.INVALID_INPUT_VALUE);
    }

    @DisplayName("owner 는 private 코스에 리뷰를 남길 수 있다")
    @Test
    void createsReviewForOwnedPrivateCourse() {
        long courseId = 106L;
        long userId = 6L;
        User owner = user(userId);
        Course course = course(courseId, CourseStatus.PRIVATE, owner);
        CreateCourseReviewRequest request = new CreateCourseReviewRequest(5, List.of("PARK"), "EASY");
        CourseReview savedReview = review(601L, course, owner, 5, Difficulty.EASY, LocalDateTime.of(2026, 3, 9, 8, 0));

        when(courseRepository.findNotDeletedCourse(courseId)).thenReturn(Optional.of(course));
        when(userRepository.findNotDeletedUser(userId)).thenReturn(Optional.of(owner));
        when(courseReviewRepository.existsByCourseIdAndUserId(courseId, userId)).thenReturn(false);
        when(courseReviewRepository.save(any(CourseReview.class))).thenReturn(savedReview);

        CreateCourseReviewResponse response = courseReviewService.createCourseReview(courseId, userId, request);

        assertThat(response.courseId()).isEqualTo(courseId);
        assertThat(response.rating()).isEqualTo(5);
    }

    @DisplayName("non-owner 는 private 코스에 리뷰를 남길 수 없다")
    @Test
    void throwsCourseNotFoundWhenCreatingReviewForPrivateCourseOwnedByAnotherUser() {
        long courseId = 107L;
        Course course = course(courseId, CourseStatus.PRIVATE, user(77L));
        CreateCourseReviewRequest request = new CreateCourseReviewRequest(4, List.of("PARK"), "EASY");
        when(courseRepository.findNotDeletedCourse(courseId)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> courseReviewService.createCourseReview(courseId, 8L, request))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.COURSE_NOT_FOUND);
    }

    private Course course(Long id) {
        return course(id, CourseStatus.OFFICIAL, null);
    }

    private Course course(Long id, CourseStatus status, User owner) {
        Course course = instantiateCourse();
        setField(course, "id", id);
        setField(course, "title", "Course " + id);
        setField(course, "status", status);
        setField(course, "distanceM", 5000);
        setField(course, "elevationGainM", 120);
        setField(course, "snapshotImageUrl", "snapshot-" + id);
        setField(course, "startLatitude", 37.5665);
        setField(course, "startLongitude", 126.9780);
        setField(course, "endLatitude", 37.5700);
        setField(course, "endLongitude", 126.9820);
        setField(course, "coordinates", new Coordinates(List.of(37.5665), List.of(126.9780), List.of(12.0)));
        if (owner != null) {
            setField(course, "user", owner);
        }
        return course;
    }

    private User user(Long id) {
        User user = instantiateUser();
        setField(user, "id", id);
        setField(user, "nickname", "runner-" + id);
        setField(user, "birthDate", LocalDate.of(1995, 3, 11));
        setField(user, "gender", Gender.MALE);
        setField(user, "height", 175.0);
        setField(user, "weight", 68.0);
        return user;
    }

    private CourseReview review(
            Long id,
            Course course,
            User user,
            int rating,
            Difficulty difficulty,
            LocalDateTime createdAt
    ) {
        CourseReview review = CourseReview.create(course, user, rating, difficulty);
        setField(review, "id", id);
        setField(review, "createdAt", createdAt);
        return review;
    }

    private User instantiateUser() {
        try {
            Constructor<User> constructor = User.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to instantiate user", exception);
        }
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

    private <T> T getField(Object target, String fieldName, Class<T> type) {
        Class<?> currentClass = target.getClass();
        while (currentClass != null) {
            try {
                Field field = currentClass.getDeclaredField(fieldName);
                field.setAccessible(true);
                return type.cast(field.get(target));
            } catch (NoSuchFieldException exception) {
                currentClass = currentClass.getSuperclass();
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException("Failed to read field " + fieldName, exception);
            }
        }

        throw new IllegalArgumentException("Field not found: " + fieldName);
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
