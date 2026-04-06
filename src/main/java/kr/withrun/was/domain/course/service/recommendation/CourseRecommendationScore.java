package kr.withrun.was.domain.course.service.recommendation;

public record CourseRecommendationScore(
        Long courseId,
        double locationScore,
        double cbfScore,
        double qualityScore,
        double cfScore,
        double baseScore,
        double finalScore
) {
}

