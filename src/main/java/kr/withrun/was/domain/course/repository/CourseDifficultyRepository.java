package kr.withrun.was.domain.course.repository;

import kr.withrun.was.domain.course.entity.CourseDifficulty;
import kr.withrun.was.domain.course.repository.query.CourseDifficultyCustomRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseDifficultyRepository extends JpaRepository<CourseDifficulty, Long>, CourseDifficultyCustomRepository {
}
