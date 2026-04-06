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
import jakarta.persistence.UniqueConstraint;
import kr.withrun.was.domain.course.type.CourseInteractionType;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.global.common.entity.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "user_course_interactions",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_user_course_interactions_source",
                columnNames = {"user_id", "course_id", "interaction_type", "source", "source_ref"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCourseInteraction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_course_interaction_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_course_interactions_user")
    )
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "course_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_course_interactions_course")
    )
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(name = "interaction_type", nullable = false, length = 16)
    private CourseInteractionType interactionType;

    @Column(name = "weight", nullable = false)
    private int weight;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "source", nullable = false, length = 32)
    private String source;

    @Column(name = "source_ref", nullable = false, length = 64)
    private String sourceRef;

    @Builder
    private UserCourseInteraction(
            User user,
            Course course,
            CourseInteractionType interactionType,
            int weight,
            LocalDateTime occurredAt,
            String source,
            String sourceRef
    ) {
        this.user = user;
        this.course = course;
        this.interactionType = interactionType;
        this.weight = weight;
        this.occurredAt = occurredAt;
        this.source = source;
        this.sourceRef = sourceRef;
    }

    public static UserCourseInteraction create(
            User user,
            Course course,
            CourseInteractionType interactionType,
            int weight,
            LocalDateTime occurredAt,
            String source,
            String sourceRef
    ) {
        return UserCourseInteraction.builder()
                .user(user)
                .course(course)
                .interactionType(interactionType)
                .weight(weight)
                .occurredAt(occurredAt == null ? LocalDateTime.now() : occurredAt)
                .source(source == null || source.isBlank() ? "UNKNOWN" : source)
                .sourceRef(sourceRef == null || sourceRef.isBlank() ? "UNKNOWN" : sourceRef)
                .build();
    }
}

