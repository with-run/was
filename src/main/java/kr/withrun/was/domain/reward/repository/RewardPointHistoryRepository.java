package kr.withrun.was.domain.reward.repository;

import kr.withrun.was.domain.reward.entity.RewardPointHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RewardPointHistoryRepository extends JpaRepository<RewardPointHistory, Long> {

    boolean existsByUserIdAndIdempotencyKeyAndDeletedAtIsNull(Long userId, String idempotencyKey);

    Optional<RewardPointHistory> findByUserIdAndIdempotencyKeyAndDeletedAtIsNull(Long userId, String idempotencyKey);

    Page<RewardPointHistory> findAllByUserIdAndDeletedAtIsNull(Long userId, Pageable pageable);
}
