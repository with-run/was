package kr.withrun.was.domain.course.repository.query;

import kr.withrun.was.domain.course.repository.query.dto.CourseCollaborativeScoreRow;

import java.util.List;

public interface UserCourseInteractionCustomRepository {

    List<Long> findRecentInteractedCourseIds(Long userId, int limit);

    List<CourseCollaborativeScoreRow> findCollaborativeScoreRows(
            Long userId,
            List<Long> seedCourseIds,
            List<Long> candidateCourseIds
    );
}

