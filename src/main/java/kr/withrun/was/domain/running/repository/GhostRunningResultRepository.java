package kr.withrun.was.domain.running.repository;

import kr.withrun.was.domain.running.entity.GhostRunningResult;
import kr.withrun.was.domain.running.repository.query.GhostRunningResultCustomRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GhostRunningResultRepository extends JpaRepository<GhostRunningResult, Long>, GhostRunningResultCustomRepository {
    Optional<GhostRunningResult> findByRunningSessionIdAndDeletedAtIsNull(Long runningSessionId);
}
