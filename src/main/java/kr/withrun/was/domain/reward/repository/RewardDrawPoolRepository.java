package kr.withrun.was.domain.reward.repository;

import kr.withrun.was.domain.reward.entity.RewardDrawPool;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface RewardDrawPoolRepository extends JpaRepository<RewardDrawPool, Long> {

    Page<RewardDrawPool> findAllByDeletedAtIsNull(Pageable pageable);

    Optional<RewardDrawPool> findByIdAndDeletedAtIsNull(Long rewardDrawPoolId);

    Optional<RewardDrawPool> findByActiveTrueAndDeletedAtIsNull();

    @EntityGraph(attributePaths = "missRewardItem")
    Optional<RewardDrawPool> findWithMissRewardItemByIdAndDeletedAtIsNull(Long rewardDrawPoolId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RewardDrawPool rewardDrawPool
            set rewardDrawPool.active = false
            where rewardDrawPool.active = true
              and rewardDrawPool.deletedAt is null
            """)
    int deactivateAllActivePools();
}
