package kr.withrun.was.domain.course.repository;

import kr.withrun.was.domain.course.entity.CourseLike;
import kr.withrun.was.domain.course.repository.query.CourseLikeCustomRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CourseLikeRepository extends JpaRepository<CourseLike, Long>, CourseLikeCustomRepository {

    Optional<CourseLike> findByCourseIdAndUserId(Long courseId, Long userId);
}
