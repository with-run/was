package kr.withrun.was.domain.running.repository.query;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.withrun.was.domain.running.entity.QGhostRunningResult;
import kr.withrun.was.domain.running.repository.query.dto.GhostRunningResultRow;
import kr.withrun.was.domain.user.entity.QUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class GhostRunningResultCustomRepositoryImpl implements GhostRunningResultCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<GhostRunningResultRow> findByRunningSessionId(Long runningSessionId) {
        QGhostRunningResult ghostRunningResult = QGhostRunningResult.ghostRunningResult;
        QUser targetUser = QUser.user;

        return Optional.ofNullable(
                queryFactory
                        .select(Projections.constructor(
                                GhostRunningResultRow.class,
                                ghostRunningResult.id,
                                ghostRunningResult.runningSession.id,
                                ghostRunningResult.ghostTargetRunningSession.id,
                                targetUser.id,
                                ghostRunningResult.resultStatus,
                                ghostRunningResult.point,
                                ghostRunningResult.timeGapSec,
                                ghostRunningResult.distanceGapM,
                                ghostRunningResult.createdAt
                        ))
                        .from(ghostRunningResult)
                        .leftJoin(ghostRunningResult.targetUser, targetUser)
                        .where(
                                ghostRunningResult.runningSession.id.eq(runningSessionId),
                                ghostRunningResult.deletedAt.isNull()
                        )
                        .fetchOne()
        );
    }

}
