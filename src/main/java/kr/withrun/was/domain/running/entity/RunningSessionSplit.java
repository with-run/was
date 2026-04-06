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
import kr.withrun.was.global.common.entity.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "running_session_splits",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_running_session_splits_session_index",
                        columnNames = {"running_session_id", "split_index"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RunningSessionSplit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "running_session_split_id")
    private Long id;

    @Column(name = "split_index", nullable = false)
    private Integer splitIndex;

    @Column(name = "split_distance_m", nullable = false)
    private Integer splitDistanceM;

    @Column(name = "split_duration_sec", nullable = false)
    private Integer splitDurationSec;

    @Column(name = "split_pace_sec_per_km", nullable = false)
    private Integer splitPaceSecPerKm;

    @Column(name = "avg_heart_rate")
    private Integer avgHeartRate;

    @Column(name = "elevation_gain_m", nullable = false)
    private Integer elevationGainM = 0;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "running_session_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_running_session_splits_running_session")
    )
    private RunningSession runningSession;

    @Builder
    private RunningSessionSplit(
            Long id,
            Integer splitIndex,
            Integer splitDistanceM,
            Integer splitDurationSec,
            Integer splitPaceSecPerKm,
            Integer avgHeartRate,
            Integer elevationGainM,
            RunningSession runningSession
    ) {
        this.id = id;
        this.splitIndex = splitIndex;
        this.splitDistanceM = splitDistanceM;
        this.splitDurationSec = splitDurationSec;
        this.splitPaceSecPerKm = splitPaceSecPerKm;
        this.avgHeartRate = avgHeartRate;
        this.elevationGainM = elevationGainM;
        this.runningSession = runningSession;
    }

    public static RunningSessionSplit create(
            RunningSession runningSession,
            Integer splitIndex,
            Integer splitDistanceM,
            Integer splitDurationSec,
            Integer splitPaceSecPerKm,
            Integer avgHeartRate,
            Integer elevationGainM
    ) {
        return RunningSessionSplit.builder()
                .runningSession(runningSession)
                .splitIndex(splitIndex)
                .splitDistanceM(splitDistanceM)
                .splitDurationSec(splitDurationSec)
                .splitPaceSecPerKm(splitPaceSecPerKm)
                .avgHeartRate(avgHeartRate)
                .elevationGainM(elevationGainM)
                .build();
    }
}
