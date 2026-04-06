package kr.withrun.was.domain.course.repository.query;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.withrun.was.domain.course.entity.QCourse;
import kr.withrun.was.domain.course.entity.QUserCourseInteraction;
import kr.withrun.was.domain.course.repository.query.dto.CourseCollaborativeScoreRow;
import kr.withrun.was.domain.course.type.CourseStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class UserCourseInteractionCustomRepositoryImpl implements UserCourseInteractionCustomRepository {

    private static final int DEFAULT_RECENT_INTERACTION_LIMIT = 30;

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Long> findRecentInteractedCourseIds(Long userId, int limit) {
        if (userId == null) {
            return List.of();
        }
        int resolvedLimit = limit <= 0 ? DEFAULT_RECENT_INTERACTION_LIMIT : limit;

        QCourse course = QCourse.course;
        QUserCourseInteraction interaction = QUserCourseInteraction.userCourseInteraction;

        return queryFactory
                .select(interaction.course.id)
                .from(interaction)
                .join(interaction.course, course)
                .where(
                        interaction.user.id.eq(userId),
                        interaction.deletedAt.isNull(),
                        isPublicCourse(course)
                )
                .orderBy(
                        interaction.occurredAt.desc(),
                        interaction.id.desc()
                )
                .limit(resolvedLimit)
                .fetch();
    }

    @Override
    public List<CourseCollaborativeScoreRow> findCollaborativeScoreRows(
            Long userId,
            List<Long> seedCourseIds,
            List<Long> candidateCourseIds
    ) {
        if (userId == null || seedCourseIds == null || seedCourseIds.isEmpty()
                || candidateCourseIds == null || candidateCourseIds.isEmpty()) {
            return List.of();
        }

        QCourse seedCourse = new QCourse("seedCourse");
        QCourse neighborCourse = new QCourse("neighborCourse");
        QUserCourseInteraction seedInteraction = new QUserCourseInteraction("seedInteraction");
        QUserCourseInteraction neighborInteraction = new QUserCourseInteraction("neighborInteraction");

        return queryFactory
                .select(Projections.constructor(
                        CourseCollaborativeScoreRow.class,
                        neighborInteraction.course.id,
                        neighborInteraction.user.id.countDistinct()
                ))
                .from(seedInteraction)
                .join(seedInteraction.course, seedCourse)
                .join(neighborInteraction).on(seedInteraction.user.id.eq(neighborInteraction.user.id))
                .join(neighborInteraction.course, neighborCourse)
                .where(
                        seedInteraction.user.id.ne(userId),
                        seedInteraction.course.id.in(seedCourseIds),
                        seedInteraction.deletedAt.isNull(),
                        isPublicCourse(seedCourse),
                        neighborInteraction.course.id.in(candidateCourseIds),
                        neighborInteraction.course.id.notIn(seedCourseIds),
                        neighborInteraction.deletedAt.isNull(),
                        isPublicCourse(neighborCourse)
                )
                .groupBy(neighborInteraction.course.id)
                .fetch();
    }

    private BooleanExpression isPublicCourse(QCourse course) {
        return course.deletedAt.isNull()
                .and(course.status.in(CourseStatus.OFFICIAL, CourseStatus.COMMUNITY));
    }
}

