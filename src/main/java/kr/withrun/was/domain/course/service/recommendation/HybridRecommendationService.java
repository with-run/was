package kr.withrun.was.domain.course.service.recommendation;

import kr.withrun.was.domain.course.entity.CourseFeature;
import kr.withrun.was.domain.course.entity.CourseFeedbackStats;
import kr.withrun.was.domain.course.repository.CourseFeatureRepository;
import kr.withrun.was.domain.course.repository.CourseFeedbackStatsRepository;
import kr.withrun.was.domain.course.repository.UserCourseInteractionRepository;
import kr.withrun.was.domain.course.repository.query.dto.CourseCollaborativeScoreRow;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.entity.UserPreferenceCourseType;
import kr.withrun.was.domain.user.entity.UserRunningPreference;
import kr.withrun.was.domain.user.repository.UserRepository;
import kr.withrun.was.global.common.type.Difficulty;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class HybridRecommendationService {

    private static final double DEFAULT_PREFERRED_DISTANCE_M = 5_000.0;
    private static final double DEFAULT_PREFERRED_DIFFICULTY_SCORE = 0.5;
    private static final double DEFAULT_QUALITY_SCORE = 0.3;
    private static final double MAX_DISTANCE_FOR_NORMALIZATION_M = 20_000.0;
    private static final int CF_RECENT_INTERACTION_LIMIT = 30;

    @Value("${app.recommendation.weight.location:0.45}")
    private double locationWeight;

    @Value("${app.recommendation.weight.cbf:0.45}")
    private double cbfWeight;

    @Value("${app.recommendation.weight.quality:0.10}")
    private double qualityWeight;

    @Value("${app.recommendation.weight.cf-alpha:0.20}")
    private double cfAlpha;

    @Value("${app.recommendation.feedback.max-beta:0.50}")
    private double maxFeedbackBlendBeta;

    @Value("${app.recommendation.feedback.review-confidence-count:10}")
    private int reviewConfidenceCount;

    private final CourseFeatureRepository courseFeatureRepository;
    private final CourseFeedbackStatsRepository courseFeedbackStatsRepository;
    private final UserCourseInteractionRepository userCourseInteractionRepository;
    private final UserRepository userRepository;

    public Map<Long, CourseRecommendationScore> scoreNearbyCourses(
            Long userId,
            int radiusM,
            List<NearbyRecommendationInput> candidates
    ) {
        if (candidates == null || candidates.isEmpty()) {
            return Map.of();
        }

        List<Long> candidateCourseIds = candidates.stream()
                .map(NearbyRecommendationInput::courseId)
                .filter(id -> id != null)
                .toList();

        Map<Long, CourseFeature> featureByCourseId = courseFeatureRepository.findAllById(candidateCourseIds)
                .stream()
                .collect(Collectors.toMap(CourseFeature::getId, Function.identity()));
        Map<Long, CourseFeedbackStats> feedbackByCourseId = courseFeedbackStatsRepository.findAllById(candidateCourseIds)
                .stream()
                .collect(Collectors.toMap(CourseFeedbackStats::getId, Function.identity()));

        UserPreferenceVector userPreferenceVector = resolveUserPreferenceVector(userId);
        List<Long> recentInteractedCourseIds = resolveRecentInteractedCourseIds(userId);
        Map<Long, Double> cfScoresByCourseId = resolveCollaborativeScores(userId, recentInteractedCourseIds, candidateCourseIds);

        Map<Long, CourseRecommendationScore> scoreByCourseId = new HashMap<>();
        for (NearbyRecommendationInput candidate : candidates) {
            if (candidate.courseId() == null) {
                continue;
            }

            CourseBaseVector baseVector = resolveCourseBaseVector(candidate, featureByCourseId.get(candidate.courseId()));
            CourseFeedbackVector feedbackVector = resolveCourseFeedbackVector(baseVector, feedbackByCourseId.get(candidate.courseId()));

            double locationScore = calculateLocationScore(candidate.distanceFromUserM(), radiusM);
            double cbfScore = calculateCbfScore(userPreferenceVector, candidate.courseDistanceM(), feedbackVector);
            double qualityScore = calculateQualityScore(feedbackByCourseId.get(candidate.courseId()));
            double baseScore = locationWeight * locationScore
                    + cbfWeight * cbfScore
                    + qualityWeight * qualityScore;

            double cfScore = clamp(cfScoresByCourseId.getOrDefault(candidate.courseId(), 0.0));
            double finalScore = (1.0 - clamp(cfAlpha)) * baseScore + clamp(cfAlpha) * cfScore;

            scoreByCourseId.put(candidate.courseId(), new CourseRecommendationScore(
                    candidate.courseId(),
                    locationScore,
                    cbfScore,
                    qualityScore,
                    cfScore,
                    baseScore,
                    finalScore
            ));
        }

        return scoreByCourseId;
    }

    private UserPreferenceVector resolveUserPreferenceVector(Long userId) {
        if (userId == null) {
            return defaultUserPreferenceVector();
        }

        User user = userRepository.findNotDeletedUser(userId).orElse(null);
        if (user == null || user.getUserRunningPreference() == null) {
            return defaultUserPreferenceVector();
        }

        UserRunningPreference runningPreference = user.getUserRunningPreference();
        double preferredDistanceM = runningPreference.getPreferredDistanceKm() == null
                ? DEFAULT_PREFERRED_DISTANCE_M
                : runningPreference.getPreferredDistanceKm() * 1_000.0;
        double preferredDifficultyScore = difficultyToScore(runningPreference.getPreferredDifficulty());
        Map<CourseType, Double> preferredTypeScores = toPreferredTypeScoreMap(runningPreference.getCourseTypes());

        return new UserPreferenceVector(
                preferredDistanceM,
                preferredDifficultyScore,
                preferredTypeScores
        );
    }

    private UserPreferenceVector defaultUserPreferenceVector() {
        Map<CourseType, Double> equalTypeScores = new EnumMap<>(CourseType.class);
        double score = 1.0 / CourseType.values().length;
        for (CourseType courseType : CourseType.values()) {
            equalTypeScores.put(courseType, score);
        }
        return new UserPreferenceVector(
                DEFAULT_PREFERRED_DISTANCE_M,
                DEFAULT_PREFERRED_DIFFICULTY_SCORE,
                equalTypeScores
        );
    }

    private Map<CourseType, Double> toPreferredTypeScoreMap(List<UserPreferenceCourseType> preferredCourseTypes) {
        Map<CourseType, Double> preferredTypeScores = new EnumMap<>(CourseType.class);
        for (CourseType courseType : CourseType.values()) {
            preferredTypeScores.put(courseType, 0.0);
        }

        if (preferredCourseTypes == null || preferredCourseTypes.isEmpty()) {
            double equalScore = 1.0 / CourseType.values().length;
            for (CourseType courseType : CourseType.values()) {
                preferredTypeScores.put(courseType, equalScore);
            }
            return preferredTypeScores;
        }

        Set<CourseType> uniqueTypes = preferredCourseTypes.stream()
                .map(UserPreferenceCourseType::getCourseType)
                .filter(type -> type != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (uniqueTypes.isEmpty()) {
            double equalScore = 1.0 / CourseType.values().length;
            for (CourseType courseType : CourseType.values()) {
                preferredTypeScores.put(courseType, equalScore);
            }
            return preferredTypeScores;
        }

        double assignedScore = 1.0 / uniqueTypes.size();
        for (CourseType uniqueType : uniqueTypes) {
            preferredTypeScores.put(uniqueType, assignedScore);
        }
        return preferredTypeScores;
    }

    private CourseBaseVector resolveCourseBaseVector(NearbyRecommendationInput candidate, CourseFeature courseFeature) {
        if (courseFeature != null) {
            return new CourseBaseVector(
                    clamp(courseFeature.getDistanceNorm()),
                    clamp(courseFeature.getDifficultyScore()),
                    normalizeTypeScoreMap(courseFeature.toCourseTypeScoreMap())
            );
        }

        Map<CourseType, Double> typeScores = new EnumMap<>(CourseType.class);
        for (CourseType courseType : CourseType.values()) {
            typeScores.put(courseType, 0.0);
        }
        List<CourseType> courseTypes = candidate.courseTypes() == null ? List.of() : candidate.courseTypes().stream()
                .filter(type -> type != null)
                .distinct()
                .toList();
        if (!courseTypes.isEmpty()) {
            double weight = 1.0 / courseTypes.size();
            for (CourseType courseType : courseTypes) {
                typeScores.put(courseType, weight);
            }
        }

        return new CourseBaseVector(
                normalizeDistance(candidate.courseDistanceM()),
                difficultyToScore(candidate.difficulty()),
                typeScores
        );
    }

    private CourseFeedbackVector resolveCourseFeedbackVector(
            CourseBaseVector baseVector,
            CourseFeedbackStats courseFeedbackStats
    ) {
        if (courseFeedbackStats == null) {
            return new CourseFeedbackVector(baseVector.difficultyScore(), baseVector.courseTypeScores(), 0.0);
        }

        long reviewCount = courseFeedbackStats.getReviewCount();
        double reviewConfidence = reviewCount <= 0
                ? 0.0
                : Math.min(1.0, reviewCount / (double) Math.max(1, reviewConfidenceCount));
        double beta = clamp(Math.min(maxFeedbackBlendBeta, reviewConfidence));

        double feedbackDifficultyScore = resolveFeedbackDifficultyScore(courseFeedbackStats, baseVector.difficultyScore());
        Map<CourseType, Double> feedbackTypeScores = resolveFeedbackTypeScores(courseFeedbackStats, baseVector.courseTypeScores());

        Map<CourseType, Double> blendedTypeScores = new EnumMap<>(CourseType.class);
        for (CourseType courseType : CourseType.values()) {
            double baseScore = baseVector.courseTypeScores().getOrDefault(courseType, 0.0);
            double feedbackScore = feedbackTypeScores.getOrDefault(courseType, baseScore);
            blendedTypeScores.put(courseType, clamp((1.0 - beta) * baseScore + beta * feedbackScore));
        }

        double blendedDifficultyScore = clamp((1.0 - beta) * baseVector.difficultyScore() + beta * feedbackDifficultyScore);
        return new CourseFeedbackVector(blendedDifficultyScore, normalizeTypeScoreMap(blendedTypeScores), beta);
    }

    private double resolveFeedbackDifficultyScore(CourseFeedbackStats courseFeedbackStats, double fallbackScore) {
        long totalDifficultyCount = courseFeedbackStats.getTotalDifficultyFeedbackCount();
        if (totalDifficultyCount <= 0) {
            return fallbackScore;
        }
        double weighted = (0.0 * courseFeedbackStats.getEasyReviewCount()
                + 0.5 * courseFeedbackStats.getMediumReviewCount()
                + 1.0 * courseFeedbackStats.getHardReviewCount()) / totalDifficultyCount;
        return clamp(weighted);
    }

    private Map<CourseType, Double> resolveFeedbackTypeScores(
            CourseFeedbackStats courseFeedbackStats,
            Map<CourseType, Double> fallbackTypeScores
    ) {
        long totalTypeCount = courseFeedbackStats.getTotalCourseTypeFeedbackCount();
        if (totalTypeCount <= 0) {
            return fallbackTypeScores;
        }

        Map<CourseType, Double> scores = new EnumMap<>(CourseType.class);
        scores.put(CourseType.RIVERSIDE, courseFeedbackStats.getRiversideTypeReviewCount() / (double) totalTypeCount);
        scores.put(CourseType.PARK, courseFeedbackStats.getParkTypeReviewCount() / (double) totalTypeCount);
        scores.put(CourseType.MOUNTAIN_TRAIL, courseFeedbackStats.getMountainTrailTypeReviewCount() / (double) totalTypeCount);
        scores.put(CourseType.TRACK, courseFeedbackStats.getTrackTypeReviewCount() / (double) totalTypeCount);
        scores.put(CourseType.URBAN, courseFeedbackStats.getUrbanTypeReviewCount() / (double) totalTypeCount);
        scores.put(CourseType.OTHER, courseFeedbackStats.getOtherTypeReviewCount() / (double) totalTypeCount);
        return scores;
    }

    private double calculateLocationScore(Integer distanceFromUserM, int radiusM) {
        if (distanceFromUserM == null || radiusM <= 0) {
            return 0.0;
        }
        double ratio = 1.0 - Math.min(1.0, distanceFromUserM / (double) radiusM);
        return clamp(ratio * ratio);
    }

    private double calculateCbfScore(
            UserPreferenceVector userPreferenceVector,
            Integer courseDistanceM,
            CourseFeedbackVector courseFeedbackVector
    ) {
        double difficultySimilarity = 1.0 - Math.abs(
                userPreferenceVector.preferredDifficultyScore() - courseFeedbackVector.effectiveDifficultyScore()
        );

        double preferredDistanceM = Math.max(1_000.0, userPreferenceVector.preferredDistanceM());
        double distanceSimilarity = 1.0 - Math.min(
                1.0,
                Math.abs((courseDistanceM == null ? preferredDistanceM : courseDistanceM.doubleValue()) - preferredDistanceM)
                        / preferredDistanceM
        );
        double typeSimilarity = cosineSimilarity(
                userPreferenceVector.preferredTypeScores(),
                courseFeedbackVector.effectiveCourseTypeScores()
        );

        return clamp(0.35 * difficultySimilarity + 0.30 * distanceSimilarity + 0.35 * typeSimilarity);
    }

    private double calculateQualityScore(CourseFeedbackStats courseFeedbackStats) {
        if (courseFeedbackStats == null) {
            return DEFAULT_QUALITY_SCORE;
        }

        double ratingScore = clamp(courseFeedbackStats.getAverageRating() / 5.0);
        double likeScore = logNormalized(courseFeedbackStats.getLikeCount(), 50.0);
        double bookmarkScore = logNormalized(courseFeedbackStats.getBookmarkCount(), 50.0);
        double completionScore = logNormalized(courseFeedbackStats.getCompletionCount(), 50.0);

        return clamp(0.45 * ratingScore + 0.25 * likeScore + 0.10 * bookmarkScore + 0.20 * completionScore);
    }

    private List<Long> resolveRecentInteractedCourseIds(Long userId) {
        if (userId == null) {
            return List.of();
        }

        List<Long> recentCourseIds = userCourseInteractionRepository.findRecentInteractedCourseIds(
                userId,
                CF_RECENT_INTERACTION_LIMIT
        );
        if (recentCourseIds.isEmpty()) {
            return List.of();
        }

        Set<Long> deduplicated = new LinkedHashSet<>();
        for (Long courseId : recentCourseIds) {
            if (courseId == null) {
                continue;
            }
            deduplicated.add(courseId);
            if (deduplicated.size() >= CF_RECENT_INTERACTION_LIMIT) {
                break;
            }
        }
        return new ArrayList<>(deduplicated);
    }

    private Map<Long, Double> resolveCollaborativeScores(
            Long userId,
            List<Long> seedCourseIds,
            List<Long> candidateCourseIds
    ) {
        if (userId == null || seedCourseIds == null || seedCourseIds.isEmpty()
                || candidateCourseIds == null || candidateCourseIds.isEmpty()) {
            return Map.of();
        }

        List<CourseCollaborativeScoreRow> scoreRows = userCourseInteractionRepository.findCollaborativeScoreRows(
                userId,
                seedCourseIds,
                candidateCourseIds
        );
        if (scoreRows.isEmpty()) {
            return Map.of();
        }

        long maxCount = scoreRows.stream()
                .map(CourseCollaborativeScoreRow::similarUserCount)
                .filter(count -> count != null && count > 0)
                .max(Comparator.naturalOrder())
                .orElse(1L);

        return scoreRows.stream()
                .filter(row -> row.courseId() != null && row.similarUserCount() != null)
                .collect(Collectors.toMap(
                        CourseCollaborativeScoreRow::courseId,
                        row -> clamp(row.similarUserCount() / (double) maxCount),
                        (left, right) -> right
                ));
    }

    private Map<CourseType, Double> normalizeTypeScoreMap(Map<CourseType, Double> rawTypeScores) {
        Map<CourseType, Double> normalized = new EnumMap<>(CourseType.class);
        if (rawTypeScores == null || rawTypeScores.isEmpty()) {
            for (CourseType courseType : CourseType.values()) {
                normalized.put(courseType, 0.0);
            }
            return normalized;
        }

        double sum = 0.0;
        for (CourseType courseType : CourseType.values()) {
            double value = clamp(rawTypeScores.getOrDefault(courseType, 0.0));
            normalized.put(courseType, value);
            sum += value;
        }

        if (sum <= 0.0) {
            return normalized;
        }
        for (CourseType courseType : CourseType.values()) {
            normalized.put(courseType, normalized.get(courseType) / sum);
        }
        return normalized;
    }

    private double cosineSimilarity(
            Map<CourseType, Double> leftVector,
            Map<CourseType, Double> rightVector
    ) {
        if (leftVector == null || rightVector == null) {
            return 0.0;
        }

        double dot = 0.0;
        double leftNorm = 0.0;
        double rightNorm = 0.0;
        for (CourseType courseType : CourseType.values()) {
            double left = clamp(leftVector.getOrDefault(courseType, 0.0));
            double right = clamp(rightVector.getOrDefault(courseType, 0.0));
            dot += left * right;
            leftNorm += left * left;
            rightNorm += right * right;
        }

        if (leftNorm <= 0.0 || rightNorm <= 0.0) {
            return 0.0;
        }
        return clamp(dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm)));
    }

    private double normalizeDistance(Integer distanceM) {
        if (distanceM == null || distanceM <= 0) {
            return 0.0;
        }
        return clamp(distanceM / MAX_DISTANCE_FOR_NORMALIZATION_M);
    }

    private double difficultyToScore(Difficulty difficulty) {
        if (difficulty == null) {
            return DEFAULT_PREFERRED_DIFFICULTY_SCORE;
        }
        return switch (difficulty) {
            case EASY -> 0.0;
            case MEDIUM -> 0.5;
            case HARD -> 1.0;
        };
    }

    private double logNormalized(long value, double denominatorBase) {
        if (value <= 0) {
            return 0.0;
        }
        return clamp(Math.log1p(value) / Math.log1p(Math.max(1.0, denominatorBase)));
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

    private record UserPreferenceVector(
            double preferredDistanceM,
            double preferredDifficultyScore,
            Map<CourseType, Double> preferredTypeScores
    ) {
    }

    private record CourseBaseVector(
            double distanceNorm,
            double difficultyScore,
            Map<CourseType, Double> courseTypeScores
    ) {
    }

    private record CourseFeedbackVector(
            double effectiveDifficultyScore,
            Map<CourseType, Double> effectiveCourseTypeScores,
            double feedbackBlendBeta
    ) {
    }
}
