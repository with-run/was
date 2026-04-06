package kr.withrun.was.domain.course.repository;

import kr.withrun.was.domain.course.entity.CourseReview;
import kr.withrun.was.domain.course.repository.query.CourseReviewCustomRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseReviewRepository extends JpaRepository<CourseReview, Long>, CourseReviewCustomRepository {
}
