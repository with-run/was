package kr.withrun.was.domain.course.repository;

import kr.withrun.was.domain.course.entity.UserCourseInteraction;
import kr.withrun.was.domain.course.repository.query.UserCourseInteractionCustomRepository;
import kr.withrun.was.domain.course.type.CourseInteractionType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCourseInteractionRepository
        extends JpaRepository<UserCourseInteraction, Long>, UserCourseInteractionCustomRepository {

    boolean existsByUserIdAndCourseIdAndInteractionTypeAndSourceAndSourceRef(
            Long userId,
            Long courseId,
            CourseInteractionType interactionType,
            String source,
            String sourceRef
    );
}

