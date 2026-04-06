package kr.withrun.was.domain.reward.service;

import kr.withrun.was.domain.file.service.CloudFrontSignedUrlService;
import kr.withrun.was.domain.reward.dto.CreateRewardDrawPoolRequest;
import kr.withrun.was.domain.reward.dto.RewardDrawPoolItemRequest;
import kr.withrun.was.domain.reward.dto.RewardDrawPoolListResponse;
import kr.withrun.was.domain.reward.dto.RewardDrawPoolResponse;
import kr.withrun.was.domain.reward.dto.UpdateRewardDrawPoolRequest;
import kr.withrun.was.domain.reward.entity.RewardDrawPool;
import kr.withrun.was.domain.reward.entity.RewardDrawPoolItem;
import kr.withrun.was.domain.reward.entity.RewardItem;
import kr.withrun.was.domain.reward.repository.RewardDrawPoolItemRepository;
import kr.withrun.was.domain.reward.repository.RewardDrawPoolRepository;
import kr.withrun.was.domain.reward.repository.RewardItemRepository;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("리워드 draw pool 관리자 서비스")
class RewardDrawPoolAdminServiceTest {

    @Mock
    private RewardDrawPoolRepository rewardDrawPoolRepository;

    @Mock
    private RewardDrawPoolItemRepository rewardDrawPoolItemRepository;

    @Mock
    private RewardItemRepository rewardItemRepository;

    @Mock
    private CloudFrontSignedUrlService cloudFrontSignedUrlService;

    private RewardDrawPoolAdminService rewardDrawPoolAdminService;

    @BeforeEach
    void setUp() {
        rewardDrawPoolAdminService = new RewardDrawPoolAdminService(
                rewardDrawPoolRepository,
                rewardDrawPoolItemRepository,
                rewardItemRepository,
                cloudFrontSignedUrlService
        );

        lenient().when(cloudFrontSignedUrlService.generateSignedUrl(any()))
                .thenAnswer(invocation -> "signed::" + invocation.getArgument(0));
    }

