package kr.withrun.was.domain.course.repository.query;

import kr.withrun.was.domain.course.repository.query.dto.CourseGhostLeaderboardRankRow;
import kr.withrun.was.domain.course.entity.CourseGhostLeaderboard;

import java.util.List;
import java.util.Optional;

public interface CourseGhostLeaderboardCustomRepository {

    List<CourseGhostLeaderboardRankRow> findRankedRowsByCourseId(Long courseId);

    Optional<CourseGhostLeaderboard> findTopByUserIdAndCourseId(Long userId, Long courseId);

}
