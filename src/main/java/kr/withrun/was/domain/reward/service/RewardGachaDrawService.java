package kr.withrun.was.domain.reward.service;

import kr.withrun.was.domain.file.service.CloudFrontSignedUrlService;
import kr.withrun.was.domain.reward.dto.RewardGachaDrawCardResponse;
import kr.withrun.was.domain.reward.dto.RewardGachaDrawResponse;
import kr.withrun.was.domain.reward.entity.RewardDrawPool;
import kr.withrun.was.domain.reward.entity.RewardDrawPoolItem;
import kr.withrun.was.domain.reward.entity.RewardGachaDraw;
import kr.withrun.was.domain.reward.entity.RewardGachaDrawCard;
import kr.withrun.was.domain.reward.entity.RewardItem;
import kr.withrun.was.domain.reward.entity.RewardPointHistory;
import kr.withrun.was.domain.reward.repository.RewardDrawPoolItemRepository;
import kr.withrun.was.domain.reward.repository.RewardDrawPoolRepository;
import kr.withrun.was.domain.reward.repository.RewardGachaDrawCardRepository;
import kr.withrun.was.domain.reward.repository.RewardGachaDrawRepository;
import kr.withrun.was.domain.reward.repository.RewardPointBalanceRepository;
import kr.withrun.was.domain.reward.repository.RewardPointHistoryRepository;
import kr.withrun.was.domain.reward.type.RewardGachaCardType;
import kr.withrun.was.domain.reward.type.RewardPointReason;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.repository.UserRepository;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class RewardGachaDrawService {

    private static final int DRAW_COST = 1;
    private static final int MAX_CARDS_PER_DRAW = 3;

    private final RewardDrawPoolRepository rewardDrawPoolRepository;
    private final RewardDrawPoolItemRepository rewardDrawPoolItemRepository;
    private final RewardPointBalanceRepository rewardPointBalanceRepository;
    private final RewardPointHistoryRepository rewardPointHistoryRepository;
    private final RewardGachaDrawRepository rewardGachaDrawRepository;
    private final RewardGachaDrawCardRepository rewardGachaDrawCardRepository;
    private final UserRepository userRepository;
    private final CloudFrontSignedUrlService cloudFrontSignedUrlService;
    private final RewardDrawRandomGenerator rewardDrawRandomGenerator;

    public RewardGachaDrawService(
            RewardDrawPoolRepository rewardDrawPoolRepository,
            RewardDrawPoolItemRepository rewardDrawPoolItemRepository,
            RewardPointBalanceRepository rewardPointBalanceRepository,
            RewardPointHistoryRepository rewardPointHistoryRepository,
            RewardGachaDrawRepository rewardGachaDrawRepository,
            RewardGachaDrawCardRepository rewardGachaDrawCardRepository,
            UserRepository userRepository,
            CloudFrontSignedUrlService cloudFrontSignedUrlService,
            RewardDrawRandomGenerator rewardDrawRandomGenerator
    ) {
        this.rewardDrawPoolRepository = rewardDrawPoolRepository;
        this.rewardDrawPoolItemRepository = rewardDrawPoolItemRepository;
        this.rewardPointBalanceRepository = rewardPointBalanceRepository;
        this.rewardPointHistoryRepository = rewardPointHistoryRepository;
        this.rewardGachaDrawRepository = rewardGachaDrawRepository;
        this.rewardGachaDrawCardRepository = rewardGachaDrawCardRepository;
        this.userRepository = userRepository;
        this.cloudFrontSignedUrlService = cloudFrontSignedUrlService;
        this.rewardDrawRandomGenerator = rewardDrawRandomGenerator;
    }

    @Transactional
    public RewardGachaDrawResponse draw(Long userId) {
        Long rewardDrawPoolId = rewardDrawPoolRepository.findByActiveTrueAndDeletedAtIsNull()
                .map(RewardDrawPool::getId)
                .orElseThrow(() -> new CustomException(ResponseCode.REWARD_DRAW_POOL_NOT_FOUND));

        if (rewardPointBalanceRepository.decreaseBalanceIfEnough(userId, DRAW_COST) == 0) {
            throw new CustomException(ResponseCode.REWARD_POINT_INSUFFICIENT);
        }

        RewardDrawPool rewardDrawPool = rewardDrawPoolRepository.findWithMissRewardItemByIdAndDeletedAtIsNull(rewardDrawPoolId)
                .orElseThrow(() -> new CustomException(ResponseCode.REWARD_DRAW_POOL_NOT_FOUND));
        List<RewardDrawPoolItem> rewardDrawPoolItems = rewardDrawPoolItemRepository
                .findWithRewardItemByRewardDrawPoolIdAndDeletedAtIsNullOrderByIdAsc(rewardDrawPoolId);
        int cardsPerDraw = Math.min(rewardDrawPool.getCardsPerDraw(), MAX_CARDS_PER_DRAW);

        User user = userRepository.getReferenceById(userId);
        RewardGachaDraw rewardGachaDraw = rewardGachaDrawRepository.save(
                RewardGachaDraw.create(user, rewardDrawPool, cardsPerDraw)
        );

        List<RewardGachaDrawCard> savedCards = rewardGachaDrawCardRepository.saveAll(
                drawCards(rewardGachaDraw, rewardDrawPool, rewardDrawPoolItems, cardsPerDraw)
        );

        rewardPointHistoryRepository.saveAndFlush(
                RewardPointHistory.create(
                        user,
                        -DRAW_COST,
                        RewardPointReason.GACHA_DRAW,
                        "reward:gacha-draw:%d".formatted(rewardGachaDraw.getId())
                )
        );

        int remainingBalance = rewardPointBalanceRepository.findByIdAndDeletedAtIsNull(userId)
                .map(balance -> balance.getCurrentBalance())
                .orElse(0);

        return new RewardGachaDrawResponse(
                rewardGachaDraw.getId(),
                DRAW_COST,
                remainingBalance,
                savedCards.stream().map(this::toResponse).toList(),
                rewardGachaDraw.getCreatedAt()
        );
    }

    private List<RewardGachaDrawCard> drawCards(
            RewardGachaDraw rewardGachaDraw,
            RewardDrawPool rewardDrawPool,
            List<RewardDrawPoolItem> rewardDrawPoolItems,
            int cardsPerDraw
    ) {
        int totalWeight = rewardDrawPool.getMissWeight()
                + rewardDrawPoolItems.stream().mapToInt(RewardDrawPoolItem::getWeight).sum();
        if (totalWeight <= 0) {
            throw new CustomException(ResponseCode.REWARD_DRAW_POOL_NOT_FOUND);
        }

        List<RewardGachaDrawCard> cards = new ArrayList<>();
        for (int cardIndex = 1; cardIndex <= cardsPerDraw; cardIndex++) {
            int drawValue = rewardDrawRandomGenerator.nextInt(totalWeight);
            cards.add(selectCard(rewardGachaDraw, rewardDrawPool, rewardDrawPoolItems, cardIndex, drawValue));
        }
        return cards;
    }

    private RewardGachaDrawCard selectCard(
            RewardGachaDraw rewardGachaDraw,
            RewardDrawPool rewardDrawPool,
            List<RewardDrawPoolItem> rewardDrawPoolItems,
            int cardIndex,
            int drawValue
    ) {
        if (drawValue < rewardDrawPool.getMissWeight()) {
            return RewardGachaDrawCard.create(
                    rewardGachaDraw,
                    cardIndex,
                    RewardGachaCardType.MISS,
                    rewardDrawPool.getMissRewardItem()
            );
        }

        int cursor = rewardDrawPool.getMissWeight();
        for (RewardDrawPoolItem rewardDrawPoolItem : rewardDrawPoolItems) {
            cursor += rewardDrawPoolItem.getWeight();
            if (drawValue < cursor) {
                return RewardGachaDrawCard.create(
                        rewardGachaDraw,
                        cardIndex,
                        RewardGachaCardType.REWARD,
                        rewardDrawPoolItem.getRewardItem()
                );
            }
        }

        throw new CustomException(ResponseCode.REWARD_DRAW_POOL_NOT_FOUND);
    }

    private RewardGachaDrawCardResponse toResponse(RewardGachaDrawCard rewardGachaDrawCard) {
        RewardItem rewardItem = rewardGachaDrawCard.getRewardItem();
        return new RewardGachaDrawCardResponse(
                rewardGachaDrawCard.getId(),
                rewardGachaDrawCard.getCardIndex(),
                rewardGachaDrawCard.getCardType(),
                rewardItem == null ? null : rewardItem.getId(),
                rewardItem == null ? null : rewardItem.getTitle(),
                rewardItem == null ? null : cloudFrontSignedUrlService.generateSignedUrl(rewardItem.getImageUrl())
        );
    }
}
