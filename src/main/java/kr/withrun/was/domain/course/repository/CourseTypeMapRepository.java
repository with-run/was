package kr.withrun.was.domain.course.repository;

import kr.withrun.was.domain.course.entity.CourseTypeMap;
import kr.withrun.was.domain.course.repository.query.CourseTypeMapCustomRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseTypeMapRepository extends JpaRepository<CourseTypeMap, Long>, CourseTypeMapCustomRepository {
}
