package kr.withrun.was.domain.course.repository.query;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import kr.withrun.was.domain.course.dto.PreferredDistanceRange;
import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.entity.QCourse;
import kr.withrun.was.domain.course.entity.QCourseBookmark;
import kr.withrun.was.domain.course.entity.QCourseDifficulty;
import kr.withrun.was.domain.course.entity.QCourseLike;
import kr.withrun.was.domain.course.entity.QCourseTypeMap;
import kr.withrun.was.domain.course.repository.query.dto.NearbyCoursePageRow;
import kr.withrun.was.domain.course.repository.query.dto.NearbyRecommendationCandidateRow;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.type.NearbyCourseSortBy;
import kr.withrun.was.domain.course.type.NearbyGhostCourseSortBy;
import kr.withrun.was.domain.running.entity.QRunningSession;
import kr.withrun.was.domain.running.type.RunningMode;
import kr.withrun.was.domain.running.type.RunningSessionCompleteState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CourseCustomRepositoryImpl implements CourseCustomRepository {

    private static final double EARTH_RADIUS_M = 6_371_000D;
    private static final int SHAPE_METRIC_SRID = 5179;
    private static final String EXISTS_PUBLIC_DUPLICATE_COURSE_BY_ROUTE_GEOMETRY_QUERY = """
            SELECT c.course_id
            FROM courses c
            CROSS JOIN (
                SELECT ST_Segmentize(
                    ST_GeomFromText(:routeLineStringWkt, 4326)::geography,
                    :shapeSegmentizeStepM
                )::geometry AS input_geom
            ) i
            CROSS JOIN LATERAL (
                SELECT
                    ST_Transform(i.input_geom, :shapeMetricSrid) AS input_geom_m,
                    ST_Transform(
                        ST_Segmentize(c.route_geom::geography, :shapeSegmentizeStepM)::geometry,
                        :shapeMetricSrid
                    ) AS course_geom_m
            ) g
            WHERE c.deleted_at IS NULL
              AND c.route_geom IS NOT NULL
              AND c.status IN ('OFFICIAL', 'COMMUNITY')
              AND c.end_latitude IS NOT NULL
              AND c.end_longitude IS NOT NULL
              AND c.distance_m BETWEEN :minDistanceM AND :maxDistanceM
              AND (
                    (
                        ST_DWithin(
                            ST_SetSRID(ST_MakePoint(c.start_longitude, c.start_latitude), 4326)::geography,
                            ST_SetSRID(ST_MakePoint(:startLongitude, :startLatitude), 4326)::geography,
                            :endpointThresholdM
                        )
                        AND ST_DWithin(
                            ST_SetSRID(ST_MakePoint(c.end_longitude, c.end_latitude), 4326)::geography,
                            ST_SetSRID(ST_MakePoint(:endLongitude, :endLatitude), 4326)::geography,
                            :endpointThresholdM
                        )
                    )
                    OR
                    (
                        ST_DWithin(
                            ST_SetSRID(ST_MakePoint(c.start_longitude, c.start_latitude), 4326)::geography,
                            ST_SetSRID(ST_MakePoint(:endLongitude, :endLatitude), 4326)::geography,
                            :endpointThresholdM
                        )
                        AND ST_DWithin(
                            ST_SetSRID(ST_MakePoint(c.end_longitude, c.end_latitude), 4326)::geography,
                            ST_SetSRID(ST_MakePoint(:startLongitude, :startLatitude), 4326)::geography,
                            :endpointThresholdM
                        )
                    )
              )
              AND ST_Length(g.input_geom_m) > 0
              AND ST_Length(g.course_geom_m) > 0
              AND LEAST(
                    ST_Length(ST_Intersection(
                        g.course_geom_m,
                        ST_Buffer(g.input_geom_m, :shapeToleranceM)
                    )) / ST_Length(g.course_geom_m),
                    ST_Length(ST_Intersection(
                        g.input_geom_m,
                        ST_Buffer(g.course_geom_m, :shapeToleranceM)
                    )) / ST_Length(g.input_geom_m)
              ) >= :shapeMinOverlapRatio
            LIMIT 1
            """;

    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;

    @Override
    public Optional<Course> findNotDeletedCourse(Long courseId) {
        QCourse course = QCourse.course;

        return Optional.ofNullable(
                queryFactory
                        .selectFrom(course)
                        .where(
                                course.id.eq(courseId),
                                course.deletedAt.isNull()
                        )
                        .fetchOne()
        );
    }

    @Override
    public List<NearbyRecommendationCandidateRow> findNearbyRecommendationCandidates(
            List<PreferredDistanceRange> preferredDistanceMs,
            double targetLatitude,
            double targetLongitude,
            double userLatitude,
            double userLongitude,
            int radiusM
    ) {
        QCourse course = QCourse.course;
        QCourseDifficulty difficulty = QCourseDifficulty.courseDifficulty;
        QCourseTypeMap courseTypeMap = QCourseTypeMap.courseTypeMap;
        NumberExpression<Integer> distanceFromTargetM =
                distanceMetersExpression(course.startLatitude, course.startLongitude, targetLatitude, targetLongitude);
        NumberExpression<Integer> distanceFromUserM =
                distanceMetersExpression(course.startLatitude, course.startLongitude, userLatitude, userLongitude);

        return queryFactory
                .select(Projections.constructor(
                        NearbyRecommendationCandidateRow.class,
                        course.id,
                        course.title,
                        course.status,
                        course.routeType,
                        course.distanceM,
                        course.elevationGainM,
                        course.startLatitude,
                        course.startLongitude,
                        course.endLatitude,
                        course.endLongitude,
                        course.snapshotImageUrl,
                        course.coordinates,
                        difficulty.difficulty,
                        courseTypeMap.courseType,
                        distanceFromTargetM,
                        distanceFromUserM
                ))
                .from(course)
                .leftJoin(difficulty).on(
                        difficulty.course.eq(course)
                                .and(difficulty.deletedAt.isNull())
                )
                .leftJoin(courseTypeMap).on(
                        courseTypeMap.course.eq(course)
                                .and(courseTypeMap.deletedAt.isNull())
                )
                .where(
                        nearbyCourseWhereClause(course, preferredDistanceMs),
                        distanceFromTargetM.loe(radiusM)
                )
                .orderBy(
                        distanceFromUserM.asc(),
                        course.id.asc(),
                        courseTypeMap.id.asc().nullsLast()
                )
                .fetch();
    }

    @Override
    public List<NearbyCoursePageRow> findNearbyCoursePageRows(
            List<PreferredDistanceRange> preferredDistanceMs,
            double targetLatitude,
            double targetLongitude,
            double userLatitude,
            double userLongitude,
            CourseStatus status,
            NearbyCourseSortBy sortBy,
            int radiusM,
            int page,
            int size
    ) {
        QCourse course = QCourse.course;
        QCourseDifficulty difficulty = QCourseDifficulty.courseDifficulty;
        NumberExpression<Integer> distanceFromTargetM =
                distanceMetersExpression(course.startLatitude, course.startLongitude, targetLatitude, targetLongitude);
        NumberExpression<Integer> distanceFromUserM =
                distanceMetersExpression(course.startLatitude, course.startLongitude, userLatitude, userLongitude);
        OrderSpecifier<?>[] orderSpecifiers = nearbyCourseOrderSpecifiers(course, distanceFromUserM, sortBy);

        return queryFactory
                .select(Projections.constructor(
                        NearbyCoursePageRow.class,
                        course.id,
                        course.title,
                        course.status,
                        course.routeType,
                        course.distanceM,
                        course.elevationGainM,
                        course.startLatitude,
                        course.startLongitude,
                        course.endLatitude,
                        course.endLongitude,
                        course.snapshotImageUrl,
                        difficulty.difficulty,
                        distanceFromTargetM,
                        distanceFromUserM
                ))
                .from(course)
                .leftJoin(difficulty).on(
                        difficulty.course.eq(course)
                                .and(difficulty.deletedAt.isNull())
                )
                .where(
                        nearbyCourseWhereClause(course, preferredDistanceMs, status),
                        distanceFromTargetM.loe(radiusM)
                )
                .orderBy(orderSpecifiers)
                .offset((long) page * size)
                .limit(size)
                .fetch();
    }

    @Override
    public List<NearbyCoursePageRow> findNearbyGhostCoursePageRows(
            List<PreferredDistanceRange> preferredDistanceMs,
            double targetLatitude,
            double targetLongitude,
            double userLatitude,
            double userLongitude,
            NearbyGhostCourseSortBy sortBy,
            int radiusM,
            int page,
            int size
    ) {
        QCourse course = QCourse.course;
        QCourseDifficulty difficulty = QCourseDifficulty.courseDifficulty;
        NumberExpression<Integer> distanceFromTargetM =
                distanceMetersExpression(course.startLatitude, course.startLongitude, targetLatitude, targetLongitude);
        NumberExpression<Integer> distanceFromUserM =
                distanceMetersExpression(course.startLatitude, course.startLongitude, userLatitude, userLongitude);
        OrderSpecifier<?>[] orderSpecifiers = nearbyGhostCourseOrderSpecifiers(course, distanceFromUserM, sortBy);

        return queryFactory
                .select(Projections.constructor(
                        NearbyCoursePageRow.class,
                        course.id,
                        course.title,
                        course.status,
                        course.routeType,
                        course.distanceM,
                        course.elevationGainM,
                        course.startLatitude,
                        course.startLongitude,
                        course.endLatitude,
                        course.endLongitude,
                        course.snapshotImageUrl,
                        difficulty.difficulty,
                        distanceFromTargetM,
                        distanceFromUserM
                ))
                .from(course)
                .leftJoin(difficulty).on(
                        difficulty.course.eq(course)
                                .and(difficulty.deletedAt.isNull())
                )
                .where(
                        nearbyGhostCourseWhereClause(course, preferredDistanceMs),
                        distanceFromTargetM.loe(radiusM)
                )
                .orderBy(orderSpecifiers)
                .offset((long) page * size)
                .limit(size)
                .fetch();
    }

    @Override
    public long countNearbyCourses(
            List<PreferredDistanceRange> preferredDistanceMs,
            double targetLatitude,
            double targetLongitude,
            CourseStatus status,
            int radiusM
    ) {
        QCourse course = QCourse.course;
        NumberExpression<Integer> distanceFromTargetM =
                distanceMetersExpression(course.startLatitude, course.startLongitude, targetLatitude, targetLongitude);

        Long count = queryFactory
                .select(course.id.count())
                .from(course)
                .where(
                        nearbyCourseWhereClause(course, preferredDistanceMs, status),
                        distanceFromTargetM.loe(radiusM)
                )
                .fetchOne();

        return count == null ? 0L : count;
    }

    @Override
    public long countNearbyGhostCourses(
            List<PreferredDistanceRange> preferredDistanceMs,
            double targetLatitude,
            double targetLongitude,
            int radiusM
    ) {
        QCourse course = QCourse.course;
        NumberExpression<Integer> distanceFromTargetM =
                distanceMetersExpression(course.startLatitude, course.startLongitude, targetLatitude, targetLongitude);

        Long count = queryFactory
                .select(course.id.count())
                .from(course)
                .where(
                        nearbyGhostCourseWhereClause(course, preferredDistanceMs),
                        distanceFromTargetM.loe(radiusM)
                )
                .fetchOne();

        return count == null ? 0L : count;
    }

    @Override
    public boolean existsPublicDuplicateCourse(
            double startLatitude,
            double startLongitude,
            double endLatitude,
            double endLongitude,
            int distanceM,
            int endpointThresholdM,
            double distanceDiffRatio
    ) {
        QCourse course = QCourse.course;
        int normalizedDistanceM = Math.max(0, distanceM);
        int normalizedEndpointThresholdM = Math.max(0, endpointThresholdM);
        double normalizedDistanceDiffRatio = Math.max(0d, distanceDiffRatio);
        int minDistanceM = Math.max(0, (int) Math.floor(normalizedDistanceM * (1d - normalizedDistanceDiffRatio)));
        int maxDistanceM = (int) Math.ceil(normalizedDistanceM * (1d + normalizedDistanceDiffRatio));
        NumberExpression<Integer> startDistanceMeters =
                distanceMetersExpression(course.startLatitude, course.startLongitude, startLatitude, startLongitude);
        NumberExpression<Integer> endDistanceMeters =
                distanceMetersExpression(course.endLatitude, course.endLongitude, endLatitude, endLongitude);

        Long foundCourseId = queryFactory
                .select(course.id)
                .from(course)
                .where(
                        course.deletedAt.isNull(),
                        course.coordinates.isNotNull(),
                        course.status.in(CourseStatus.OFFICIAL, CourseStatus.COMMUNITY),
                        course.endLatitude.isNotNull(),
                        course.endLongitude.isNotNull(),
                        course.distanceM.between(minDistanceM, maxDistanceM),
                        startDistanceMeters.loe(normalizedEndpointThresholdM),
                        endDistanceMeters.loe(normalizedEndpointThresholdM)
                )
                .fetchFirst();

        return foundCourseId != null;
    }

    @Override
    public boolean existsPublicDuplicateCourseByRouteGeometry(
            double startLatitude,
            double startLongitude,
            double endLatitude,
            double endLongitude,
            int distanceM,
            int endpointThresholdM,
            double distanceDiffRatio,
            String routeLineStringWkt,
            double shapeToleranceM,
            double shapeMinOverlapRatio,
            double shapeSegmentizeStepM
    ) {
        if (routeLineStringWkt == null || routeLineStringWkt.isBlank()) {
            return false;
        }

        int normalizedDistanceM = Math.max(0, distanceM);
        int normalizedEndpointThresholdM = Math.max(0, endpointThresholdM);
        double normalizedDistanceDiffRatio = Math.max(0d, distanceDiffRatio);
        double normalizedShapeToleranceM = Math.max(1d, shapeToleranceM);
        double normalizedShapeMinOverlapRatio = Math.min(1d, Math.max(0d, shapeMinOverlapRatio));
        double normalizedShapeSegmentizeStepM = Math.max(1d, shapeSegmentizeStepM);
        int minDistanceM = Math.max(0, (int) Math.floor(normalizedDistanceM * (1d - normalizedDistanceDiffRatio)));
        int maxDistanceM = (int) Math.ceil(normalizedDistanceM * (1d + normalizedDistanceDiffRatio));

        List<?> foundRows = entityManager.createNativeQuery(EXISTS_PUBLIC_DUPLICATE_COURSE_BY_ROUTE_GEOMETRY_QUERY)
                .setParameter("routeLineStringWkt", routeLineStringWkt)
                .setParameter("startLatitude", startLatitude)
                .setParameter("startLongitude", startLongitude)
                .setParameter("endLatitude", endLatitude)
                .setParameter("endLongitude", endLongitude)
                .setParameter("endpointThresholdM", normalizedEndpointThresholdM)
                .setParameter("minDistanceM", minDistanceM)
                .setParameter("maxDistanceM", maxDistanceM)
                .setParameter("shapeToleranceM", normalizedShapeToleranceM)
                .setParameter("shapeMinOverlapRatio", normalizedShapeMinOverlapRatio)
                .setParameter("shapeSegmentizeStepM", normalizedShapeSegmentizeStepM)
                .setParameter("shapeMetricSrid", SHAPE_METRIC_SRID)
                .getResultList();

        return !foundRows.isEmpty();
    }

    private BooleanBuilder nearbyCourseWhereClause(QCourse course, List<PreferredDistanceRange> preferredDistanceMs) {
        BooleanBuilder whereClause = new BooleanBuilder()
                .and(course.deletedAt.isNull())
                .and(course.coordinates.isNotNull())
                .and(isVisibleOnPublicFeeds(course));

        if (preferredDistanceMs == null || preferredDistanceMs.isEmpty()) {
            return whereClause;
        }

        BooleanBuilder preferredDistancePredicate = new BooleanBuilder();
        for (PreferredDistanceRange preferredDistanceM : preferredDistanceMs) {
            preferredDistancePredicate.or(course.distanceM.between(preferredDistanceM.min(), preferredDistanceM.max()));
        }

        return whereClause.and(preferredDistancePredicate);
    }

    private BooleanBuilder nearbyCourseWhereClause(
            QCourse course,
            List<PreferredDistanceRange> preferredDistanceMs,
            CourseStatus status
    ) {
        BooleanBuilder whereClause = nearbyCourseWhereClause(course, preferredDistanceMs);
        if (status == null) {
            return whereClause;
        }

        return whereClause.and(course.status.eq(status));
    }

    private BooleanBuilder nearbyGhostCourseWhereClause(
            QCourse course,
            List<PreferredDistanceRange> preferredDistanceMs
    ) {
        BooleanBuilder whereClause = nearbyCourseWhereClause(course, preferredDistanceMs);
        return whereClause.and(course.status.eq(CourseStatus.OFFICIAL));
    }

    private BooleanBuilder isVisibleOnPublicFeeds(QCourse course) {
        return new BooleanBuilder(course.status.in(CourseStatus.OFFICIAL, CourseStatus.COMMUNITY));
    }

    private NumberExpression<Integer> distanceMetersExpression(
            NumberPath<Double> latitudePath,
            NumberPath<Double> longitudePath,
            double targetLatitude,
            double targetLongitude
    ) {
        return Expressions.numberTemplate(
                Integer.class,
                "cast(round({0} * 2 * asin(sqrt(power(sin(radians(({1} - {2}) / 2)), 2) + cos(radians({2})) * cos(radians({1})) * power(sin(radians(({3} - {4}) / 2)), 2)))) as integer)",
                Expressions.constant(EARTH_RADIUS_M),
                latitudePath,
                Expressions.constant(targetLatitude),
                longitudePath,
                Expressions.constant(targetLongitude)
        );
    }

    private OrderSpecifier<?>[] nearbyCourseOrderSpecifiers(
            QCourse course,
            NumberExpression<Integer> distanceFromUserM,
            NearbyCourseSortBy sortBy
    ) {
        if (sortBy == NearbyCourseSortBy.POPULAR) {
            return new OrderSpecifier<?>[]{
                    popularityScoreExpression(course).desc(),
                    distanceFromUserM.asc(),
                    course.id.asc()
            };
        }

        return new OrderSpecifier<?>[]{
                distanceFromUserM.asc(),
                course.id.asc()
        };
    }

    private OrderSpecifier<?>[] nearbyGhostCourseOrderSpecifiers(
            QCourse course,
            NumberExpression<Integer> distanceFromUserM,
            NearbyGhostCourseSortBy sortBy
    ) {
        if (sortBy == NearbyGhostCourseSortBy.GHOST_RUN_COUNT) {
            return new OrderSpecifier<?>[]{
                    ghostRunCountExpression(course).desc(),
                    distanceFromUserM.asc(),
                    course.id.asc()
            };
        }

        return new OrderSpecifier<?>[]{
                distanceFromUserM.asc(),
                course.id.asc()
        };
    }

    private NumberExpression<Long> popularityScoreExpression(QCourse course) {
        QCourseLike courseLike = QCourseLike.courseLike;
        QCourseBookmark courseBookmark = QCourseBookmark.courseBookmark;
        NumberExpression<Long> likeCountExpression = Expressions.numberTemplate(
                Long.class,
                "coalesce(({0}), 0)",
                JPAExpressions.select(courseLike.count())
                        .from(courseLike)
                        .where(courseLike.course.id.eq(course.id))
        );
        NumberExpression<Long> bookmarkCountExpression = Expressions.numberTemplate(
                Long.class,
                "coalesce(({0}), 0)",
                JPAExpressions.select(courseBookmark.count())
                        .from(courseBookmark)
                        .where(courseBookmark.course.id.eq(course.id))
        );

        return likeCountExpression.add(bookmarkCountExpression);
    }

    private NumberExpression<Long> ghostRunCountExpression(QCourse course) {
        QRunningSession runningSession = QRunningSession.runningSession;

        return Expressions.numberTemplate(
                Long.class,
                "coalesce(({0}), 0)",
                JPAExpressions.select(runningSession.count())
                        .from(runningSession)
                        .where(
                                runningSession.course.id.eq(course.id),
                                runningSession.mode.eq(RunningMode.GHOST),
                                runningSession.completeState.eq(RunningSessionCompleteState.SUCCESS),
                                runningSession.deletedAt.isNull()
                        )
        );
    }
}
