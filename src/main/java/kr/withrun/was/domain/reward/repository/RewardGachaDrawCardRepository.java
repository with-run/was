package kr.withrun.was.domain.reward.repository;

import kr.withrun.was.domain.reward.entity.RewardGachaDrawCard;
import kr.withrun.was.domain.reward.type.RewardGachaCardType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RewardGachaDrawCardRepository extends JpaRepository<RewardGachaDrawCard, Long> {

    List<RewardGachaDrawCard> findAllByRewardGachaDrawIdAndDeletedAtIsNullOrderByCardIndexAsc(Long rewardGachaDrawId);

    Page<RewardGachaDrawCard> findAllByRewardGachaDrawUserIdAndCardTypeAndDeletedAtIsNull(
            Long userId,
            RewardGachaCardType cardType,
            Pageable pageable
    );

    default Page<RewardGachaDrawCard> findInventoryByUserId(Long userId, Pageable pageable) {
        return findAllByRewardGachaDrawUserIdAndCardTypeAndDeletedAtIsNull(userId, RewardGachaCardType.REWARD, pageable);
    }
}
