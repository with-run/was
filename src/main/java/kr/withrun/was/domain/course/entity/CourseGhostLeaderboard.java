package kr.withrun.was.domain.course.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import kr.withrun.was.domain.running.entity.RunningSession;
import kr.withrun.was.domain.user.entity.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "course_ghost_leaderboard",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_course_ghost_leaderboard_user_course_session",
                columnNames = {"user_id", "course_id", "running_session_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseGhostLeaderboard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_ghost_leaderboard_id")
    private Long id;

    @Column(name = "point", nullable = false)
    private Integer point = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_course_ghost_leaderboard_user")
    )
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "course_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_course_ghost_leaderboard_course")
    )
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "running_session_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_course_ghost_leaderboard_running_session")
    )
    private RunningSession runningSession;

    private CourseGhostLeaderboard(
            User user,
            Course course,
            RunningSession runningSession,
            Integer point
    ) {
        this.user = user;
        this.course = course;
        this.runningSession = runningSession;
        this.point = point == null ? 0 : point;
    }

    public static CourseGhostLeaderboard create(
            User user,
            Course course,
            RunningSession runningSession,
            Integer point
    ) {
        return new CourseGhostLeaderboard(user, course, runningSession, point);
    }

    public void updatePoint(Integer point) {
        this.point = point == null ? 0 : point;
    }

    public void updatePointAndRunningSession(Integer point, RunningSession runningSession) {
        updatePoint(point);
        this.runningSession = runningSession;
    }

}
