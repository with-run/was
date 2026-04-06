package kr.withrun.was.domain.course.service;

import kr.withrun.was.domain.course.dto.CreateCourseReviewRequest;
import kr.withrun.was.domain.course.dto.CreateCourseReviewResponse;
import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.entity.CourseReview;
import kr.withrun.was.domain.course.entity.CourseReviewCourseType;
import kr.withrun.was.domain.course.repository.CourseRepository;
import kr.withrun.was.domain.course.repository.CourseReviewCourseTypeRepository;
import kr.withrun.was.domain.course.repository.CourseReviewRepository;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.repository.UserRepository;
import kr.withrun.was.global.common.type.Difficulty;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CourseReviewService {

    private final CourseRepository courseRepository;
    private final CourseReviewRepository courseReviewRepository;
    private final CourseReviewCourseTypeRepository courseReviewCourseTypeRepository;
    private final UserRepository userRepository;
    private final CourseSignalService courseSignalService;
    private final CourseAccessPolicy courseAccessPolicy;

    @Transactional
    public CreateCourseReviewResponse createCourseReview(Long courseId, Long currentUserId, CreateCourseReviewRequest request) {
        List<CourseType> courseTypes = parseCourseTypes(request.courseTypes());
        Difficulty difficulty = parseDifficulty(request.submittedDifficulty());
        Course course = getCourse(courseId, currentUserId);
        User user = getUser(currentUserId);

        if (courseReviewRepository.existsByCourseIdAndUserId(courseId, user.getId())) {
            throw new CustomException(ResponseCode.REVIEW_ALREADY_EXISTS);
        }

        CourseReview savedCourseReview;
        try {
            savedCourseReview = courseReviewRepository.save(
                    CourseReview.create(course, user, request.rating(), difficulty)
            );
        } catch (DataIntegrityViolationException exception) {
            throw new CustomException(ResponseCode.REVIEW_ALREADY_EXISTS);
        }

        courseReviewCourseTypeRepository.saveAll(
                courseTypes.stream()
                        .map(courseType -> CourseReviewCourseType.create(savedCourseReview, courseType))
                        .toList()
        );
        courseSignalService.recordReview(
                course,
                user,
                savedCourseReview.getId(),
                request.rating(),
                difficulty,
                courseTypes
        );

        return CreateCourseReviewResponse.from(savedCourseReview, courseTypes);
    }

    private Course getCourse(Long courseId, Long currentUserId) {
        return courseAccessPolicy.getAccessibleCourse(courseRepository, courseId, currentUserId);
    }

    private User getUser(Long userId) {
        return userRepository.findNotDeletedUser(userId)
                .orElseThrow(() -> new CustomException(ResponseCode.USER_NOT_FOUND));
    }

    private List<CourseType> parseCourseTypes(List<String> rawCourseTypes) {
        LinkedHashSet<CourseType> courseTypes = new LinkedHashSet<>();
        for (String rawCourseType : rawCourseTypes) {
            courseTypes.add(parseCourseType(rawCourseType));
        }
        return new ArrayList<>(courseTypes);
    }

    private CourseType parseCourseType(String rawCourseType) {
        try {
            return CourseType.valueOf(rawCourseType);
        } catch (IllegalArgumentException exception) {
            throw new CustomException(ResponseCode.INVALID_INPUT_VALUE);
        }
    }

    private Difficulty parseDifficulty(String rawDifficulty) {
        try {
            return Difficulty.valueOf(rawDifficulty);
        } catch (IllegalArgumentException exception) {
            throw new CustomException(ResponseCode.INVALID_INPUT_VALUE);
        }
    }
}
