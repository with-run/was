package kr.withrun.was.domain.reward.repository;

import kr.withrun.was.domain.reward.entity.RewardPointBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RewardPointBalanceRepository extends JpaRepository<RewardPointBalance, Long> {

    Optional<RewardPointBalance> findByIdAndDeletedAtIsNull(Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RewardPointBalance rewardPointBalance
            set rewardPointBalance.currentBalance = rewardPointBalance.currentBalance + :deltaPoint
            where rewardPointBalance.id = :userId
              and rewardPointBalance.deletedAt is null
            """)
    int increaseBalance(@Param("userId") Long userId, @Param("deltaPoint") int deltaPoint);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RewardPointBalance rewardPointBalance
            set rewardPointBalance.currentBalance = rewardPointBalance.currentBalance - :deltaPoint
            where rewardPointBalance.id = :userId
              and rewardPointBalance.deletedAt is null
              and rewardPointBalance.currentBalance >= :deltaPoint
            """)
    int decreaseBalanceIfEnough(@Param("userId") Long userId, @Param("deltaPoint") int deltaPoint);
}
