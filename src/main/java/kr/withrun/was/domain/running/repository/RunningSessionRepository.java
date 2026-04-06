package kr.withrun.was.domain.running.repository;

import kr.withrun.was.domain.running.entity.RunningSession;
import kr.withrun.was.domain.running.repository.query.RunningSessionCustomRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RunningSessionRepository extends JpaRepository<RunningSession, Long>, RunningSessionCustomRepository {
}
