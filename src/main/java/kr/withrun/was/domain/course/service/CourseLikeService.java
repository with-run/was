package kr.withrun.was.domain.course.service;

import kr.withrun.was.domain.course.dto.CourseLikeStatusResponse;
import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.entity.CourseLike;
import kr.withrun.was.domain.course.repository.CourseLikeRepository;
import kr.withrun.was.domain.course.repository.CourseRepository;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.repository.UserRepository;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CourseLikeService {

    private final CourseRepository courseRepository;
    private final CourseLikeRepository courseLikeRepository;
    private final UserRepository userRepository;
    private final CourseSignalService courseSignalService;
    private final CourseAccessPolicy courseAccessPolicy;

    @Transactional
    public CourseLikeStatusResponse likeCourse(Long courseId, Long userId) {
        Course course = getCourse(courseId, userId);
        User user = getUser(userId);

        if (courseLikeRepository.findByCourseIdAndUserId(courseId, userId).isEmpty()) {
            CourseLike savedCourseLike = courseLikeRepository.save(CourseLike.create(course, user));
            courseSignalService.recordLike(course, user, savedCourseLike.getId());
        }

        long likeCount = courseLikeRepository.countByCourseId(courseId);
        course.promoteToOfficialIfEligible(likeCount);
        return new CourseLikeStatusResponse(courseId, true, likeCount, course.getStatus());
    }

    @Transactional
    public CourseLikeStatusResponse unlikeCourse(Long courseId, Long userId) {
        Course course = getCourse(courseId, userId);
        getUser(userId);

        courseLikeRepository.findByCourseIdAndUserId(courseId, userId)
                .ifPresent(courseLike -> {
                    courseLikeRepository.delete(courseLike);
                    courseSignalService.recordUnlike(course);
                });

        long likeCount = courseLikeRepository.countByCourseId(courseId);
        return new CourseLikeStatusResponse(courseId, false, likeCount, course.getStatus());
    }

    private Course getCourse(Long courseId, Long userId) {
        return courseAccessPolicy.getAccessibleCourse(courseRepository, courseId, userId);
    }

    private User getUser(Long userId) {
        return userRepository.findNotDeletedUser(userId)
                .orElseThrow(() -> new CustomException(ResponseCode.USER_NOT_FOUND));
    }
}
