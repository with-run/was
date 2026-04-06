package kr.withrun.was.domain.course.repository.query.dto;

import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.type.RouteType;
import kr.withrun.was.global.common.type.Difficulty;

public record NearbyCoursePageRow(
        Long courseId,
        String title,
        CourseStatus status,
        RouteType routeType,
        Integer distanceM,
        Integer elevationGainM,
        Double startLatitude,
        Double startLongitude,
        Double endLatitude,
        Double endLongitude,
        String snapshotImageUrl,
        Difficulty difficulty,
        Integer distanceFromTargetM,
        Integer distanceFromUserM
) {
}
