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
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.domain.course.util.BookmarkedCourseCursorCodec;
import kr.withrun.was.domain.file.service.CloudFrontSignedUrlService;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.repository.UserRepository;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CourseBookmarkService {

    private final CourseRepository courseRepository;
    private final CourseBookmarkRepository courseBookmarkRepository;
    private final UserRepository userRepository;
    private final CourseSignalService courseSignalService;
    private final CloudFrontSignedUrlService cloudFrontSignedUrlService;
    private final CourseAccessPolicy courseAccessPolicy;

    @Transactional
    public CourseBookmarkAddResponse addBookmark(Long courseId, Long userId) {
        Course course = getCourse(courseId, userId);
        User user = getUser(userId);

        return courseBookmarkRepository.findByCourseIdAndUserId(courseId, userId)
                .map(bookmark -> new CourseBookmarkAddResponse(courseId, true, bookmark.getCreatedAt()))
                .orElseGet(() -> saveBookmark(course, user));
    }

    @Transactional
    public CourseBookmarkRemoveResponse removeBookmark(Long courseId, Long userId) {
        getCourse(courseId, userId);
        getUser(userId);

        return courseBookmarkRepository.findByCourseIdAndUserId(courseId, userId)
                .map(bookmark -> {
                    courseBookmarkRepository.deleteBookmark(courseId, userId);
                    courseSignalService.recordBookmarkRemoved(bookmark.getCourse());
                    return new CourseBookmarkRemoveResponse(courseId, false);
                })
                .orElseGet(() -> new CourseBookmarkRemoveResponse(courseId, false));
    }

    public BookmarkedCoursesResponse findBookmarkedCourses(Long userId, BookmarkedCoursesRequest request) {
        getUser(userId);

        List<BookmarkedCourseRow> rows = fetchBookmarkedCourseRows(userId, request);
        List<BookmarkedCourseItemResponse> items = groupBookmarkedCourseItems(rows);
        int pageSize = request.size();
        boolean hasMore = items.size() > pageSize;
        List<BookmarkedCourseItemResponse> pagedItems = hasMore
                ? List.copyOf(items.subList(0, pageSize))
                : List.copyOf(items);
        String nextCursor = hasMore ? toCursor(pagedItems.getLast()) : null;

        return new BookmarkedCoursesResponse(pagedItems, hasMore, nextCursor);
    }

    private List<BookmarkedCourseRow> fetchBookmarkedCourseRows(Long userId, BookmarkedCoursesRequest request) {
        BookmarkedCourseCursorCodec.CursorPayload payload = request.cursor() == null
                ? null
                : BookmarkedCourseCursorCodec.decode(request.cursor());

        return courseBookmarkRepository.findBookmarkedCourseRows(
                userId,
                payload == null ? null : payload.bookmarkedAt(),
                payload == null ? null : payload.bookmarkId(),
                request.size()
        );
    }

    private List<BookmarkedCourseItemResponse> groupBookmarkedCourseItems(List<BookmarkedCourseRow> rows) {
        Map<Long, List<BookmarkedCourseRow>> rowsByBookmarkId = rows.stream()
                .collect(Collectors.groupingBy(
                        BookmarkedCourseRow::bookmarkId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return rowsByBookmarkId.values().stream()
                .map(this::toBookmarkedCourseItem)
                .toList();
    }

    private BookmarkedCourseItemResponse toBookmarkedCourseItem(List<BookmarkedCourseRow> rows) {
        BookmarkedCourseRow representative = rows.getFirst();
        return new BookmarkedCourseItemResponse(
                representative.bookmarkId(),
                representative.bookmarkedAt(),
                representative.courseId(),
                representative.title(),
                representative.status(),
                representative.routeType(),
                representative.distanceM(),
                representative.elevationGainM(),
                BookmarkedCourseItemResponse.DifficultyOption.from(representative.difficulty()),
                mergeCourseTypes(rows),
                cloudFrontSignedUrlService.generateSignedUrl(representative.snapshotImageUrl()),
                representative.likeCount(),
                Boolean.TRUE.equals(representative.isLiked()),
                true
        );
    }

    private List<BookmarkedCourseItemResponse.CourseTypeOption> mergeCourseTypes(List<BookmarkedCourseRow> rows) {
        EnumSet<CourseType> courseTypes = EnumSet.noneOf(CourseType.class);
        for (BookmarkedCourseRow row : rows) {
            if (row.courseType() != null) {
                courseTypes.add(row.courseType());
            }
        }
        return courseTypes.stream()
                .map(BookmarkedCourseItemResponse.CourseTypeOption::from)
                .toList();
    }

    private String toCursor(BookmarkedCourseItemResponse item) {
        return BookmarkedCourseCursorCodec.encode(item.bookmarkedAt(), item.bookmarkId());
    }

    private CourseBookmarkAddResponse saveBookmark(Course course, User user) {
        CourseBookmark bookmark = courseBookmarkRepository.save(CourseBookmark.create(course, user));
        courseSignalService.recordBookmarkAdded(course, user, bookmark.getId(), bookmark.getCreatedAt());
        return new CourseBookmarkAddResponse(course.getId(), true, bookmark.getCreatedAt());
    }

    private Course getCourse(Long courseId, Long userId) {
        return courseAccessPolicy.getAccessibleCourse(courseRepository, courseId, userId);
    }

    private User getUser(Long userId) {
        return userRepository.findNotDeletedUser(userId)
                .orElseThrow(() -> new CustomException(ResponseCode.USER_NOT_FOUND));
    }
}
