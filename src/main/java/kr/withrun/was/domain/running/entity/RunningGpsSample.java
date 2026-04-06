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
        name = "running_gps_samples",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_running_gps_samples_session_time",
                        columnNames = {"running_session_id", "sampled_at"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RunningGpsSample extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "running_gps_sample_id")
    private Long id;

    @Column(name = "sampled_at", nullable = false)
    private LocalDateTime sampledAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "running_session_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_running_gps_samples_running_session")
    )
    private RunningSession runningSession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_running_gps_samples_user")
    )
    private User user;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "altitude_m")
    private Double altitudeM;

    @Column(name = "accuracy_m")
    private Double accuracyM;

    @Column(name = "bearing_deg")
    private Double bearingDeg;

    @Column(name = "speed_mps")
    private Double speedMps;

    @Column(name = "pace_sec_per_km")
    private Integer paceSecPerKm;

    @Column(name = "distance_m")
    private Integer distanceM;

    @Column(name = "cadence_spm")
    private Short cadenceSpm;

    @Builder
    private RunningGpsSample (
            Double bearingDeg,
            Double accuracyM,
            Double altitudeM,
            Double longitude,
            Double latitude,
            Double speedMps,
            Integer paceSecPerKm,
            Integer distanceM,
            Short cadenceSpm,
            User user,
            RunningSession runningSession,
            LocalDateTime sampledAt,
            Long id
    ) {
        this.bearingDeg = bearingDeg;
        this.accuracyM = accuracyM;
        this.altitudeM = altitudeM;
        this.longitude = longitude;
        this.latitude = latitude;
        this.speedMps = speedMps;
        this.paceSecPerKm = paceSecPerKm;
        this.distanceM = distanceM;
        this.cadenceSpm = cadenceSpm;
        this.user = user;
        this.runningSession = runningSession;
        this.sampledAt = sampledAt;
        this.id = id;
    }

    /**
     * 러닝 세션 중 수집된 GPS 정보를 기반으로 엔티티를 생성하는
     * 정적 팩토리 메서드
     *
     * @param bearingDeg 진행 방향 (degree)
     * @param accuracyM 위치 정확도 (미터)
     * @param altitudeM 고도 (미터)
     * @param longitude 경도 (소수)
     * @param latitude 위도 (소수)
     * @param speedMps 순간 속도 (m/s)
     * @param paceSecPerKm 순간 페이스 (sec/km)
     * @param distanceM 누적 거리 (m)
     * @param cadenceSpm 케이던스 (spm)
     * @param user
     * @param runningSession
     * @param sampledAt GPS 로그 수집 시간
     * @return
     */
    public static RunningGpsSample create (
            User user,
            RunningSession runningSession,
            Double bearingDeg,
            Double accuracyM,
            Double altitudeM,
            Double longitude,
            Double latitude,
            Double speedMps,
            Integer paceSecPerKm,
            Integer distanceM,
            Short cadenceSpm,
            LocalDateTime sampledAt
    ) {
        return RunningGpsSample.builder()
                .bearingDeg(bearingDeg)
                .accuracyM(accuracyM)
                .altitudeM(altitudeM)
                .longitude(longitude)
                .latitude(latitude)
                .speedMps(speedMps)
                .paceSecPerKm(paceSecPerKm)
                .distanceM(distanceM)
                .cadenceSpm(cadenceSpm)
                .user(user)
                .runningSession(runningSession)
                .sampledAt(sampledAt)
                .build();
    }
}
