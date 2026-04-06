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
import jakarta.persistence.UniqueConstraint;
import kr.withrun.was.global.common.entity.BaseEntity;

@Entity
@Table(
        name = "reward_draw_pool_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_reward_draw_pool_items_pool_reward_item",
                columnNames = {"reward_draw_pool_id", "reward_item_id"}
        )
)
public class RewardDrawPoolItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reward_draw_pool_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "reward_draw_pool_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_reward_draw_pool_items_pool")
    )
    private RewardDrawPool rewardDrawPool;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "reward_item_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_reward_draw_pool_items_reward_item")
    )
    private RewardItem rewardItem;

    @Column(name = "weight", nullable = false)
    private Integer weight;

    protected RewardDrawPoolItem() {
    }

    public static RewardDrawPoolItem create(RewardDrawPool rewardDrawPool, RewardItem rewardItem, int weight) {
        RewardDrawPoolItem rewardDrawPoolItem = new RewardDrawPoolItem();
        rewardDrawPoolItem.rewardDrawPool = rewardDrawPool;
        rewardDrawPoolItem.rewardItem = rewardItem;
        rewardDrawPoolItem.weight = weight;
        return rewardDrawPoolItem;
    }

    public Long getId() {
        return id;
    }

    public RewardDrawPool getRewardDrawPool() {
        return rewardDrawPool;
    }

    public RewardItem getRewardItem() {
        return rewardItem;
    }

    public Integer getWeight() {
        return weight;
    }
}
