package kr.withrun.was.domain.course.repository;

import kr.withrun.was.domain.course.entity.CourseFeature;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseFeatureRepository extends JpaRepository<CourseFeature, Long> {
}

