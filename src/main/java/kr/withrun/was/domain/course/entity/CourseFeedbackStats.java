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
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.global.common.entity.BaseEntity;
import kr.withrun.was.global.common.type.Difficulty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Entity
@Table(name = "course_feedback_stats")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseFeedbackStats extends BaseEntity {

    @Id
    @Column(name = "course_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(
            name = "course_id",
            foreignKey = @ForeignKey(name = "fk_course_feedback_stats_course")
    )
    private Course course;

    @Column(name = "like_count", nullable = false)
    private long likeCount;

    @Column(name = "bookmark_count", nullable = false)
    private long bookmarkCount;

    @Column(name = "completion_count", nullable = false)
    private long completionCount;

    @Column(name = "review_count", nullable = false)
    private long reviewCount;

    @Column(name = "rating_sum", nullable = false)
    private double ratingSum;

    @Column(name = "easy_review_count", nullable = false)
    private long easyReviewCount;

    @Column(name = "medium_review_count", nullable = false)
    private long mediumReviewCount;

    @Column(name = "hard_review_count", nullable = false)
    private long hardReviewCount;

    @Column(name = "riverside_type_review_count", nullable = false)
    private long riversideTypeReviewCount;

    @Column(name = "park_type_review_count", nullable = false)
    private long parkTypeReviewCount;

    @Column(name = "mountain_trail_type_review_count", nullable = false)
    private long mountainTrailTypeReviewCount;

    @Column(name = "track_type_review_count", nullable = false)
    private long trackTypeReviewCount;

    @Column(name = "urban_type_review_count", nullable = false)
    private long urbanTypeReviewCount;

    @Column(name = "other_type_review_count", nullable = false)
    private long otherTypeReviewCount;

    private CourseFeedbackStats(Course course) {
        this.course = course;
    }

    public static CourseFeedbackStats create(Course course) {
        return new CourseFeedbackStats(course);
    }

    public void addLike() {
        likeCount += 1;
    }

    public void removeLike() {
        if (likeCount > 0) {
            likeCount -= 1;
        }
    }

    public void addBookmark() {
        bookmarkCount += 1;
    }

    public void removeBookmark() {
        if (bookmarkCount > 0) {
            bookmarkCount -= 1;
        }
    }

    public void addCompletion() {
        completionCount += 1;
    }

    public void addReview(Integer rating, Difficulty difficulty, List<CourseType> courseTypes) {
        if (rating != null) {
            reviewCount += 1;
            ratingSum += rating;
        }

        if (difficulty != null) {
            switch (difficulty) {
                case EASY -> easyReviewCount += 1;
                case MEDIUM -> mediumReviewCount += 1;
                case HARD -> hardReviewCount += 1;
            }
        }

        if (courseTypes == null) {
            return;
        }
        for (CourseType courseType : courseTypes) {
            if (courseType == null) {
                continue;
            }
            switch (courseType) {
                case RIVERSIDE -> riversideTypeReviewCount += 1;
                case PARK -> parkTypeReviewCount += 1;
                case MOUNTAIN_TRAIL -> mountainTrailTypeReviewCount += 1;
                case TRACK -> trackTypeReviewCount += 1;
                case URBAN -> urbanTypeReviewCount += 1;
                case OTHER -> otherTypeReviewCount += 1;
            }
        }
    }

    public double getAverageRating() {
        if (reviewCount <= 0) {
            return 0.0;
        }
        return ratingSum / reviewCount;
    }

    public long getTotalDifficultyFeedbackCount() {
        return easyReviewCount + mediumReviewCount + hardReviewCount;
    }

    public long getTotalCourseTypeFeedbackCount() {
        return riversideTypeReviewCount
                + parkTypeReviewCount
                + mountainTrailTypeReviewCount
                + trackTypeReviewCount
                + urbanTypeReviewCount
                + otherTypeReviewCount;
    }
}

