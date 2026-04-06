package kr.withrun.was.domain.course.repository;

import kr.withrun.was.domain.course.entity.CourseFeedbackStats;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseFeedbackStatsRepository extends JpaRepository<CourseFeedbackStats, Long> {
}

