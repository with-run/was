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
import kr.withrun.was.global.common.entity.BaseEntity;

@Entity
@Table(name = "reward_draw_pools")
public class RewardDrawPool extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reward_draw_pool_id")
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "cards_per_draw", nullable = false)
    private Integer cardsPerDraw;

    @Column(name = "miss_weight", nullable = false)
    private Integer missWeight;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "miss_reward_item_id",
            foreignKey = @ForeignKey(name = "fk_reward_draw_pools_miss_reward_item")
    )
    private RewardItem missRewardItem;

    protected RewardDrawPool() {
    }

    public static RewardDrawPool create(String name, int cardsPerDraw, int missWeight) {
        return create(name, cardsPerDraw, missWeight, null);
    }

    public static RewardDrawPool create(String name, int cardsPerDraw, int missWeight, RewardItem missRewardItem) {
        RewardDrawPool rewardDrawPool = new RewardDrawPool();
        rewardDrawPool.name = name;
        rewardDrawPool.active = false;
        rewardDrawPool.cardsPerDraw = cardsPerDraw;
        rewardDrawPool.missWeight = missWeight;
        rewardDrawPool.missRewardItem = missRewardItem;
        return rewardDrawPool;
    }

    public void updateConfiguration(String name, int cardsPerDraw, int missWeight) {
        updateConfiguration(name, cardsPerDraw, missWeight, null);
    }

    public void updateConfiguration(String name, int cardsPerDraw, int missWeight, RewardItem missRewardItem) {
        this.name = name;
        this.cardsPerDraw = cardsPerDraw;
        this.missWeight = missWeight;
        this.missRewardItem = missRewardItem;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isActive() {
        return active;
    }

    public Integer getCardsPerDraw() {
        return cardsPerDraw;
    }

    public Integer getMissWeight() {
        return missWeight;
    }

    public RewardItem getMissRewardItem() {
        return missRewardItem;
    }
}
