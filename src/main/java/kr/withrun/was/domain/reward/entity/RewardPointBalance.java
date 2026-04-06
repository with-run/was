package kr.withrun.was.domain.reward.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.global.common.entity.BaseEntity;

@Entity
@Table(name = "reward_point_balances")
public class RewardPointBalance extends BaseEntity {

    @Id
    @Column(name = "user_id")
    private Long id;

    @Column(name = "current_balance", nullable = false)
    private Integer currentBalance;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_reward_point_balances_user")
    )
    private User user;

    protected RewardPointBalance() {
    }

    public static RewardPointBalance create(User user) {
        RewardPointBalance rewardPointBalance = new RewardPointBalance();
        rewardPointBalance.user = user;
        rewardPointBalance.currentBalance = 0;
        return rewardPointBalance;
    }

    public Long getId() {
        return id;
    }

    public Integer getCurrentBalance() {
        return currentBalance;
    }

    public User getUser() {
        return user;
    }
}
