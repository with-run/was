package kr.withrun.was.domain.course.repository.query;

import kr.withrun.was.domain.course.type.CourseType;

import java.util.List;

public interface CourseTypeMapCustomRepository {

    List<CourseType> findCourseTypesByCourseId(Long courseId);
}
