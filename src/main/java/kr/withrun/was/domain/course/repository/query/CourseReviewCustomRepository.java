package kr.withrun.was.domain.course.repository.query;

public interface CourseReviewCustomRepository {

    Double findAverageRatingByCourseId(Long courseId);

    boolean existsByCourseIdAndUserId(Long courseId, Long userId);
}
