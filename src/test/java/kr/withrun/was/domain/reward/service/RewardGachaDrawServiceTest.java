package kr.withrun.was.domain.reward.service;

import kr.withrun.was.domain.file.service.CloudFrontSignedUrlService;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("리워드 가챠 draw 서비스")
class RewardGachaDrawServiceTest {

    @Mock
    private RewardDrawPoolRepository rewardDrawPoolRepository;

    @Mock
    private RewardDrawPoolItemRepository rewardDrawPoolItemRepository;

    @Mock
    private RewardPointBalanceRepository rewardPointBalanceRepository;

    @Mock
    private RewardPointHistoryRepository rewardPointHistoryRepository;

    @Mock
    private RewardGachaDrawRepository rewardGachaDrawRepository;

    @Mock
    private RewardGachaDrawCardRepository rewardGachaDrawCardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CloudFrontSignedUrlService cloudFrontSignedUrlService;

    @Mock
    private RewardDrawRandomGenerator rewardDrawRandomGenerator;

    private RewardGachaDrawService rewardGachaDrawService;

    @BeforeEach
    void setUp() {
        rewardGachaDrawService = new RewardGachaDrawService(
                rewardDrawPoolRepository,
                rewardDrawPoolItemRepository,
                rewardPointBalanceRepository,
                rewardPointHistoryRepository,
                rewardGachaDrawRepository,
                rewardGachaDrawCardRepository,
                userRepository,
                cloudFrontSignedUrlService,
                rewardDrawRandomGenerator
        );
    }

    @DisplayName("활성 pool 기준으로 한 번의 draw 결과 전체를 저장하고 1포인트를 차감한다")
    @Test
    void drawsCardsAndDeductsOnePoint() {
        User user = user(1L);
        RewardItem missRewardItem = rewardItem(200L, "Miss Reward", "reward-item/miss.png");
        RewardDrawPool rewardDrawPool = rewardDrawPool(10L, "Launch Pool", 3, 40, true, missRewardItem);
        RewardItem rewardItem = rewardItem(100L, "Reward A", "reward-item/1.png");
        RewardDrawPoolItem rewardDrawPoolItem = rewardDrawPoolItem(1000L, rewardDrawPool, rewardItem, 60);

        when(rewardDrawPoolRepository.findByActiveTrueAndDeletedAtIsNull()).thenReturn(Optional.of(rewardDrawPool));
        when(rewardDrawPoolRepository.findWithMissRewardItemByIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(rewardDrawPool));
        when(rewardDrawPoolItemRepository.findWithRewardItemByRewardDrawPoolIdAndDeletedAtIsNullOrderByIdAsc(10L))
                .thenReturn(List.of(rewardDrawPoolItem));
        when(rewardPointBalanceRepository.decreaseBalanceIfEnough(1L, 1)).thenReturn(1);
        when(rewardPointBalanceRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(rewardPointBalance(1L, 6)));
        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(cloudFrontSignedUrlService.generateSignedUrl("reward-item/1.png")).thenReturn("signed::reward-item/1.png");
        when(cloudFrontSignedUrlService.generateSignedUrl("reward-item/miss.png")).thenReturn("signed::reward-item/miss.png");
        when(rewardDrawRandomGenerator.nextInt(100)).thenReturn(40, 0, 40);
        when(rewardGachaDrawRepository.save(any(RewardGachaDraw.class))).thenAnswer(invocation -> {
            RewardGachaDraw rewardGachaDraw = invocation.getArgument(0);
            setField(rewardGachaDraw, "id", 500L);
            setField(rewardGachaDraw, "createdAt", LocalDateTime.of(2026, 3, 29, 21, 0));
            setField(rewardGachaDraw, "updatedAt", LocalDateTime.of(2026, 3, 29, 21, 0));
            return rewardGachaDraw;
        });
        when(rewardGachaDrawCardRepository.saveAll(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<RewardGachaDrawCard> cards = invocation.getArgument(0);
            setField(cards.get(0), "id", 700L);
            setField(cards.get(1), "id", 701L);
            setField(cards.get(2), "id", 702L);
            return cards;
        });

        RewardGachaDrawResponse response = rewardGachaDrawService.draw(1L);

        ArgumentCaptor<RewardPointHistory> historyCaptor = ArgumentCaptor.forClass(RewardPointHistory.class);
        verify(rewardPointHistoryRepository).saveAndFlush(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getDeltaPoint()).isEqualTo(-1);
        assertThat(historyCaptor.getValue().getReason()).isEqualTo(RewardPointReason.GACHA_DRAW);
        assertThat(historyCaptor.getValue().getIdempotencyKey()).isEqualTo("reward:gacha-draw:500");

        assertThat(response.rewardGachaDrawId()).isEqualTo(500L);
        assertThat(response.spentPoint()).isEqualTo(1);
        assertThat(response.remainingBalance()).isEqualTo(6);
        assertThat(response.cards()).hasSize(3);
        assertThat(response.cards()).extracting(card -> card.cardType().name())
                .containsExactly("REWARD", "MISS", "REWARD");
        assertThat(response.cards().get(0).imageUrl()).isEqualTo("signed::reward-item/1.png");
        assertThat(response.cards().get(1).rewardItemId()).isEqualTo(200L);
        assertThat(response.cards().get(1).title()).isEqualTo("Miss Reward");
        assertThat(response.cards().get(1).imageUrl()).isEqualTo("signed::reward-item/miss.png");
        verify(rewardPointBalanceRepository).decreaseBalanceIfEnough(1L, 1);
    }

