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
import kr.withrun.was.domain.reward.type.RewardGachaCardType;
import kr.withrun.was.global.common.entity.BaseEntity;

@Entity
@Table(name = "reward_gacha_draw_cards")
public class RewardGachaDrawCard extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reward_gacha_draw_card_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reward_gacha_draw_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reward_gacha_draw_cards_draw"))
    private RewardGachaDraw rewardGachaDraw;

    @Column(name = "card_index", nullable = false)
    private Integer cardIndex;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", nullable = false, length = 16)
    private RewardGachaCardType cardType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reward_item_id", foreignKey = @ForeignKey(name = "fk_reward_gacha_draw_cards_reward_item"))
    private RewardItem rewardItem;

    protected RewardGachaDrawCard() {
    }

    public static RewardGachaDrawCard create(
            RewardGachaDraw rewardGachaDraw,
            int cardIndex,
            RewardGachaCardType cardType,
            RewardItem rewardItem
    ) {
        RewardGachaDrawCard rewardGachaDrawCard = new RewardGachaDrawCard();
        rewardGachaDrawCard.rewardGachaDraw = rewardGachaDraw;
        rewardGachaDrawCard.cardIndex = cardIndex;
        rewardGachaDrawCard.cardType = cardType;
        rewardGachaDrawCard.rewardItem = rewardItem;
        return rewardGachaDrawCard;
    }

    public Long getId() {
        return id;
    }

    public RewardGachaDraw getRewardGachaDraw() {
        return rewardGachaDraw;
    }

    public Integer getCardIndex() {
        return cardIndex;
    }

    public RewardGachaCardType getCardType() {
        return cardType;
    }

    public RewardItem getRewardItem() {
        return rewardItem;
    }
}
