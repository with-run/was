package kr.withrun.was.domain.course.service;

import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.repository.CourseRepository;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import org.springframework.stereotype.Service;

@Service
public class CourseAccessPolicy {

    public boolean canAccess(Course course, Long currentUserId) {
        if (course == null) {
            return false;
        }

        if (course.getStatus() == CourseStatus.OFFICIAL || course.getStatus() == CourseStatus.COMMUNITY) {
            return true;
        }

        return course.getStatus() == CourseStatus.PRIVATE
                && course.getUser() != null
                && currentUserId != null
                && course.getUser().getId().equals(currentUserId);
    }

    public Course getAccessibleCourse(CourseRepository courseRepository, Long courseId, Long currentUserId) {
        return courseRepository.findNotDeletedCourse(courseId)
                .filter(course -> canAccess(course, currentUserId))
                .orElseThrow(() -> new CustomException(ResponseCode.COURSE_NOT_FOUND));
    }
}
