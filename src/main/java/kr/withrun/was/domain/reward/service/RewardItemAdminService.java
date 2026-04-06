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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class RewardItemAdminService {

    private static final int MAX_TITLE_LENGTH = 100;
    private static final String PENDING_IMAGE_KEY = "pending";

    private final RewardItemRepository rewardItemRepository;
    private final CloudFrontSignedUrlService cloudFrontSignedUrlService;
    private final S3FileService s3FileService;

    @Transactional(readOnly = true)
    public RewardItemResponse getRewardItem(Long rewardItemId) {
        return toResponse(findRewardItem(rewardItemId));
    }

    @Transactional(readOnly = true)
    public RewardItemListResponse getRewardItems(Boolean active, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<RewardItem> rewardItems = active == null
                ? rewardItemRepository.findAllByDeletedAtIsNull(pageable)
                : rewardItemRepository.findAllByDeletedAtIsNullAndActive(active, pageable);

        return new RewardItemListResponse(
                rewardItems.getContent().stream().map(this::toResponse).toList(),
                rewardItems.getNumber(),
                rewardItems.getSize(),
                rewardItems.getTotalElements(),
                rewardItems.getTotalPages(),
                rewardItems.hasNext()
        );
    }

    @Transactional
    public RewardItemResponse updateRewardItem(Long rewardItemId, UpdateRewardItemRequest request) {
        if (request == null || request.title() == null && request.isActive() == null) {
            throw new CustomException(ResponseCode.INVALID_INPUT_VALUE);
        }

        RewardItem rewardItem = findRewardItem(rewardItemId);

        if (request.title() != null) {
            rewardItem.updateTitle(validateAndTrimTitle(request.title()));
        }
        if (request.isActive() != null) {
            if (request.isActive()) {
                rewardItem.activate();
            } else {
                rewardItem.deactivate();
            }
        }

        return toResponse(rewardItem);
    }

    @Transactional
    public RewardItemResponse createRewardItem(String title, MultipartFile file) {
        String normalizedTitle = validateAndTrimTitle(title);
        validateFile(file);

        RewardItem rewardItem = rewardItemRepository.save(
                RewardItem.builder()
                        .title(normalizedTitle)
                        .imageUrl(PENDING_IMAGE_KEY)
                        .build()
        );

        rewardItem.updateImageUrl(uploadRewardItemImage(file, rewardItem.getId()));
        return toResponse(rewardItem);
    }

    @Transactional
    public RewardItemResponse updateRewardItemImage(Long rewardItemId, MultipartFile file) {
        RewardItem rewardItem = findRewardItem(rewardItemId);
        validateFile(file);

        rewardItem.updateImageUrl(uploadRewardItemImage(file, rewardItem.getId()));
        return toResponse(rewardItem);
    }

    private RewardItem findRewardItem(Long rewardItemId) {
        return rewardItemRepository.findByIdAndDeletedAtIsNull(rewardItemId)
                .orElseThrow(() -> new CustomException(ResponseCode.REWARD_ITEM_NOT_FOUND));
    }

    private RewardItemResponse toResponse(RewardItem rewardItem) {
        return new RewardItemResponse(
                rewardItem.getId(),
                rewardItem.getTitle(),
                cloudFrontSignedUrlService.generateSignedUrl(rewardItem.getImageUrl()),
                rewardItem.isActive(),
                rewardItem.getCreatedAt(),
                rewardItem.getUpdatedAt()
        );
    }

    private String validateAndTrimTitle(String title) {
        if (!StringUtils.hasText(title)) {
            throw new CustomException(ResponseCode.INVALID_INPUT_VALUE);
        }

        String normalizedTitle = title.trim();
        if (normalizedTitle.length() > MAX_TITLE_LENGTH) {
            throw new CustomException(ResponseCode.INVALID_INPUT_VALUE);
        }

        return normalizedTitle;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ResponseCode.EMPTY_FILE);
        }
    }

    private String uploadRewardItemImage(MultipartFile file, Long rewardItemId) {
        try {
            return s3FileService.uploadRewardItemImage(file.getBytes(), rewardItemId);
        } catch (IOException exception) {
            throw new CustomException(ResponseCode.S3_UPLOAD_FAILED);
        }
    }
}
