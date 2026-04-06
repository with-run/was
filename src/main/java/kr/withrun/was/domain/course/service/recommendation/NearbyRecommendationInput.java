package kr.withrun.was.domain.course.service.recommendation;

import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.global.common.type.Difficulty;

import java.util.List;

public record NearbyRecommendationInput(
        Long courseId,
        Integer courseDistanceM,
        Integer distanceFromUserM,
        Difficulty difficulty,
        List<CourseType> courseTypes
) {
}

