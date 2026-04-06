package kr.withrun.was.domain.course.repository.query;

import java.util.List;
import java.util.Map;

public interface CourseLikeCustomRepository {

    long countByCourseId(Long courseId);

    Map<Long, Long> countByCourseIds(List<Long> courseIds);
}
