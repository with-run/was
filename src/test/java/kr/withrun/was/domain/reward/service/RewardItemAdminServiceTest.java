package kr.withrun.was.domain.reward.service;

import kr.withrun.was.domain.file.service.CloudFrontSignedUrlService;
import kr.withrun.was.domain.file.service.S3FileService;
import kr.withrun.was.domain.reward.dto.RewardItemListResponse;
import kr.withrun.was.domain.reward.dto.RewardItemResponse;
import kr.withrun.was.domain.reward.dto.UpdateRewardItemRequest;
import kr.withrun.was.domain.reward.entity.RewardItem;
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
import org.springframework.mock.web.MockMultipartFile;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("리워드 아이템 관리자 서비스")
class RewardItemAdminServiceTest {

    @Mock
    private RewardItemRepository rewardItemRepository;

    @Mock
    private CloudFrontSignedUrlService cloudFrontSignedUrlService;

    @Mock
    private S3FileService s3FileService;

    private RewardItemAdminService rewardItemAdminService;

    @BeforeEach
    void setUp() {
        rewardItemAdminService = new RewardItemAdminService(
                rewardItemRepository,
                cloudFrontSignedUrlService,
                s3FileService
        );

        lenient().when(cloudFrontSignedUrlService.generateSignedUrl(any()))
                .thenAnswer(invocation -> {
                    Object value = invocation.getArgument(0);
                    return value == null ? null : "signed::" + value;
                });
    }

    @DisplayName("단건 조회 시 signed/public URL로 imageUrl이 변환된다")
    @Test
    void returnsRewardItemDetailWithSignedImageUrl() {
        RewardItem rewardItem = rewardItem(1L, "Reward", "reward-item/1.png", true);
        when(rewardItemRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(rewardItem));

        RewardItemResponse response = rewardItemAdminService.getRewardItem(1L);

        assertThat(response.rewardItemId()).isEqualTo(1L);
        assertThat(response.imageUrl()).isEqualTo("signed::reward-item/1.png");
    }

    @DisplayName("목록 조회 시 active 필터가 반영된다")
    @Test
    void appliesActiveFilterToRewardItemList() {
        RewardItem rewardItem = rewardItem(2L, "Active Reward", "reward-item/2.png", true);
        PageRequest pageRequest = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(rewardItemRepository.findAllByDeletedAtIsNullAndActive(true, pageRequest))
                .thenReturn(new PageImpl<>(List.of(rewardItem), pageRequest, 1));

        RewardItemListResponse response = rewardItemAdminService.getRewardItems(true, 0, 20);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).title()).isEqualTo("Active Reward");
        assertThat(response.hasNext()).isFalse();
    }

    @DisplayName("PATCH에서 title 또는 isActive가 변경된다")
    @Test
    void updatesRewardItemMetadata() {
        RewardItem rewardItem = rewardItem(3L, "Old Reward", "reward-item/3.png", true);
        when(rewardItemRepository.findByIdAndDeletedAtIsNull(3L)).thenReturn(Optional.of(rewardItem));

        RewardItemResponse response = rewardItemAdminService.updateRewardItem(
                3L,
                new UpdateRewardItemRequest("Updated Reward", false)
        );

        assertThat(response.title()).isEqualTo("Updated Reward");
        assertThat(response.isActive()).isFalse();
        assertThat(rewardItem.getTitle()).isEqualTo("Updated Reward");
        assertThat(rewardItem.isActive()).isFalse();
    }

    @DisplayName("soft deleted 또는 없는 아이템 수정 시 예외가 발생한다")
    @Test
    void throwsWhenRewardItemToUpdateDoesNotExist() {
        when(rewardItemRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rewardItemAdminService.updateRewardItem(99L, new UpdateRewardItemRequest("Missing", true)))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.REWARD_ITEM_NOT_FOUND);
    }

    @DisplayName("title과 file로 아이템 생성 시 엔터티가 저장되고 업로드 key가 반영된다")
    @Test
    void createsRewardItemAndStoresUploadedImageKey() {
        byte[] fileBytes = "reward-image".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "reward.png", "image/png", fileBytes);
        when(rewardItemRepository.save(any(RewardItem.class))).thenAnswer(invocation -> {
            RewardItem rewardItem = invocation.getArgument(0);
            setField(rewardItem, "id", 10L);
            return rewardItem;
        });
        when(s3FileService.uploadRewardItemImage(fileBytes, 10L)).thenReturn("reward-item/10.png");

        RewardItemResponse response = rewardItemAdminService.createRewardItem("New Reward", file);

        assertThat(response.rewardItemId()).isEqualTo(10L);
        assertThat(response.title()).isEqualTo("New Reward");
        assertThat(response.imageUrl()).isEqualTo("signed::reward-item/10.png");
        verify(s3FileService).uploadRewardItemImage(fileBytes, 10L);
    }

    @DisplayName("이미지 교체 시 기존 아이템의 imageUrl이 갱신된다")
    @Test
    void replacesRewardItemImage() {
        RewardItem rewardItem = rewardItem(11L, "Reward", "reward-item/old.png", true);
        byte[] fileBytes = "replacement-image".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "reward.png", "image/png", fileBytes);
        when(rewardItemRepository.findByIdAndDeletedAtIsNull(11L)).thenReturn(Optional.of(rewardItem));
        when(s3FileService.uploadRewardItemImage(fileBytes, 11L)).thenReturn("reward-item/11.png");

        RewardItemResponse response = rewardItemAdminService.updateRewardItemImage(11L, file);

        assertThat(response.imageUrl()).isEqualTo("signed::reward-item/11.png");
        assertThat(rewardItem.getImageUrl()).isEqualTo("reward-item/11.png");
    }

    @DisplayName("파일이 비어 있으면 예외가 발생한다")
    @Test
    void throwsWhenCreateFileIsEmpty() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> rewardItemAdminService.createRewardItem("Reward", emptyFile))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.EMPTY_FILE);
    }

    private RewardItem rewardItem(Long id, String title, String imageUrl, boolean isActive) {
        RewardItem rewardItem = RewardItem.builder()
                .title(title)
                .imageUrl(imageUrl)
                .active(isActive)
                .build();
        setField(rewardItem, "id", id);
        setField(rewardItem, "createdAt", LocalDateTime.of(2026, 3, 29, 12, 0));
        setField(rewardItem, "updatedAt", LocalDateTime.of(2026, 3, 29, 12, 30));
        return rewardItem;
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
