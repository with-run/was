package kr.withrun.was.domain.course.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import kr.withrun.was.global.common.entity.BaseEntity;
import kr.withrun.was.global.common.type.Difficulty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "course_difficulties")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseDifficulty extends BaseEntity {

    @Id
    @Column(name = "course_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, length = 16)
    private Difficulty difficulty;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(
            name = "course_id",
            foreignKey = @ForeignKey(name = "fk_course_difficulties_course")
    )
    private Course course;

    @Builder
    private CourseDifficulty(Difficulty difficulty, Course course) {
        this.difficulty = difficulty;
        this.course = course;
    }

    public static CourseDifficulty create(Course course, Difficulty difficulty) {
        return CourseDifficulty.builder()
                .course(course)
                .difficulty(difficulty)
                .build();
    }

}
