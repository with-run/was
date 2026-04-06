package kr.withrun.was.domain.reward.repository;

import kr.withrun.was.domain.reward.entity.RewardItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RewardItemRepository extends JpaRepository<RewardItem, Long> {

    Page<RewardItem> findAllByDeletedAtIsNull(Pageable pageable);

    Page<RewardItem> findAllByDeletedAtIsNullAndActive(boolean active, Pageable pageable);

    List<RewardItem> findAllByDeletedAtIsNullAndActiveTrueOrderByCreatedAtDescIdDesc();

    Optional<RewardItem> findByIdAndDeletedAtIsNull(Long id);
}
