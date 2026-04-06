package kr.withrun.was.domain.reward.service;

import kr.withrun.was.domain.file.service.CloudFrontSignedUrlService;
import kr.withrun.was.domain.reward.dto.RewardInventoryPageResponse;
import kr.withrun.was.domain.reward.entity.RewardGachaDraw;
import kr.withrun.was.domain.reward.entity.RewardGachaDrawCard;
import kr.withrun.was.domain.reward.entity.RewardItem;
import kr.withrun.was.domain.reward.repository.RewardGachaDrawCardRepository;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("리워드 인벤토리 조회 서비스")
class RewardGachaInventoryQueryServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RewardGachaDrawCardRepository rewardGachaDrawCardRepository;

    @Mock
    private CloudFrontSignedUrlService cloudFrontSignedUrlService;

    private RewardGachaInventoryQueryService rewardGachaInventoryQueryService;

    @BeforeEach
    void setUp() {
        rewardGachaInventoryQueryService = new RewardGachaInventoryQueryService(
                userRepository,
                rewardGachaDrawCardRepository,
                cloudFrontSignedUrlService
        );
        lenient().when(cloudFrontSignedUrlService.generateSignedUrl(any()))
                .thenAnswer(invocation -> "signed::" + invocation.getArgument(0));
    }

    @DisplayName("내 인벤토리를 최신 획득순 페이지 응답으로 반환한다")
    @Test
    void returnsPagedInventory() {
        User user = User.createPendingSocialUser("runner-inventory");
        setField(user, "id", 1L);
        RewardItem rewardItem = rewardItem(100L, "Reward A", "reward-item/1.png");
        RewardGachaDraw rewardGachaDraw = rewardGachaDraw(500L, user, LocalDateTime.of(2026, 3, 30, 10, 0));
        RewardGachaDrawCard rewardGachaDrawCard = rewardGachaDrawCard(700L, rewardGachaDraw, rewardItem, 1, LocalDateTime.of(2026, 3, 30, 10, 0));
        PageRequest pageRequest = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        when(userRepository.findNotDeletedUser(1L)).thenReturn(Optional.of(user));
        when(rewardGachaDrawCardRepository.findInventoryByUserId(1L, pageRequest))
                .thenReturn(new PageImpl<>(List.of(rewardGachaDrawCard), pageRequest, 1));

        RewardInventoryPageResponse response = rewardGachaInventoryQueryService.getRewardInventory(1L, 0, 20);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).rewardGachaDrawCardId()).isEqualTo(700L);
        assertThat(response.items().get(0).rewardGachaDrawId()).isEqualTo(500L);
        assertThat(response.items().get(0).rewardItemId()).isEqualTo(100L);
        assertThat(response.items().get(0).title()).isEqualTo("Reward A");
        assertThat(response.items().get(0).imageUrl()).isEqualTo("signed::reward-item/1.png");
        assertThat(response.hasNext()).isFalse();
    }

    private RewardItem rewardItem(Long id, String title, String imageUrl) {
        RewardItem rewardItem = instantiate(RewardItem.class);
        setField(rewardItem, "id", id);
        setField(rewardItem, "title", title);
        setField(rewardItem, "imageUrl", imageUrl);
        setField(rewardItem, "active", true);
        return rewardItem;
    }

    private RewardGachaDraw rewardGachaDraw(Long id, User user, LocalDateTime createdAt) {
        RewardGachaDraw rewardGachaDraw = instantiate(RewardGachaDraw.class);
        setField(rewardGachaDraw, "id", id);
        setField(rewardGachaDraw, "user", user);
        setField(rewardGachaDraw, "createdAt", createdAt);
        return rewardGachaDraw;
    }

    private RewardGachaDrawCard rewardGachaDrawCard(Long id, RewardGachaDraw rewardGachaDraw, RewardItem rewardItem, int cardIndex, LocalDateTime createdAt) {
        RewardGachaDrawCard rewardGachaDrawCard = RewardGachaDrawCard.create(
                rewardGachaDraw,
                cardIndex,
                kr.withrun.was.domain.reward.type.RewardGachaCardType.REWARD,
                rewardItem
        );
        setField(rewardGachaDrawCard, "id", id);
        setField(rewardGachaDrawCard, "createdAt", createdAt);
        return rewardGachaDrawCard;
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
