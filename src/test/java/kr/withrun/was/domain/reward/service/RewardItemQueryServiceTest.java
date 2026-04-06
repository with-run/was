package kr.withrun.was.domain.reward.service;

import kr.withrun.was.domain.file.service.CloudFrontSignedUrlService;
import kr.withrun.was.domain.reward.dto.RewardItemShowcaseListResponse;
import kr.withrun.was.domain.reward.entity.RewardItem;
import kr.withrun.was.domain.reward.repository.RewardItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("리워드 아이템 공개 조회 서비스")
class RewardItemQueryServiceTest {

    @Mock
    private RewardItemRepository rewardItemRepository;

    @Mock
    private CloudFrontSignedUrlService cloudFrontSignedUrlService;

    private RewardItemQueryService rewardItemQueryService;

    @BeforeEach
    void setUp() {
        rewardItemQueryService = new RewardItemQueryService(rewardItemRepository, cloudFrontSignedUrlService);
        lenient().when(cloudFrontSignedUrlService.generateSignedUrl(any()))
                .thenAnswer(invocation -> "signed::" + invocation.getArgument(0));
    }

    @DisplayName("showcase 목록 조회 시 이미지 URL이 client-facing URL로 정규화된다")
    @Test
    void returnsShowcaseItemsWithNormalizedImageUrls() {
        RewardItem newest = rewardItem(2L, "Newest Reward", "reward-item/2.png", true);
        RewardItem older = rewardItem(1L, "Older Reward", "reward-item/1.png", true);
        when(rewardItemRepository.findAllByDeletedAtIsNullAndActiveTrueOrderByCreatedAtDescIdDesc())
                .thenReturn(List.of(newest, older));

        RewardItemShowcaseListResponse response = rewardItemQueryService.getRewardItemShowcase();

        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).rewardItemId()).isEqualTo(2L);
        assertThat(response.items().get(0).title()).isEqualTo("Newest Reward");
        assertThat(response.items().get(0).imageUrl()).isEqualTo("signed::reward-item/2.png");
        assertThat(response.items()).extracting(item -> item.title())
                .containsExactly("Newest Reward", "Older Reward");
        assertThat(response.items().get(0).getClass().getRecordComponents())
                .extracting(java.lang.reflect.RecordComponent::getName)
                .containsExactly("rewardItemId", "title", "imageUrl");
        verify(cloudFrontSignedUrlService).generateSignedUrl("reward-item/2.png");
        verify(cloudFrontSignedUrlService).generateSignedUrl("reward-item/1.png");
    }

    private RewardItem rewardItem(Long id, String title, String imageUrl, boolean isActive) {
        RewardItem rewardItem = instantiate(RewardItem.class);
        setField(rewardItem, "title", title);
        setField(rewardItem, "imageUrl", imageUrl);
        setField(rewardItem, "active", isActive);
        setField(rewardItem, "id", id);
        setField(rewardItem, "createdAt", LocalDateTime.of(2026, 3, 29, 12, 0));
        return rewardItem;
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
