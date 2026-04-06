package kr.withrun.was.domain.running.repository.query;

import kr.withrun.was.domain.running.repository.query.dto.GhostRunningResultRow;

import java.util.Optional;

public interface GhostRunningResultCustomRepository {

    Optional<GhostRunningResultRow> findByRunningSessionId(Long runningSessionId);

}