    @DisplayName("활성 pool 설정이 3장을 넘겨도 draw 는 최대 3장만 반환하고 저장한다")
    @Test
    void capsDrawToAtMostThreeCards() {
        User user = user(1L);
        RewardItem missRewardItem = rewardItem(200L, "Miss Reward", "reward-item/miss.png");
        RewardDrawPool rewardDrawPool = rewardDrawPool(10L, "Launch Pool", 5, 40, true, missRewardItem);
        RewardItem rewardItem = rewardItem(100L, "Reward A", "reward-item/1.png");
        RewardDrawPoolItem rewardDrawPoolItem = rewardDrawPoolItem(1000L, rewardDrawPool, rewardItem, 60);

        when(rewardDrawPoolRepository.findByActiveTrueAndDeletedAtIsNull()).thenReturn(Optional.of(rewardDrawPool));
        when(rewardDrawPoolRepository.findWithMissRewardItemByIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(rewardDrawPool));
        when(rewardDrawPoolItemRepository.findWithRewardItemByRewardDrawPoolIdAndDeletedAtIsNullOrderByIdAsc(10L))
                .thenReturn(List.of(rewardDrawPoolItem));
        when(rewardPointBalanceRepository.decreaseBalanceIfEnough(1L, 1)).thenReturn(1);
        when(rewardPointBalanceRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(rewardPointBalance(1L, 6)));
        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(cloudFrontSignedUrlService.generateSignedUrl("reward-item/1.png")).thenReturn("signed::reward-item/1.png");
        when(cloudFrontSignedUrlService.generateSignedUrl("reward-item/miss.png")).thenReturn("signed::reward-item/miss.png");
        when(rewardDrawRandomGenerator.nextInt(100)).thenReturn(40, 0, 40);
        when(rewardGachaDrawRepository.save(any(RewardGachaDraw.class))).thenAnswer(invocation -> {
            RewardGachaDraw rewardGachaDraw = invocation.getArgument(0);
            setField(rewardGachaDraw, "id", 500L);
            setField(rewardGachaDraw, "createdAt", LocalDateTime.of(2026, 3, 29, 21, 0));
            setField(rewardGachaDraw, "updatedAt", LocalDateTime.of(2026, 3, 29, 21, 0));
            return rewardGachaDraw;
        });
        when(rewardGachaDrawCardRepository.saveAll(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<RewardGachaDrawCard> cards = invocation.getArgument(0);
            setField(cards.get(0), "id", 700L);
            setField(cards.get(1), "id", 701L);
            setField(cards.get(2), "id", 702L);
            return cards;
        });

        RewardGachaDrawResponse response = rewardGachaDrawService.draw(1L);

        ArgumentCaptor<RewardGachaDraw> drawCaptor = ArgumentCaptor.forClass(RewardGachaDraw.class);
        verify(rewardGachaDrawRepository).save(drawCaptor.capture());
        assertThat(drawCaptor.getValue().getCardsPerDraw()).isEqualTo(3);

        ArgumentCaptor<List<RewardGachaDrawCard>> cardsCaptor = ArgumentCaptor.forClass(List.class);
        verify(rewardGachaDrawCardRepository).saveAll(cardsCaptor.capture());
        assertThat(cardsCaptor.getValue()).hasSize(3);
        assertThat(cardsCaptor.getValue())
                .extracting(RewardGachaDrawCard::getCardIndex)
                .containsExactly(1, 2, 3);

        assertThat(response.cards()).hasSize(3);
        assertThat(response.cards()).extracting(card -> card.cardType().name())
                .containsExactly("REWARD", "MISS", "REWARD");
    }

