package kr.withrun.was.domain.user.entity;

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
import jakarta.persistence.UniqueConstraint;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.global.common.entity.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Entity
@Table(
        name = "user_preference_course_types",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_user_preference_course_types_user_type",
                columnNames = {"user_id", "course_type"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPreferenceCourseType extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_preference_course_type_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "course_type", nullable = false, length = 32)
    private CourseType courseType;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_preference_course_type_user_running_preference")
    )
    private UserRunningPreference userRunningPreference;

    public static UserPreferenceCourseType create(CourseType courseType) {
        UserPreferenceCourseType preferenceCourseType = new UserPreferenceCourseType();
        preferenceCourseType.courseType = courseType;
        return preferenceCourseType;
    }
}
