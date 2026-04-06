package kr.withrun.was.domain.course.repository.query;

import kr.withrun.was.global.common.type.Difficulty;

import java.util.Optional;

public interface CourseDifficultyCustomRepository {

    Optional<Difficulty> findDifficultyByCourseId(Long courseId);
}
