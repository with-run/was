package kr.withrun.was.domain.reward.entity;

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
import kr.withrun.was.domain.reward.type.RewardPointReason;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.global.common.entity.BaseEntity;

@Entity
@Table(
        name = "reward_point_histories",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_reward_point_histories_user_key",
                columnNames = {"user_id", "idempotency_key"}
        )
)
public class RewardPointHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reward_point_history_id")
    private Long id;

    @Column(name = "delta_point", nullable = false)
    private Integer deltaPoint;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 32)
    private RewardPointReason reason;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_reward_point_histories_user")
    )
    private User user;

    protected RewardPointHistory() {
    }

    public static RewardPointHistory create(
            User user,
            int deltaPoint,
            RewardPointReason reason,
            String idempotencyKey
    ) {
        RewardPointHistory rewardPointHistory = new RewardPointHistory();
        rewardPointHistory.user = user;
        rewardPointHistory.deltaPoint = deltaPoint;
        rewardPointHistory.reason = reason;
        rewardPointHistory.idempotencyKey = idempotencyKey;
        return rewardPointHistory;
    }

    public Long getId() {
        return id;
    }

    public Integer getDeltaPoint() {
        return deltaPoint;
    }

    public RewardPointReason getReason() {
        return reason;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public User getUser() {
        return user;
    }
}
