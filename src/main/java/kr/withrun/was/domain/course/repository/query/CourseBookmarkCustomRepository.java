package kr.withrun.was.domain.course.repository.query;

import kr.withrun.was.domain.course.entity.CourseBookmark;
import kr.withrun.was.domain.course.repository.query.dto.BookmarkedCourseRow;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CourseBookmarkCustomRepository {

    long countByCourseId(Long courseId);

    Map<Long, Long> countByCourseIds(List<Long> courseIds);

    Optional<CourseBookmark> findByCourseIdAndUserId(Long courseId, Long userId);

    List<BookmarkedCourseRow> findBookmarkedCourseRows(Long userId, LocalDateTime cursorBookmarkedAt, Long cursorBookmarkId, int pageSize);

    void deleteBookmark(Long courseId, Long userId);
}
