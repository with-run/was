package kr.withrun.was.domain.reward.service;

import kr.withrun.was.domain.reward.dto.RewardPointBalanceResponse;
import kr.withrun.was.domain.reward.dto.RewardPointHistoryItemResponse;
import kr.withrun.was.domain.reward.dto.RewardPointHistoryPageResponse;
import kr.withrun.was.domain.reward.entity.RewardPointBalance;
import kr.withrun.was.domain.reward.entity.RewardPointHistory;
import kr.withrun.was.domain.reward.repository.RewardPointBalanceRepository;
import kr.withrun.was.domain.reward.repository.RewardPointHistoryRepository;
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
public class RewardPointQueryService {

    private final UserRepository userRepository;
    private final RewardPointBalanceRepository rewardPointBalanceRepository;
    private final RewardPointHistoryRepository rewardPointHistoryRepository;

    public RewardPointQueryService(
            UserRepository userRepository,
            RewardPointBalanceRepository rewardPointBalanceRepository,
            RewardPointHistoryRepository rewardPointHistoryRepository
    ) {
        this.userRepository = userRepository;
        this.rewardPointBalanceRepository = rewardPointBalanceRepository;
        this.rewardPointHistoryRepository = rewardPointHistoryRepository;
    }

    public RewardPointBalanceResponse getRewardPointBalance(Long userId) {
        ensureUserExists(userId);

        int currentBalance = rewardPointBalanceRepository.findByIdAndDeletedAtIsNull(userId)
                .map(RewardPointBalance::getCurrentBalance)
                .orElse(0);

        return new RewardPointBalanceResponse(currentBalance);
    }

    public RewardPointHistoryPageResponse getRewardPointHistories(Long userId, int page, int size) {
        ensureUserExists(userId);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Page<RewardPointHistory> rewardPointHistories = rewardPointHistoryRepository.findAllByUserIdAndDeletedAtIsNull(userId, pageable);

        return new RewardPointHistoryPageResponse(
                rewardPointHistories.getContent().stream().map(RewardPointHistoryItemResponse::from).toList(),
                rewardPointHistories.getNumber(),
                rewardPointHistories.getSize(),
                rewardPointHistories.getTotalElements(),
                rewardPointHistories.getTotalPages(),
                rewardPointHistories.hasNext()
        );
    }

    private void ensureUserExists(Long userId) {
        userRepository.findNotDeletedUser(userId)
                .orElseThrow(() -> new CustomException(ResponseCode.USER_NOT_FOUND));
    }
}
