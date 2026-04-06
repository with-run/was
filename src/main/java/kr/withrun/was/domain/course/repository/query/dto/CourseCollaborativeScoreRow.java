package kr.withrun.was.domain.course.repository.query.dto;

public record CourseCollaborativeScoreRow(
        Long courseId,
        Long similarUserCount
) {
}

