package kr.withrun.was.domain.running.entity;


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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import kr.withrun.was.domain.running.type.GhostResultStatus;
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
        name = "ghost_running_results",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_ghost_running_results_session",
                        columnNames = "running_session_id"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GhostRunningResult extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ghost_running_result_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_status", nullable = false, length = 16)
    private GhostResultStatus resultStatus;

    @Column(name = "point", nullable = false)
    private Integer point = 0;

    @Column(name = "time_gap_sec", nullable = false)
    private Integer timeGapSec = 0;

    @Column(name = "distance_gap_m", nullable = false)
    private Integer distanceGapM = 0;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "running_session_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ghost_running_results_running_session")
    )
    private RunningSession runningSession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "ghost_target_running_session_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ghost_running_results_ghost_target_running_session")
    )
    private RunningSession ghostTargetRunningSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "target_user_id",
            foreignKey = @ForeignKey(name = "fk_ghost_running_results_target_user")
    )
    private User targetUser;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (timeGapSec == null) {
            timeGapSec = 0;
        }
        if (distanceGapM == null) {
            distanceGapM = 0;
        }
        if (point == null) {
            point = 0;
        }
    }

    @Builder
    private GhostRunningResult(
            GhostResultStatus resultStatus,
            int point,
            int timeGapSec,
            int distanceGapM,
            RunningSession runningSession,
            RunningSession ghostTargetRunningSession,
            User targetUser
    ) {
        this.resultStatus = resultStatus;
        this.point = point;
        this.timeGapSec = timeGapSec;
        this.distanceGapM = distanceGapM;
        this.runningSession = runningSession;
        this.ghostTargetRunningSession = ghostTargetRunningSession;
        this.targetUser = targetUser;
    }

    public static GhostRunningResult create(GhostResultStatus resultStatus,
                                            int point,
                                            int timeGapSec,
                                            int distanceGapM,
                                            RunningSession runningSession,
                                            RunningSession ghostTargetRunningSession,
                                            User targetUser
    ) {
        return GhostRunningResult.builder()
                .resultStatus(resultStatus)
                .point(point)
                .timeGapSec(timeGapSec)
                .distanceGapM(distanceGapM)
                .runningSession(runningSession)
                .ghostTargetRunningSession(ghostTargetRunningSession)
                .targetUser(targetUser)
                .build();
    }
}
