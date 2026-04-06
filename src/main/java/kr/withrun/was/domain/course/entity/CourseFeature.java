package kr.withrun.was.domain.course.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import kr.withrun.was.global.common.entity.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.EnumMap;
import java.util.Map;

import static kr.withrun.was.domain.course.type.CourseType.MOUNTAIN_TRAIL;
import static kr.withrun.was.domain.course.type.CourseType.OTHER;
import static kr.withrun.was.domain.course.type.CourseType.PARK;
import static kr.withrun.was.domain.course.type.CourseType.RIVERSIDE;
import static kr.withrun.was.domain.course.type.CourseType.TRACK;
import static kr.withrun.was.domain.course.type.CourseType.URBAN;

@Getter
@Entity
@Table(name = "course_features")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseFeature extends BaseEntity {

    public static final String DEFAULT_FEATURE_VERSION = "v1";

    @Id
    @Column(name = "course_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(
            name = "course_id",
            foreignKey = @ForeignKey(name = "fk_course_features_course")
    )
    private Course course;

    @Column(name = "distance_norm", nullable = false)
    private double distanceNorm;

    @Column(name = "difficulty_score", nullable = false)
    private double difficultyScore;

    @Column(name = "type_riverside_score", nullable = false)
    private double typeRiversideScore;

    @Column(name = "type_park_score", nullable = false)
    private double typeParkScore;

    @Column(name = "type_mountain_trail_score", nullable = false)
    private double typeMountainTrailScore;

    @Column(name = "type_track_score", nullable = false)
    private double typeTrackScore;

    @Column(name = "type_urban_score", nullable = false)
    private double typeUrbanScore;

    @Column(name = "type_other_score", nullable = false)
    private double typeOtherScore;

    @Column(name = "feature_version", nullable = false, length = 32)
    private String featureVersion;

    private CourseFeature(Course course) {
        this.course = course;
        this.featureVersion = DEFAULT_FEATURE_VERSION;
    }

    public static CourseFeature create(Course course) {
        return new CourseFeature(course);
    }

    public void update(
            double distanceNorm,
            double difficultyScore,
            Map<kr.withrun.was.domain.course.type.CourseType, Double> courseTypeScores,
            String featureVersion
    ) {
        this.distanceNorm = clamp(distanceNorm);
        this.difficultyScore = clamp(difficultyScore);
        this.typeRiversideScore = clamp(courseTypeScores.getOrDefault(RIVERSIDE, 0.0));
        this.typeParkScore = clamp(courseTypeScores.getOrDefault(PARK, 0.0));
        this.typeMountainTrailScore = clamp(courseTypeScores.getOrDefault(MOUNTAIN_TRAIL, 0.0));
        this.typeTrackScore = clamp(courseTypeScores.getOrDefault(TRACK, 0.0));
        this.typeUrbanScore = clamp(courseTypeScores.getOrDefault(URBAN, 0.0));
        this.typeOtherScore = clamp(courseTypeScores.getOrDefault(OTHER, 0.0));
        this.featureVersion = featureVersion == null || featureVersion.isBlank()
                ? DEFAULT_FEATURE_VERSION
                : featureVersion;
    }

    public Map<kr.withrun.was.domain.course.type.CourseType, Double> toCourseTypeScoreMap() {
        Map<kr.withrun.was.domain.course.type.CourseType, Double> scores = new EnumMap<>(kr.withrun.was.domain.course.type.CourseType.class);
        scores.put(RIVERSIDE, typeRiversideScore);
        scores.put(PARK, typeParkScore);
        scores.put(MOUNTAIN_TRAIL, typeMountainTrailScore);
        scores.put(TRACK, typeTrackScore);
        scores.put(URBAN, typeUrbanScore);
        scores.put(OTHER, typeOtherScore);
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

