package kr.withrun.was.domain.running.repository.query;

import kr.withrun.was.domain.running.entity.RunningSession;
import kr.withrun.was.domain.running.repository.query.dto.PastRunningSessionHistoryRow;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RunningSessionCustomRepository {

    Optional<RunningSession> findByIdAndDeletedAtIsNull(Long id);

    List<PastRunningSessionHistoryRow> findPastRunningSessionHistoryRows(
            Long userId,
            LocalDateTime rangeStartInclusive,
            LocalDateTime rangeEndExclusive,
            LocalDateTime cursorStartedAt,
            Long cursorRunningSessionId,
            int pageSize
    );

}
