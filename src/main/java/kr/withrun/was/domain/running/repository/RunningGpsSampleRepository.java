package kr.withrun.was.domain.running.repository;

import kr.withrun.was.domain.running.entity.RunningGpsSample;
import kr.withrun.was.domain.running.repository.query.RunningGpsSampleCustomRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RunningGpsSampleRepository extends JpaRepository<RunningGpsSample, Long>, RunningGpsSampleCustomRepository {
    List<RunningGpsSample> findByRunningSessionIdOrderBySampledAtAsc(Long runningSessionId);
}
