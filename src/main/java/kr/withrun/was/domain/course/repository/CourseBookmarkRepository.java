package kr.withrun.was.domain.course.repository;

import kr.withrun.was.domain.course.entity.CourseBookmark;
import kr.withrun.was.domain.course.repository.query.CourseBookmarkCustomRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseBookmarkRepository extends JpaRepository<CourseBookmark, Long>, CourseBookmarkCustomRepository {
}
