package kr.withrun.was.domain.running.repository.query;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.withrun.was.domain.running.entity.QGhostRunningResult;
import kr.withrun.was.domain.running.entity.QRunningSession;
import kr.withrun.was.domain.running.entity.RunningSession;
import kr.withrun.was.domain.running.repository.query.dto.PastRunningSessionHistoryRow;
import kr.withrun.was.domain.running.type.RunningSessionCompleteState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RunningSessionCustomRepositoryImpl implements RunningSessionCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<RunningSession> findByIdAndDeletedAtIsNull(Long runningSessionId) {
        QRunningSession runningSession = QRunningSession.runningSession;

        return Optional.ofNullable(
                queryFactory
                        .selectFrom(runningSession)
                        .where(
                                runningSession.id.eq(runningSessionId),
                                runningSession.deletedAt.isNull()
                        )
                        .fetchOne()
        );
    }

    @Override
    public List<PastRunningSessionHistoryRow> findPastRunningSessionHistoryRows(
            Long userId,
            LocalDateTime rangeStartInclusive,
            LocalDateTime rangeEndExclusive,
            LocalDateTime cursorStartedAt,
            Long cursorRunningSessionId,
            int pageSize
    ) {
        QRunningSession runningSession = QRunningSession.runningSession;
        QGhostRunningResult ghostRunningResult = QGhostRunningResult.ghostRunningResult;

        List<Long> pagedRunningSessionIds = queryFactory
                .select(runningSession.id)
                .from(runningSession)
                .where(
                        runningSession.user.id.eq(userId),
                        runningSession.deletedAt.isNull(),
                        runningSession.completeState.eq(RunningSessionCompleteState.SUCCESS),
                        withinRange(runningSession, rangeStartInclusive, rangeEndExclusive),
                        isAfterCursor(runningSession, cursorStartedAt, cursorRunningSessionId)
                )
                .orderBy(
                        runningSession.startedAt.desc(),
                        runningSession.id.desc()
                )
                .limit((long) pageSize + 1)
                .fetch();

        if (pagedRunningSessionIds.isEmpty()) {
            return List.of();
        }

        return queryFactory
                .select(Projections.constructor(
                        PastRunningSessionHistoryRow.class,
                        runningSession.id,
                        runningSession.startedAt,
                        runningSession.distanceM,
                        runningSession.durationSec,
                        runningSession.caloriesKcal,
                        runningSession.snapshotImageUrl,
                        runningSession.elevationGainM,
                        runningSession.completeState,
                        runningSession.mode,
                        ghostRunningResult.resultStatus
                ))
                .from(runningSession)
                .leftJoin(ghostRunningResult).on(
                        ghostRunningResult.runningSession.eq(runningSession),
                        ghostRunningResult.deletedAt.isNull()
                )
                .where(
                        runningSession.id.in(pagedRunningSessionIds)
                )
                .orderBy(
                        runningSession.startedAt.desc(),
                        runningSession.id.desc()
                )
                .fetch();
    }

    private BooleanExpression withinRange(
            QRunningSession runningSession,
            LocalDateTime rangeStartInclusive,
            LocalDateTime rangeEndExclusive
    ) {
        if (rangeStartInclusive == null || rangeEndExclusive == null) {
            return null;
        }

        return runningSession.startedAt.goe(rangeStartInclusive)
                .and(runningSession.startedAt.lt(rangeEndExclusive));
    }

    private BooleanExpression isAfterCursor(
            QRunningSession runningSession,
            LocalDateTime cursorStartedAt,
            Long cursorRunningSessionId
    ) {
        if (cursorStartedAt == null || cursorRunningSessionId == null) {
            return null;
        }

        return runningSession.startedAt.lt(cursorStartedAt)
                .or(runningSession.startedAt.eq(cursorStartedAt)
                        .and(runningSession.id.lt(cursorRunningSessionId)));
    }

}
