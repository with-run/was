package kr.withrun.was.domain.course.repository.query.dto;

import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.domain.course.vo.Coordinates;
import kr.withrun.was.global.common.type.Difficulty;

public record NearbyCourseCandidateRow(
        Long courseId,
        String title,
        CourseStatus status,
        Integer distanceM,
        Integer elevationGainM,
        Double startLatitude,
        Double startLongitude,
        Double endLatitude,
        Double endLongitude,
        String snapshotImageUrl,
        Coordinates coordinates,
        Difficulty difficulty,
        CourseType courseType
) {
}
