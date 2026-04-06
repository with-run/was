package kr.withrun.was.domain.reward.service;

import kr.withrun.was.domain.file.service.CloudFrontSignedUrlService;
import kr.withrun.was.domain.reward.dto.RewardInventoryItemResponse;
import kr.withrun.was.domain.reward.dto.RewardInventoryPageResponse;
import kr.withrun.was.domain.reward.entity.RewardGachaDrawCard;
import kr.withrun.was.domain.reward.repository.RewardGachaDrawCardRepository;
import kr.withrun.was.domain.user.repository.UserRepository;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RewardGachaInventoryQueryService {

    private final UserRepository userRepository;
    private final RewardGachaDrawCardRepository rewardGachaDrawCardRepository;
    private final CloudFrontSignedUrlService cloudFrontSignedUrlService;

    public RewardGachaInventoryQueryService(
            UserRepository userRepository,
            RewardGachaDrawCardRepository rewardGachaDrawCardRepository,
            CloudFrontSignedUrlService cloudFrontSignedUrlService
    ) {
        this.userRepository = userRepository;
        this.rewardGachaDrawCardRepository = rewardGachaDrawCardRepository;
        this.cloudFrontSignedUrlService = cloudFrontSignedUrlService;
    }

    public RewardInventoryPageResponse getRewardInventory(Long userId, int page, int size) {
        ensureUserExists(userId);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Page<RewardGachaDrawCard> inventory = rewardGachaDrawCardRepository.findInventoryByUserId(userId, pageable);

        return new RewardInventoryPageResponse(
                inventory.getContent().stream().map(this::toResponse).toList(),
                inventory.getNumber(),
                inventory.getSize(),
                inventory.getTotalElements(),
                inventory.getTotalPages(),
                inventory.hasNext()
        );
    }

    private RewardInventoryItemResponse toResponse(RewardGachaDrawCard rewardGachaDrawCard) {
        return new RewardInventoryItemResponse(
                rewardGachaDrawCard.getId(),
                rewardGachaDrawCard.getRewardGachaDraw().getId(),
                rewardGachaDrawCard.getRewardItem().getId(),
                rewardGachaDrawCard.getRewardItem().getTitle(),
                cloudFrontSignedUrlService.generateSignedUrl(rewardGachaDrawCard.getRewardItem().getImageUrl()),
                rewardGachaDrawCard.getCreatedAt()
        );
    }

    private void ensureUserExists(Long userId) {
        userRepository.findNotDeletedUser(userId)
                .orElseThrow(() -> new CustomException(ResponseCode.USER_NOT_FOUND));
    }
}
