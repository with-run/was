package kr.withrun.was.domain.course.repository;

import kr.withrun.was.domain.course.entity.CourseGhostLeaderboard;
import kr.withrun.was.domain.course.repository.query.CourseGhostLeaderboardCustomRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseGhostLeaderboardRepository extends JpaRepository<CourseGhostLeaderboard, Long>, CourseGhostLeaderboardCustomRepository {
}
