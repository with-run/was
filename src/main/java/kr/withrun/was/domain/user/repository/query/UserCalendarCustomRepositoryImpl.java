package kr.withrun.was.domain.user.repository.query;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.withrun.was.domain.user.entity.QUser;
import kr.withrun.was.domain.user.entity.QUserCalendar;
import kr.withrun.was.domain.user.entity.UserCalendar;
import kr.withrun.was.domain.user.repository.query.dto.UserCalendarDailySummaryRow;
import kr.withrun.was.domain.user.repository.query.dto.UserCalendarSummaryRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserCalendarCustomRepositoryImpl implements UserCalendarCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<UserCalendar> findByUserIdAndCalendarDateAndDeletedAtIsNull(Long userId, LocalDate calendarDate) {
        QUser user = QUser.user;
        QUserCalendar userCalendar = QUserCalendar.userCalendar;

        return Optional.ofNullable(queryFactory
                .selectFrom(userCalendar)
                .join(userCalendar.user, user)
                .where(
                        user.id.eq(userId),
                        user.deletedAt.isNull(),
                        userCalendar.calendarDate.eq(calendarDate),
                        userCalendar.deletedAt.isNull()
                )
                .fetchOne());
    }

    @Override
    public UserCalendarSummaryRow findSummaryRow(Long userId) {
        QUser user = QUser.user;
        QUserCalendar userCalendar = QUserCalendar.userCalendar;
        NumberExpression<Integer> runCountExpression = getRunCountExpression(userCalendar);

        UserCalendarSummaryRow summaryRow = queryFactory
                .select(Projections.constructor(
                        UserCalendarSummaryRow.class,
                        runCountExpression.sum().coalesce(0),
                        userCalendar.totalDistanceM.sum().coalesce(0),
                        userCalendar.courseRunCount.sum().coalesce(0),
                        userCalendar.freeRunCount.sum().coalesce(0),
                        userCalendar.ghostRunCount.sum().coalesce(0)
                ))
                .from(userCalendar)
                .join(userCalendar.user, user)
                .where(
                        user.id.eq(userId),
                        user.deletedAt.isNull(),
                        userCalendar.deletedAt.isNull()
                )
                .fetchOne();

        if (summaryRow == null) {
            return new UserCalendarSummaryRow(0, 0, 0, 0, 0);
        }

        return summaryRow;
    }

    @Override
    public List<UserCalendarDailySummaryRow> findDailySummaries(Long userId, LocalDate startDate, LocalDate endDate) {
        QUser user = QUser.user;
        QUserCalendar userCalendar = QUserCalendar.userCalendar;

        return queryFactory
                .select(Projections.constructor(
                        UserCalendarDailySummaryRow.class,
                        userCalendar.calendarDate,
                        userCalendar.totalDistanceM,
                        userCalendar.totalDurationSec,
                        userCalendar.totalCaloriesKcal,
                        userCalendar.courseRunCount,
                        userCalendar.freeRunCount,
                        userCalendar.ghostRunCount
                ))
                .from(userCalendar)
                .join(userCalendar.user, user)
                .where(
                        user.id.eq(userId),
                        user.deletedAt.isNull(),
                        userCalendar.deletedAt.isNull(),
                        userCalendar.calendarDate.goe(startDate),
                        userCalendar.calendarDate.loe(endDate)
                )
                .orderBy(userCalendar.calendarDate.asc())
                .fetch();
    }

    @Override
    public List<LocalDate> findRunningDates(Long userId) {
        QUser user = QUser.user;
        QUserCalendar userCalendar = QUserCalendar.userCalendar;
        NumberExpression<Integer> runCountExpression = getRunCountExpression(userCalendar);

        return queryFactory
                .select(userCalendar.calendarDate)
                .from(userCalendar)
                .join(userCalendar.user, user)
                .where(
                        user.id.eq(userId),
                        user.deletedAt.isNull(),
                        userCalendar.deletedAt.isNull(),
                        runCountExpression.gt(0)
                )
                .orderBy(userCalendar.calendarDate.desc())
                .fetch();
    }

    private static NumberExpression<Integer> getRunCountExpression(QUserCalendar userCalendar) {
        return userCalendar.courseRunCount
                .add(userCalendar.freeRunCount)
                .add(userCalendar.ghostRunCount);
    }
}
