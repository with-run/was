package kr.withrun.was.domain.reward.entity;

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
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.global.common.entity.BaseEntity;

@Entity
@Table(name = "reward_gacha_draws")
public class RewardGachaDraw extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reward_gacha_draw_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reward_gacha_draws_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reward_draw_pool_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reward_gacha_draws_pool"))
    private RewardDrawPool rewardDrawPool;

    @Column(name = "cards_per_draw", nullable = false)
    private Integer cardsPerDraw;

    protected RewardGachaDraw() {
    }

    public static RewardGachaDraw create(User user, RewardDrawPool rewardDrawPool, int cardsPerDraw) {
        RewardGachaDraw rewardGachaDraw = new RewardGachaDraw();
        rewardGachaDraw.user = user;
        rewardGachaDraw.rewardDrawPool = rewardDrawPool;
        rewardGachaDraw.cardsPerDraw = cardsPerDraw;
        return rewardGachaDraw;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public RewardDrawPool getRewardDrawPool() {
        return rewardDrawPool;
    }

    public Integer getCardsPerDraw() {
        return cardsPerDraw;
    }
}
