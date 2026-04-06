package kr.withrun.was.domain.course.service;

import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.entity.CourseFeature;
import kr.withrun.was.domain.course.entity.CourseFeedbackStats;
import kr.withrun.was.domain.course.entity.UserCourseInteraction;
import kr.withrun.was.domain.course.repository.CourseFeatureRepository;
import kr.withrun.was.domain.course.repository.CourseFeedbackStatsRepository;
import kr.withrun.was.domain.course.repository.UserCourseInteractionRepository;
import kr.withrun.was.domain.course.type.CourseInteractionType;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.global.common.type.Difficulty;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseSignalService {

    private static final int LIKE_WEIGHT = 1;
    private static final int BOOKMARK_WEIGHT = 2;
    private static final int REVIEW_WEIGHT = 3;
    private static final int COMPLETE_WEIGHT = 4;
    private static final String FEATURE_VERSION = "v1";

    private final UserCourseInteractionRepository userCourseInteractionRepository;
    private final CourseFeedbackStatsRepository courseFeedbackStatsRepository;
    private final CourseFeatureRepository courseFeatureRepository;

    public void recordLike(Course course, User user, Long sourceId) {
        saveInteractionIfAbsent(
                user,
                course,
                CourseInteractionType.LIKE,
                LIKE_WEIGHT,
                "COURSE_LIKE",
                toSourceRef(course, user, sourceId)
        );
        upsertFeedbackStats(course).addLike();
    }

    public void recordUnlike(Course course) {
        upsertFeedbackStats(course).removeLike();
    }

    public void recordBookmarkAdded(Course course, User user, Long sourceId, LocalDateTime occurredAt) {
        saveInteractionIfAbsent(
                user,
                course,
                CourseInteractionType.BOOKMARK,
                BOOKMARK_WEIGHT,
                "COURSE_BOOKMARK",
                toSourceRef(course, user, sourceId),
                occurredAt
        );
        upsertFeedbackStats(course).addBookmark();
    }

    public void recordBookmarkRemoved(Course course) {
        upsertFeedbackStats(course).removeBookmark();
    }

    public void recordReview(
            Course course,
            User user,
            Long reviewId,
            Integer rating,
            Difficulty submittedDifficulty,
            List<CourseType> submittedCourseTypes
    ) {
        saveInteractionIfAbsent(
                user,
                course,
                CourseInteractionType.REVIEW,
                REVIEW_WEIGHT,
                "COURSE_REVIEW",
                toSourceRef(course, user, reviewId)
        );
        upsertFeedbackStats(course).addReview(rating, submittedDifficulty, submittedCourseTypes);
    }

    public void recordCompletion(Course course, User user, Long runningSessionId) {
        saveInteractionIfAbsent(
                user,
                course,
                CourseInteractionType.COMPLETE,
                COMPLETE_WEIGHT,
                "RUNNING_SESSION",
                toSourceRef(course, user, runningSessionId)
        );
        upsertFeedbackStats(course).addCompletion();
    }

    public void upsertCourseFeature(Course course, Difficulty difficulty, List<CourseType> courseTypes) {
        CourseFeature courseFeature = courseFeatureRepository.findById(course.getId())
                .orElseGet(() -> courseFeatureRepository.save(CourseFeature.create(course)));
        courseFeature.update(
                normalizeDistance(course.getDistanceM()),
                difficultyToScore(difficulty),
                toCourseTypeScoreMap(courseTypes),
                FEATURE_VERSION
        );
    }

    private void saveInteractionIfAbsent(
            User user,
            Course course,
            CourseInteractionType interactionType,
            int weight,
            String source,
            String sourceRef
    ) {
        saveInteractionIfAbsent(user, course, interactionType, weight, source, sourceRef, LocalDateTime.now());
    }

    private void saveInteractionIfAbsent(
            User user,
            Course course,
            CourseInteractionType interactionType,
            int weight,
            String source,
            String sourceRef,
            LocalDateTime occurredAt
    ) {
        boolean exists = userCourseInteractionRepository.existsByUserIdAndCourseIdAndInteractionTypeAndSourceAndSourceRef(
                user.getId(),
                course.getId(),
                interactionType,
                source,
                sourceRef
        );
        if (exists) {
            return;
        }
        userCourseInteractionRepository.save(UserCourseInteraction.create(
                user,
                course,
                interactionType,
                weight,
                occurredAt,
                source,
                sourceRef
        ));
    }

    private CourseFeedbackStats upsertFeedbackStats(Course course) {
        return courseFeedbackStatsRepository.findById(course.getId())
                .orElseGet(() -> courseFeedbackStatsRepository.save(CourseFeedbackStats.create(course)));
    }

    private String toSourceRef(Course course, User user, Long sourceId) {
        if (sourceId != null) {
            return sourceId.toString();
        }
        return course.getId() + ":" + user.getId();
    }

    private double normalizeDistance(Integer distanceM) {
        if (distanceM == null || distanceM <= 0) {
            return 0.0;
        }
        return clamp(distanceM / 20_000.0);
    }

    private double difficultyToScore(Difficulty difficulty) {
        if (difficulty == null) {
            return 0.5;
        }
        return switch (difficulty) {
            case EASY -> 0.0;
            case MEDIUM -> 0.5;
            case HARD -> 1.0;
        };
    }

    private Map<CourseType, Double> toCourseTypeScoreMap(List<CourseType> courseTypes) {
        Map<CourseType, Double> scores = new EnumMap<>(CourseType.class);
        for (CourseType courseType : CourseType.values()) {
            scores.put(courseType, 0.0);
        }

        if (courseTypes == null || courseTypes.isEmpty()) {
            return scores;
        }

        List<CourseType> uniqueTypes = courseTypes.stream()
                .filter(type -> type != null)
                .distinct()
                .toList();
        if (uniqueTypes.isEmpty()) {
            return scores;
        }

        double assignedScore = 1.0 / uniqueTypes.size();
        for (CourseType uniqueType : uniqueTypes) {
            scores.put(uniqueType, assignedScore);
        }
        return scores;
    }

    private double clamp(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        if (value < 0.0) {
            return 0.0;
        }
        if (value > 1.0) {
            return 1.0;
        }
        return value;
    }
}

