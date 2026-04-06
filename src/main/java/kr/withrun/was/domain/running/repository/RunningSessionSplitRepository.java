package kr.withrun.was.domain.running.repository;

import kr.withrun.was.domain.running.entity.RunningSessionSplit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RunningSessionSplitRepository extends JpaRepository<RunningSessionSplit, Long> {
    List<RunningSessionSplit> findByRunningSessionIdOrderBySplitIndexAsc(Long runningSessionId);
}
