package kr.withrun.was.domain.course.repository.query;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.withrun.was.domain.course.entity.CourseGhostLeaderboard;
import kr.withrun.was.domain.course.entity.QCourseGhostLeaderboard;
import kr.withrun.was.domain.course.repository.query.dto.CourseGhostLeaderboardRankRow;
import kr.withrun.was.domain.user.entity.QUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CourseGhostLeaderboardCustomRepositoryImpl implements CourseGhostLeaderboardCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<CourseGhostLeaderboardRankRow> findRankedRowsByCourseId(Long courseId) {
        QCourseGhostLeaderboard courseGhostLeaderboard = QCourseGhostLeaderboard.courseGhostLeaderboard;
        QUser user = QUser.user;
        NumberExpression<Long> rankExpression = Expressions.numberTemplate(
                Long.class,
                "rank() over(order by {0} desc)",
                courseGhostLeaderboard.point
        );

        return queryFactory
                .select(Projections.constructor(
                        CourseGhostLeaderboardRankRow.class,
                        courseGhostLeaderboard.id,
                        courseGhostLeaderboard.user.id,
                        user.nickname,
                        courseGhostLeaderboard.runningSession.id,
                        courseGhostLeaderboard.point,
                        rankExpression,
                        courseGhostLeaderboard.createdAt
                ))
                .from(courseGhostLeaderboard)
                .join(courseGhostLeaderboard.user, user)
                .where(courseGhostLeaderboard.course.id.eq(courseId))
                .orderBy(
                        courseGhostLeaderboard.point.desc(),
                        courseGhostLeaderboard.id.asc()
                )
                .fetch();
    }

    @Override
    public Optional<CourseGhostLeaderboard> findTopByUserIdAndCourseId(Long userId, Long courseId) {
        QCourseGhostLeaderboard courseGhostLeaderboard = QCourseGhostLeaderboard.courseGhostLeaderboard;

        CourseGhostLeaderboard row = queryFactory
                .selectFrom(courseGhostLeaderboard)
                .where(
                        courseGhostLeaderboard.user.id.eq(userId),
                        courseGhostLeaderboard.course.id.eq(courseId)
                )
                .orderBy(
                        courseGhostLeaderboard.point.desc(),
                        courseGhostLeaderboard.id.asc()
                )
                .limit(1)
                .fetchOne();

        return Optional.ofNullable(row);
    }
}
