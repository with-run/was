package kr.withrun.was.domain.running.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
        name = "running_health_samples",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_running_health_samples_session_time",
                        columnNames = {"running_session_id", "sampled_at"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RunningHealthSample extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "running_health_sample_id")
    private Long id;

    @Column(name = "sampled_at", nullable = false)
    private LocalDateTime sampledAt;

    @Column(name = "heart_rate")
    private Short heartRate;

    @Column(name = "calories_kcal")
    private Double caloriesKcal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "running_session_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_running_health_samples_running_session")
    )
    private RunningSession runningSession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_running_health_samples_user")
    )
    private User user;

    @Builder
    private RunningHealthSample(
            Long id,
            LocalDateTime sampledAt,
            Short heartRate,
            Double caloriesKcal,
            RunningSession runningSession,
            User user
    ) {
        this.id = id;
        this.sampledAt = sampledAt;
        this.heartRate = heartRate;
        this.caloriesKcal = caloriesKcal;
        this.runningSession = runningSession;
        this.user = user;
    }

    public static RunningHealthSample create(
            User user,
            RunningSession runningSession,
            Short heartRate,
            Double caloriesKcal,
            LocalDateTime sampledAt
    ) {
        return RunningHealthSample.builder()
                .sampledAt(sampledAt)
                .heartRate(heartRate)
                .caloriesKcal(caloriesKcal)
                .runningSession(runningSession)
                .user(user)
                .build();
    }
}
