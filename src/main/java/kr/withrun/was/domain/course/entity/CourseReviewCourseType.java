package kr.withrun.was.domain.course.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.global.common.entity.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "course_review_course_types")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseReviewCourseType extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_review_course_type_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "course_type")
    private CourseType courseType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "course_review_id",
            foreignKey = @ForeignKey(name = "fk_course_reviews_course_types_review")
    )
    private CourseReview courseReview;

    @Builder
    private CourseReviewCourseType(CourseType courseType, CourseReview courseReview) {
        this.courseType = courseType;
        this.courseReview = courseReview;
    }

    public static CourseReviewCourseType create(CourseReview courseReview, CourseType courseType) {
        return CourseReviewCourseType.builder()
                .courseReview(courseReview)
                .courseType(courseType)
                .build();
    }

}
