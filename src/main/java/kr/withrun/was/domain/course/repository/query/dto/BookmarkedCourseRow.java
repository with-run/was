package kr.withrun.was.domain.course.repository.query.dto;

import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.domain.course.type.RouteType;
import kr.withrun.was.global.common.type.Difficulty;

import java.time.LocalDateTime;

public record BookmarkedCourseRow(
        Long bookmarkId,
        LocalDateTime bookmarkedAt,
        Long courseId,
        String title,
        CourseStatus status,
        RouteType routeType,
        Integer distanceM,
        Integer elevationGainM,
        Difficulty difficulty,
        CourseType courseType,
        String snapshotImageUrl,
        Long likeCount,
        Boolean isLiked,
        Boolean isBookmarked
) {
}
