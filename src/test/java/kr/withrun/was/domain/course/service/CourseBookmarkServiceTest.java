package kr.withrun.was.domain.course.service;

import kr.withrun.was.domain.course.dto.BookmarkedCourseItemResponse;
import kr.withrun.was.domain.course.dto.BookmarkedCoursesRequest;
import kr.withrun.was.domain.course.dto.BookmarkedCoursesResponse;
import kr.withrun.was.domain.course.dto.CourseBookmarkAddResponse;
import kr.withrun.was.domain.course.dto.CourseBookmarkRemoveResponse;
import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.entity.CourseBookmark;
import kr.withrun.was.domain.course.repository.CourseBookmarkRepository;
import kr.withrun.was.domain.course.repository.CourseRepository;
import kr.withrun.was.domain.course.repository.query.dto.BookmarkedCourseRow;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.domain.course.type.RouteType;
import kr.withrun.was.domain.course.vo.Coordinates;
import kr.withrun.was.domain.file.service.CloudFrontSignedUrlService;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("코스 북마크 서비스")
class CourseBookmarkServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseBookmarkRepository courseBookmarkRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CourseSignalService courseSignalService;

    @Mock
    private CloudFrontSignedUrlService cloudFrontSignedUrlService;

    private CourseBookmarkService courseBookmarkService;

    @BeforeEach
    void setUp() {
        courseBookmarkService = new CourseBookmarkService(
                courseRepository,
                courseBookmarkRepository,
                userRepository,
                courseSignalService,
                cloudFrontSignedUrlService,
                new CourseAccessPolicy()
        );
        lenient().when(cloudFrontSignedUrlService.generateSignedUrl(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> {
                    Object value = invocation.getArgument(0);
                    return value == null ? null : "signed::" + value;
                });
    }

    @DisplayName("북마크를 추가하면 북마크 상태와 생성 시각을 반환한다")
    @Test
    void addsBookmarkAndReturnsBookmarkedStateWithCreatedAt() {
        Course course = course(10L);
        User user = user(20L);
        CourseBookmark savedBookmark = bookmark(30L, course, user, LocalDateTime.of(2026, 3, 11, 4, 0));
        stubCourseAndUser(course, user);
        when(courseBookmarkRepository.findByCourseIdAndUserId(course.getId(), user.getId())).thenReturn(Optional.empty());
        when(courseBookmarkRepository.save(any(CourseBookmark.class))).thenReturn(savedBookmark);

        CourseBookmarkAddResponse response = courseBookmarkService.addBookmark(course.getId(), user.getId());

        assertThat(response.courseId()).isEqualTo(course.getId());
        assertThat(response.isBookmarked()).isTrue();
        assertThat(response.createdAt()).isEqualTo(savedBookmark.getCreatedAt());
    }

    @DisplayName("중복 북마크 추가는 기존 북마크를 그대로 반환한다")
    @Test
    void keepsDuplicateAddIdempotent() {
        Course course = course(11L);
        User user = user(21L);
        CourseBookmark existingBookmark = bookmark(31L, course, user, LocalDateTime.of(2026, 3, 11, 5, 0));
        stubCourseAndUser(course, user);
        when(courseBookmarkRepository.findByCourseIdAndUserId(course.getId(), user.getId()))
                .thenReturn(Optional.of(existingBookmark));

        CourseBookmarkAddResponse response = courseBookmarkService.addBookmark(course.getId(), user.getId());

        assertThat(response.courseId()).isEqualTo(course.getId());
        assertThat(response.isBookmarked()).isTrue();
        assertThat(response.createdAt()).isEqualTo(existingBookmark.getCreatedAt());
        verify(courseBookmarkRepository, never()).save(any(CourseBookmark.class));
    }

    @DisplayName("기존 북마크를 삭제하면 북마크 해제 상태를 반환한다")
    @Test
    void removesExistingBookmarkAndReturnsUnbookmarkedState() {
        Course course = course(12L);
        User user = user(22L);
        CourseBookmark existingBookmark = bookmark(32L, course, user, LocalDateTime.of(2026, 3, 11, 6, 0));
        stubCourseAndUser(course, user);
        when(courseBookmarkRepository.findByCourseIdAndUserId(course.getId(), user.getId()))
                .thenReturn(Optional.of(existingBookmark));

        CourseBookmarkRemoveResponse response = courseBookmarkService.removeBookmark(course.getId(), user.getId());

        assertThat(response.courseId()).isEqualTo(course.getId());
        assertThat(response.isBookmarked()).isFalse();
        verify(courseBookmarkRepository).deleteBookmark(course.getId(), user.getId());
    }

    @DisplayName("없는 북마크를 삭제해도 북마크 해제 상태를 유지한다")
    @Test
    void keepsDeleteMissingBookmarkIdempotent() {
        Course course = course(13L);
        User user = user(23L);
        stubCourseAndUser(course, user);
        when(courseBookmarkRepository.findByCourseIdAndUserId(course.getId(), user.getId())).thenReturn(Optional.empty());

        CourseBookmarkRemoveResponse response = courseBookmarkService.removeBookmark(course.getId(), user.getId());

        assertThat(response.courseId()).isEqualTo(course.getId());
        assertThat(response.isBookmarked()).isFalse();
        verify(courseBookmarkRepository, never()).deleteBookmark(course.getId(), user.getId());
    }

    @DisplayName("북마크 목록 첫 페이지를 반환하고 다음 커서를 생성한다")
    @Test
    void returnsFirstPageOfBookmarkedCoursesWithNextCursor() {
        User user = user(40L);
        when(userRepository.findNotDeletedUser(user.getId())).thenReturn(Optional.of(user));
        when(courseBookmarkRepository.findBookmarkedCourseRows(user.getId(), null, null, 1)).thenReturn(List.of(
                bookmarkedCourseRow(
                        102L,
                        LocalDateTime.of(2026, 3, 14, 10, 30),
                        12L,
                        "Latest Course",
                        CourseStatus.OFFICIAL,
                        7000,
                        50,
                        Difficulty.HARD,
                        CourseType.PARK,
                        "snapshot-12",
                        9L,
                        true
                ),
                bookmarkedCourseRow(
                        101L,
                        LocalDateTime.of(2026, 3, 14, 9, 0),
                        11L,
                        "Earlier Course",
                        CourseStatus.COMMUNITY,
                        5000,
                        20,
                        Difficulty.MEDIUM,
                        CourseType.RIVERSIDE,
                        "snapshot-11",
                        4L,
                        false
                ),
                bookmarkedCourseRow(
                        101L,
                        LocalDateTime.of(2026, 3, 14, 9, 0),
                        11L,
                        "Earlier Course",
                        CourseStatus.COMMUNITY,
                        5000,
                        20,
                        Difficulty.MEDIUM,
                        CourseType.PARK,
                        "snapshot-11",
                        4L,
                        false
                )
        ));

        BookmarkedCoursesResponse response = courseBookmarkService.findBookmarkedCourses(
                user.getId(),
                new BookmarkedCoursesRequest(1, null)
        );

        assertThat(response.items()).hasSize(1);
        BookmarkedCourseItemResponse firstItem = response.items().getFirst();
        assertThat(firstItem.bookmarkId()).isEqualTo(102L);
        assertThat(firstItem.bookmarkedAt()).isEqualTo(LocalDateTime.of(2026, 3, 14, 10, 30));
        assertThat(firstItem.courseId()).isEqualTo(12L);
        assertThat(firstItem.routeType()).isEqualTo(RouteType.LOOP);
        assertThat(firstItem.difficulty()).isEqualTo(new BookmarkedCourseItemResponse.DifficultyOption("HARD", "어려움"));
        assertThat(firstItem.likeCount()).isEqualTo(9L);
        assertThat(firstItem.isLiked()).isTrue();
        assertThat(firstItem.isBookmarked()).isTrue();
        assertThat(firstItem.courseTypes()).containsExactly(new BookmarkedCourseItemResponse.CourseTypeOption("PARK", "공원"));
        assertThat(firstItem.snapshotImageUrl()).isEqualTo("signed::snapshot-12");
        assertThat(response.hasMore()).isTrue();
        assertThat(response.nextCursor()).isNotBlank();
    }

    @DisplayName("다음 커서를 넘기면 이후 북마크 코스만 반환하고 타입을 병합한다")
    @Test
    void returnsBookmarkedCoursesAfterCursorAndMergesCourseTypes() {
        User user = user(41L);
        when(userRepository.findNotDeletedUser(user.getId())).thenReturn(Optional.of(user));
        when(courseBookmarkRepository.findBookmarkedCourseRows(
                user.getId(),
                LocalDateTime.of(2026, 3, 14, 10, 30),
                102L,
                5
        )).thenReturn(List.of(
                bookmarkedCourseRow(
                        101L,
                        LocalDateTime.of(2026, 3, 14, 9, 0),
                        11L,
                        "Earlier Course",
                        CourseStatus.COMMUNITY,
                        5000,
                        20,
                        Difficulty.MEDIUM,
                        CourseType.PARK,
                        "snapshot-11",
                        3L,
                        false
                ),
                bookmarkedCourseRow(
                        101L,
                        LocalDateTime.of(2026, 3, 14, 9, 0),
                        11L,
                        "Earlier Course",
                        CourseStatus.COMMUNITY,
                        5000,
                        20,
                        Difficulty.MEDIUM,
                        CourseType.RIVERSIDE,
                        "snapshot-11",
                        3L,
                        false
                )
        ));

        String cursor = kr.withrun.was.domain.course.util.BookmarkedCourseCursorCodec.encode(
                LocalDateTime.of(2026, 3, 14, 10, 30),
                102L
        );

        BookmarkedCoursesResponse response = courseBookmarkService.findBookmarkedCourses(
                user.getId(),
                new BookmarkedCoursesRequest(5, cursor)
        );

        assertThat(response.items()).hasSize(1);
        BookmarkedCourseItemResponse item = response.items().getFirst();
        assertThat(item.bookmarkId()).isEqualTo(101L);
        assertThat(item.status()).isEqualTo(CourseStatus.COMMUNITY);
        assertThat(item.routeType()).isEqualTo(RouteType.LOOP);
        assertThat(item.likeCount()).isEqualTo(3L);
        assertThat(item.isLiked()).isFalse();
        assertThat(item.difficulty()).isEqualTo(new BookmarkedCourseItemResponse.DifficultyOption("MEDIUM", "보통"));
        assertThat(item.courseTypes()).containsExactly(
                new BookmarkedCourseItemResponse.CourseTypeOption("RIVERSIDE", "강변"),
                new BookmarkedCourseItemResponse.CourseTypeOption("PARK", "공원")
        );
        assertThat(response.hasMore()).isFalse();
        assertThat(response.nextCursor()).isNull();
    }

    @DisplayName("잘못된 커서는 INVALID_CURSOR 예외를 던진다")
    @Test
    void throwsInvalidCursorWhenCursorIsMalformed() {
        User user = user(42L);
        when(userRepository.findNotDeletedUser(user.getId())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> courseBookmarkService.findBookmarkedCourses(
                user.getId(),
                new BookmarkedCoursesRequest(10, "%%%")
        ))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.INVALID_CURSOR);
    }

    @DisplayName("없는 사용자가 북마크 목록을 조회하면 사용자 없음 예외를 던진다")
    @Test
    void throwsUserNotFoundWhenUserIsMissingOnList() {
        when(userRepository.findNotDeletedUser(43L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseBookmarkService.findBookmarkedCourses(
                43L,
                new BookmarkedCoursesRequest(10, null)
        ))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.USER_NOT_FOUND);
    }

    @DisplayName("없는 코스에 북마크를 추가하면 코스 없음 예외를 던진다")
    @Test
    void throwsCourseNotFoundWhenCourseIsMissingOnAdd() {
        when(courseRepository.findNotDeletedCourse(14L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseBookmarkService.addBookmark(14L, 24L))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.COURSE_NOT_FOUND);
    }

    @DisplayName("삭제된 코스에 북마크를 추가하면 코스 없음 예외를 던진다")
    @Test
    void throwsCourseNotFoundWhenCourseIsDeletedOnAdd() {
        Course deletedCourse = course(15L);
        deletedCourse.delete();
        when(courseRepository.findNotDeletedCourse(deletedCourse.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseBookmarkService.addBookmark(deletedCourse.getId(), 25L))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.COURSE_NOT_FOUND);
    }

    @DisplayName("없는 사용자가 북마크를 삭제하면 사용자 없음 예외를 던진다")
    @Test
    void throwsUserNotFoundWhenUserIsMissingOnRemove() {
        Course course = course(16L);
        when(courseRepository.findNotDeletedCourse(course.getId())).thenReturn(Optional.of(course));
        when(userRepository.findNotDeletedUser(26L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseBookmarkService.removeBookmark(course.getId(), 26L))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.USER_NOT_FOUND);
    }

    @DisplayName("owner 는 private 코스에 북마크를 추가할 수 있다")
    @Test
    void addsBookmarkToOwnedPrivateCourse() {
        User owner = user(29L);
        Course course = course(19L, CourseStatus.PRIVATE, owner);
        CourseBookmark savedBookmark = bookmark(39L, course, owner, LocalDateTime.of(2026, 3, 11, 8, 0));
        stubCourseAndUser(course, owner);
        when(courseBookmarkRepository.findByCourseIdAndUserId(course.getId(), owner.getId())).thenReturn(Optional.empty());
        when(courseBookmarkRepository.save(any(CourseBookmark.class))).thenReturn(savedBookmark);

        CourseBookmarkAddResponse response = courseBookmarkService.addBookmark(course.getId(), owner.getId());

        assertThat(response.courseId()).isEqualTo(course.getId());
        assertThat(response.isBookmarked()).isTrue();
    }

    @DisplayName("non-owner 는 private 코스에 북마크를 추가할 수 없다")
    @Test
    void throwsCourseNotFoundWhenAddingBookmarkToPrivateCourseOwnedByAnotherUser() {
        Course course = course(20L, CourseStatus.PRIVATE, user(199L));
        when(courseRepository.findNotDeletedCourse(course.getId())).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> courseBookmarkService.addBookmark(course.getId(), 30L))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.COURSE_NOT_FOUND);
    }

    @DisplayName("owner 는 private 코스의 북마크를 해제할 수 있다")
    @Test
    void removesBookmarkFromOwnedPrivateCourse() {
        User owner = user(31L);
        Course course = course(21L, CourseStatus.PRIVATE, owner);
        CourseBookmark existingBookmark = bookmark(41L, course, owner, LocalDateTime.of(2026, 3, 11, 9, 0));
        stubCourseAndUser(course, owner);
        when(courseBookmarkRepository.findByCourseIdAndUserId(course.getId(), owner.getId()))
                .thenReturn(Optional.of(existingBookmark));

        CourseBookmarkRemoveResponse response = courseBookmarkService.removeBookmark(course.getId(), owner.getId());

        assertThat(response.courseId()).isEqualTo(course.getId());
        assertThat(response.isBookmarked()).isFalse();
        verify(courseBookmarkRepository).deleteBookmark(course.getId(), owner.getId());
    }

    @DisplayName("non-owner 는 private 코스의 북마크를 해제할 수 없다")
    @Test
    void throwsCourseNotFoundWhenRemovingBookmarkFromPrivateCourseOwnedByAnotherUser() {
        Course course = course(22L, CourseStatus.PRIVATE, user(299L));
        when(courseRepository.findNotDeletedCourse(course.getId())).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> courseBookmarkService.removeBookmark(course.getId(), 32L))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.COURSE_NOT_FOUND);
    }

    @DisplayName("삭제되지 않은 사용자는 활성 상태 플래그 없이도 북마크를 추가할 수 있다")
    @Test
    void addsBookmarkWithoutUserActiveFlag() {
        Course course = course(18L);
        User user = userWithoutActiveFlag(28L);
        CourseBookmark savedBookmark = bookmark(38L, course, user, LocalDateTime.of(2026, 3, 11, 7, 0));
        when(courseRepository.findNotDeletedCourse(course.getId())).thenReturn(Optional.of(course));
        when(userRepository.findNotDeletedUser(user.getId())).thenReturn(Optional.of(user));
        when(courseBookmarkRepository.findByCourseIdAndUserId(course.getId(), user.getId())).thenReturn(Optional.empty());
        when(courseBookmarkRepository.save(any(CourseBookmark.class))).thenReturn(savedBookmark);

        CourseBookmarkAddResponse response = courseBookmarkService.addBookmark(course.getId(), user.getId());

        assertThat(response.courseId()).isEqualTo(course.getId());
        assertThat(response.isBookmarked()).isTrue();
        assertThat(response.createdAt()).isEqualTo(savedBookmark.getCreatedAt());
    }

    @DisplayName("삭제된 사용자가 북마크를 삭제하면 사용자 없음 예외를 던진다")
    @Test
    void throwsUserNotFoundWhenUserIsDeletedOnRemove() {
        Course course = course(17L);
        User deletedUser = user(27L);
        deletedUser.delete();
        when(courseRepository.findNotDeletedCourse(course.getId())).thenReturn(Optional.of(course));
        when(userRepository.findNotDeletedUser(deletedUser.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseBookmarkService.removeBookmark(course.getId(), deletedUser.getId()))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.USER_NOT_FOUND);
    }

    private void stubCourseAndUser(Course course, User user) {
        when(courseRepository.findNotDeletedCourse(course.getId())).thenReturn(Optional.of(course));
        when(userRepository.findNotDeletedUser(user.getId())).thenReturn(Optional.of(user));
    }

    private BookmarkedCourseRow bookmarkedCourseRow(
            Long bookmarkId,
            LocalDateTime bookmarkedAt,
            Long courseId,
            String title,
            CourseStatus status,
            Integer distanceM,
            Integer elevationGainM,
            Difficulty difficulty,
            CourseType courseType,
            String snapshotImageUrl,
            Long likeCount,
            boolean isLiked
    ) {
        return new BookmarkedCourseRow(
                bookmarkId,
                bookmarkedAt,
                courseId,
                title,
                status,
                RouteType.LOOP,
                distanceM,
                elevationGainM,
                difficulty,
                courseType,
                snapshotImageUrl,
                likeCount,
                isLiked,
                true
        );
    }

    private Course course(Long id) {
        return course(id, CourseStatus.OFFICIAL, null);
    }

    private Course course(Long id, CourseStatus status, User owner) {
        Course course = Course.builder()
                .title("Course " + id)
                .status(status)
                .distanceM(5000)
                .elevationGainM(120)
                .snapshotImageUrl("snapshot-" + id)
                .startLatitude(37.5665)
                .startLongitude(126.9780)
                .endLatitude(37.5700)
                .endLongitude(126.9820)
                .coordinates(new Coordinates(List.of(37.5665), List.of(126.9780), List.of(12.0)))
                .build();
        setField(course, "id", id);
        setField(course, "routeType", RouteType.LOOP);
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

    private User userWithoutActiveFlag(Long id) {
        User user = instantiateUser();
        setField(user, "id", id);
        setField(user, "nickname", "runner-" + id);
        setField(user, "birthDate", LocalDate.of(1995, 3, 11));
        setField(user, "gender", Gender.MALE);
        setField(user, "height", 175.0);
        setField(user, "weight", 68.0);
        return user;
    }

    private CourseBookmark bookmark(Long id, Course course, User user, LocalDateTime createdAt) {
        CourseBookmark bookmark = CourseBookmark.create(course, user);
        setField(bookmark, "id", id);
        setField(bookmark, "createdAt", createdAt);
        return bookmark;
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
