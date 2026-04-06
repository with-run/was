package kr.withrun.was.domain.course.repository.query;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.withrun.was.domain.course.entity.CourseBookmark;
import kr.withrun.was.domain.course.entity.QCourse;
import kr.withrun.was.domain.course.entity.QCourseBookmark;
import kr.withrun.was.domain.course.entity.QCourseDifficulty;
import kr.withrun.was.domain.course.entity.QCourseLike;
import kr.withrun.was.domain.course.entity.QCourseTypeMap;
import kr.withrun.was.domain.course.repository.query.dto.BookmarkedCourseRow;
import kr.withrun.was.domain.course.type.CourseStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class CourseBookmarkCustomRepositoryImpl implements CourseBookmarkCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public long countByCourseId(Long courseId) {
        QCourseBookmark courseBookmark = QCourseBookmark.courseBookmark;

        Long result = queryFactory
                .select(courseBookmark.count())
                .from(courseBookmark)
                .where(courseBookmark.course.id.eq(courseId))
                .fetchOne();

        return result == null ? 0L : result;
    }

    @Override
    public Map<Long, Long> countByCourseIds(List<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return Map.of();
        }

        QCourseBookmark courseBookmark = QCourseBookmark.courseBookmark;

        return queryFactory
                .select(courseBookmark.course.id, courseBookmark.count())
                .from(courseBookmark)
                .where(courseBookmark.course.id.in(courseIds))
                .groupBy(courseBookmark.course.id)
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(courseBookmark.course.id),
                        tuple -> tuple.get(courseBookmark.count())
                ));
    }

    @Override
    public Optional<CourseBookmark> findByCourseIdAndUserId(Long courseId, Long userId) {
        QCourseBookmark courseBookmark = QCourseBookmark.courseBookmark;

        return Optional.ofNullable(
                queryFactory
                        .selectFrom(courseBookmark)
                        .where(
                                courseBookmark.course.id.eq(courseId),
                                courseBookmark.user.id.eq(userId)
                        )
                        .fetchOne()
        );
    }


    @Override
    public List<BookmarkedCourseRow> findBookmarkedCourseRows(Long userId, LocalDateTime cursorBookmarkedAt, Long cursorBookmarkId, int pageSize) {
        QCourseBookmark courseBookmark = QCourseBookmark.courseBookmark;
        QCourse course = QCourse.course;
        QCourseDifficulty courseDifficulty = QCourseDifficulty.courseDifficulty;
        QCourseLike courseLike = QCourseLike.courseLike;
        QCourseLike currentUserCourseLike = new QCourseLike("currentUserCourseLike");
        QCourseTypeMap courseTypeMap = QCourseTypeMap.courseTypeMap;
        Expression<Long> likeCountExpression = JPAExpressions
                .select(courseLike.count())
                .from(courseLike)
                .where(courseLike.course.eq(course));
        BooleanExpression isLikedExpression = currentUserCourseLike.id.isNotNull();

        List<Long> pagedBookmarkIds = queryFactory
                .select(courseBookmark.id)
                .from(courseBookmark)
                .join(courseBookmark.course, course)
                .where(
                        courseBookmark.user.id.eq(userId),
                        course.deletedAt.isNull(),
                        isPublicCourse(course),
                        isAfterCursor(courseBookmark, cursorBookmarkedAt, cursorBookmarkId)
                )
                .orderBy(
                        courseBookmark.createdAt.desc(),
                        courseBookmark.id.desc()
                )
                .limit((long) pageSize + 1)
                .fetch();

        if (pagedBookmarkIds.isEmpty()) {
            return List.of();
        }

        return queryFactory
                .select(Projections.constructor(
                        BookmarkedCourseRow.class,
                        courseBookmark.id,
                        courseBookmark.createdAt,
                        course.id,
                        course.title,
                        course.status,
                        course.routeType,
                        course.distanceM,
                        course.elevationGainM,
                        courseDifficulty.difficulty,
                        courseTypeMap.courseType,
                        course.snapshotImageUrl,
                        likeCountExpression,
                        isLikedExpression,
                        Expressions.constant(Boolean.TRUE)
                ))
                .from(courseBookmark)
                .join(courseBookmark.course, course)
                .leftJoin(courseDifficulty).on(courseDifficulty.course.eq(course)
                        .and(courseDifficulty.deletedAt.isNull()))
                .leftJoin(courseTypeMap).on(courseTypeMap.course.eq(course)
                        .and(courseTypeMap.deletedAt.isNull()))
                .leftJoin(currentUserCourseLike).on(currentUserCourseLike.course.eq(course)
                        .and(currentUserCourseLike.user.id.eq(userId)))
                .where(
                        courseBookmark.id.in(pagedBookmarkIds),
                        courseBookmark.user.id.eq(userId),
                        course.deletedAt.isNull(),
                        isPublicCourse(course)
                )
                .orderBy(
                        courseBookmark.createdAt.desc(),
                        courseBookmark.id.desc(),
                        courseTypeMap.id.asc().nullsLast()
                )
                .fetch();
    }

    @Override
    public void deleteBookmark(Long courseId, Long userId) {
        QCourseBookmark courseBookmark = QCourseBookmark.courseBookmark;

        queryFactory
                .delete(courseBookmark)
                .where(
                        courseBookmark.course.id.eq(courseId),
                        courseBookmark.user.id.eq(userId)
                )
                .execute();
    }

    private BooleanExpression isAfterCursor(
            QCourseBookmark courseBookmark,
            LocalDateTime cursorBookmarkedAt,
            Long cursorBookmarkId
    ) {
        if (cursorBookmarkedAt == null || cursorBookmarkId == null) {
            return null;
        }

        return courseBookmark.createdAt.lt(cursorBookmarkedAt)
                .or(courseBookmark.createdAt.eq(cursorBookmarkedAt)
                        .and(courseBookmark.id.lt(cursorBookmarkId)));
    }

    private BooleanExpression isPublicCourse(QCourse course) {
        return course.status.in(CourseStatus.OFFICIAL, CourseStatus.COMMUNITY);
    }
}