    @DisplayName("현재 포인트가 부족하면 draw 를 실행하지 않는다")
    @Test
    void throwsWhenRewardPointIsInsufficient() {
        RewardDrawPool rewardDrawPool = rewardDrawPool(10L, "Launch Pool", 3, 40, true, null);

        when(rewardDrawPoolRepository.findByActiveTrueAndDeletedAtIsNull()).thenReturn(Optional.of(rewardDrawPool));
        when(rewardPointBalanceRepository.decreaseBalanceIfEnough(1L, 1)).thenReturn(0);

        assertThatThrownBy(() -> rewardGachaDrawService.draw(1L))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.REWARD_POINT_INSUFFICIENT);

        verify(rewardPointHistoryRepository, never()).saveAndFlush(any());
        verify(rewardGachaDrawRepository, never()).save(any());
        verify(rewardGachaDrawCardRepository, never()).saveAll(any());
    }

    private User user(Long userId) {
        User user = User.createPendingSocialUser("runner-" + userId);
        setField(user, "id", userId);
        return user;
    }

    private RewardDrawPool rewardDrawPool(Long id, String name, int cardsPerDraw, int missWeight, boolean isActive, RewardItem missRewardItem) {
        RewardDrawPool rewardDrawPool = RewardDrawPool.create(name, cardsPerDraw, missWeight, missRewardItem);
        if (isActive) {
            rewardDrawPool.activate();
        }
        setField(rewardDrawPool, "id", id);
        return rewardDrawPool;
    }

    private RewardItem rewardItem(Long id, String title, String imageUrl) {
        RewardItem rewardItem = instantiate(RewardItem.class);
        setField(rewardItem, "title", title);
        setField(rewardItem, "imageUrl", imageUrl);
        setField(rewardItem, "active", true);
        setField(rewardItem, "id", id);
        return rewardItem;
    }

    private kr.withrun.was.domain.reward.entity.RewardPointBalance rewardPointBalance(Long userId, int currentBalance) {
        kr.withrun.was.domain.reward.entity.RewardPointBalance rewardPointBalance = instantiate(kr.withrun.was.domain.reward.entity.RewardPointBalance.class);
        setField(rewardPointBalance, "id", userId);
        setField(rewardPointBalance, "currentBalance", currentBalance);
        return rewardPointBalance;
    }

    private RewardDrawPoolItem rewardDrawPoolItem(Long id, RewardDrawPool rewardDrawPool, RewardItem rewardItem, int weight) {
        RewardDrawPoolItem rewardDrawPoolItem = RewardDrawPoolItem.create(rewardDrawPool, rewardItem, weight);
        setField(rewardDrawPoolItem, "id", id);
        return rewardDrawPoolItem;
    }

    private <T> T instantiate(Class<T> type) {
        try {
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to instantiate " + type.getSimpleName(), exception);
        }
    }

    private void setField(Object target, String fieldName, Object value) {
        Class<?> currentClass = target.getClass();
        while (currentClass != null) {
            try {
                Field field = currentClass.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException exception) {
                currentClass = currentClass.getSuperclass();
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException("Failed to set field " + fieldName, exception);
            }
        }

        throw new IllegalArgumentException("Field not found: " + fieldName);
    }
}