    @DisplayName("pool 생성 시 item weight 매핑이 함께 저장된다")
    @Test
    void createsRewardDrawPoolWithItems() {
        RewardItem missRewardItem = rewardItem(9L, "Miss Reward", "reward-item/miss.png", true);
        RewardItem firstRewardItem = rewardItem(1L, "First Reward", "reward-item/1.png", true);
        RewardItem secondRewardItem = rewardItem(2L, "Second Reward", "reward-item/2.png", true);
        when(rewardItemRepository.findByIdAndDeletedAtIsNull(9L)).thenReturn(Optional.of(missRewardItem));
        when(rewardItemRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(firstRewardItem));
        when(rewardItemRepository.findByIdAndDeletedAtIsNull(2L)).thenReturn(Optional.of(secondRewardItem));
        when(rewardDrawPoolRepository.save(any(RewardDrawPool.class))).thenAnswer(invocation -> {
            RewardDrawPool rewardDrawPool = invocation.getArgument(0);
            setField(rewardDrawPool, "id", 10L);
            setField(rewardDrawPool, "createdAt", LocalDateTime.of(2026, 3, 29, 20, 0));
            setField(rewardDrawPool, "updatedAt", LocalDateTime.of(2026, 3, 29, 20, 5));
            return rewardDrawPool;
        });
        when(rewardDrawPoolItemRepository.saveAll(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<RewardDrawPoolItem> items = invocation.getArgument(0);
            setField(items.get(0), "id", 100L);
            setField(items.get(1), "id", 101L);
            return items;
        });

        RewardDrawPoolResponse response = rewardDrawPoolAdminService.createRewardDrawPool(
                new CreateRewardDrawPoolRequest(
                        "Launch Pool",
                        3,
                        40,
                        9L,
                        List.of(
                                new RewardDrawPoolItemRequest(1L, 70),
                                new RewardDrawPoolItemRequest(2L, 30)
                        )
                )
        );

        assertThat(response.rewardDrawPoolId()).isEqualTo(10L);
        assertThat(response.name()).isEqualTo("Launch Pool");
        assertThat(response.isActive()).isFalse();
        assertThat(response.cardsPerDraw()).isEqualTo(3);
        assertThat(response.missWeight()).isEqualTo(40);
        assertThat(readMissRewardItemId(response)).isEqualTo(9L);
        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).imageUrl()).isEqualTo("signed::reward-item/1.png");
        assertThat(response.items().get(0).weight()).isEqualTo(70);
    }

    @DisplayName("item 목록이 비어 있으면 pool 생성 시 예외가 발생한다")
    @Test
    void throwsWhenCreateRequestHasNoItems() {
        assertThatThrownBy(() -> rewardDrawPoolAdminService.createRewardDrawPool(
                new CreateRewardDrawPoolRequest("Launch Pool", 3, 40, null, List.of())
        )).isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.INVALID_INPUT_VALUE);

        verify(rewardDrawPoolRepository, never()).save(any());
    }

    @DisplayName("동일 rewardItemId 가 중복되면 pool 생성 시 예외가 발생한다")
    @Test
    void throwsWhenCreateRequestContainsDuplicateRewardItemIds() {
        assertThatThrownBy(() -> rewardDrawPoolAdminService.createRewardDrawPool(
                new CreateRewardDrawPoolRequest(
                        "Launch Pool",
                        3,
                        40,
                        null,
                        List.of(
                                new RewardDrawPoolItemRequest(1L, 50),
                                new RewardDrawPoolItemRequest(1L, 50)
                        )
                )
        )).isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.INVALID_INPUT_VALUE);
    }

    @DisplayName("weight 가 0 이하이면 pool 생성 시 예외가 발생한다")
    @Test
    void throwsWhenCreateRequestContainsNonPositiveWeight() {
        assertThatThrownBy(() -> rewardDrawPoolAdminService.createRewardDrawPool(
                new CreateRewardDrawPoolRequest(
                        "Launch Pool",
                        3,
                        40,
                        null,
                        List.of(new RewardDrawPoolItemRequest(1L, 0))
                )
        )).isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.INVALID_INPUT_VALUE);
    }

    @DisplayName("cardsPerDraw 가 0 이하이면 pool 생성 시 예외가 발생한다")
    @Test
    void throwsWhenCreateRequestContainsNonPositiveCardsPerDraw() {
        assertThatThrownBy(() -> rewardDrawPoolAdminService.createRewardDrawPool(
                new CreateRewardDrawPoolRequest(
                        "Launch Pool",
                        0,
                        40,
                        null,
                        List.of(new RewardDrawPoolItemRequest(1L, 100))
                )
        )).isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.INVALID_INPUT_VALUE);
    }

    @DisplayName("missWeight 가 음수이면 pool 생성 시 예외가 발생한다")
    @Test
    void throwsWhenCreateRequestContainsNegativeMissWeight() {
        assertThatThrownBy(() -> rewardDrawPoolAdminService.createRewardDrawPool(
                new CreateRewardDrawPoolRequest(
                        "Launch Pool",
                        3,
                        -1,
                        null,
                        List.of(new RewardDrawPoolItemRequest(1L, 100))
                )
        )).isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.INVALID_INPUT_VALUE);
    }

    @DisplayName("단건 조회 시 pool item imageUrl 이 client-facing URL로 정규화된다")
    @Test
    void returnsRewardDrawPoolDetailWithNormalizedImageUrls() {
        RewardDrawPool rewardDrawPool = rewardDrawPool(20L, "Active Pool", true);
        RewardItem rewardItem = rewardItem(3L, "Reward", "reward-item/3.png", true);
        RewardDrawPoolItem rewardDrawPoolItem = rewardDrawPoolItem(200L, rewardDrawPool, rewardItem, 100);
        when(rewardDrawPoolRepository.findByIdAndDeletedAtIsNull(20L)).thenReturn(Optional.of(rewardDrawPool));
        when(rewardDrawPoolItemRepository.findAllByRewardDrawPoolIdAndDeletedAtIsNullOrderByIdAsc(20L))
                .thenReturn(List.of(rewardDrawPoolItem));

        RewardDrawPoolResponse response = rewardDrawPoolAdminService.getRewardDrawPool(20L);

        assertThat(response.rewardDrawPoolId()).isEqualTo(20L);
        assertThat(response.cardsPerDraw()).isEqualTo(3);
        assertThat(response.missWeight()).isEqualTo(40);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).imageUrl()).isEqualTo("signed::reward-item/3.png");
        assertThat(response.items().get(0).weight()).isEqualTo(100);
    }

    @DisplayName("활성화 시 기존 active pool 은 비활성화된다")
    @Test
    void activatesTargetPoolAndDeactivatesOthers() {
        RewardDrawPool rewardDrawPool = rewardDrawPool(21L, "Next Pool", false);
        when(rewardDrawPoolRepository.findByIdAndDeletedAtIsNull(21L)).thenReturn(Optional.of(rewardDrawPool));
        when(rewardDrawPoolItemRepository.findAllByRewardDrawPoolIdAndDeletedAtIsNullOrderByIdAsc(21L))
                .thenReturn(List.of());

        RewardDrawPoolResponse response = rewardDrawPoolAdminService.activateRewardDrawPool(21L);

        verify(rewardDrawPoolRepository).deactivateAllActivePools();
        assertThat(response.isActive()).isTrue();
        assertThat(rewardDrawPool.isActive()).isTrue();
    }

    @DisplayName("목록 조회는 createdAt 내림차순 페이지 응답을 반환한다")
    @Test
    void returnsPagedRewardDrawPools() {
        RewardDrawPool rewardDrawPool = rewardDrawPool(22L, "Pool A", true);
        PageRequest pageRequest = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(rewardDrawPoolRepository.findAllByDeletedAtIsNull(pageRequest))
                .thenReturn(new PageImpl<>(List.of(rewardDrawPool), pageRequest, 1));
        when(rewardDrawPoolItemRepository.findAllByRewardDrawPoolIdAndDeletedAtIsNullOrderByIdAsc(22L))
                .thenReturn(List.of());

        RewardDrawPoolListResponse response = rewardDrawPoolAdminService.getRewardDrawPools(0, 20);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).rewardDrawPoolId()).isEqualTo(22L);
        assertThat(response.items().get(0).name()).isEqualTo("Pool A");
    }

    @DisplayName("pool 수정 시 이름과 item 구성이 교체된다")
    @Test
    void updatesRewardDrawPoolMetadataAndItems() {
        RewardDrawPool rewardDrawPool = rewardDrawPool(23L, "Old Pool", false);
        RewardItem rewardItem = rewardItem(5L, "Updated Reward", "reward-item/5.png", true);
        RewardDrawPoolItem oldItem = rewardDrawPoolItem(300L, rewardDrawPool, rewardItem, 10);
        when(rewardDrawPoolRepository.findByIdAndDeletedAtIsNull(23L)).thenReturn(Optional.of(rewardDrawPool));
        when(rewardDrawPoolItemRepository.findAllByRewardDrawPoolIdAndDeletedAtIsNullOrderByIdAsc(23L))
                .thenReturn(List.of(oldItem));
        when(rewardItemRepository.findByIdAndDeletedAtIsNull(5L)).thenReturn(Optional.of(rewardItem));
        when(rewardDrawPoolItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RewardDrawPoolResponse response = rewardDrawPoolAdminService.updateRewardDrawPool(
                23L,
                new UpdateRewardDrawPoolRequest(
                        "Updated Pool",
                        5,
                        25,
                        5L,
                        List.of(new RewardDrawPoolItemRequest(5L, 100))
                )
        );

        assertThat(response.name()).isEqualTo("Updated Pool");
        assertThat(response.cardsPerDraw()).isEqualTo(5);
        assertThat(response.missWeight()).isEqualTo(25);
        assertThat(readMissRewardItemId(response)).isEqualTo(5L);
        assertThat(oldItem.isDeleted()).isTrue();
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).weight()).isEqualTo(100);
    }

    private Long readMissRewardItemId(RewardDrawPoolResponse response) {
        try {
            return (Long) response.getClass().getMethod("missRewardItemId").invoke(response);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("RewardDrawPoolResponse must expose missRewardItemId", exception);
        }
    }

    private RewardDrawPool rewardDrawPool(Long id, String name, boolean isActive) {
        RewardDrawPool rewardDrawPool = RewardDrawPool.create(name, 3, 40);
        if (isActive) {
            rewardDrawPool.activate();
        }
        setField(rewardDrawPool, "id", id);
        setField(rewardDrawPool, "createdAt", LocalDateTime.of(2026, 3, 29, 12, 0));
        setField(rewardDrawPool, "updatedAt", LocalDateTime.of(2026, 3, 29, 12, 30));
        return rewardDrawPool;
    }

    private RewardItem rewardItem(Long id, String title, String imageUrl, boolean isActive) {
        RewardItem rewardItem = instantiate(RewardItem.class);
        setField(rewardItem, "title", title);
        setField(rewardItem, "imageUrl", imageUrl);
        setField(rewardItem, "active", isActive);
        setField(rewardItem, "id", id);
        return rewardItem;
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
