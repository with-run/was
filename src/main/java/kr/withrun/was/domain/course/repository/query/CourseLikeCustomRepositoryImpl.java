package kr.withrun.was.domain.course.repository.query;

import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.withrun.was.domain.course.entity.QCourseLike;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class CourseLikeCustomRepositoryImpl implements CourseLikeCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public long countByCourseId(Long courseId) {
        QCourseLike courseLike = QCourseLike.courseLike;

        Long result = queryFactory
                .select(courseLike.count())
                .from(courseLike)
                .where(courseLike.course.id.eq(courseId))
                .fetchOne();

        return result == null ? 0L : result;
    }

    @Override
    public Map<Long, Long> countByCourseIds(List<Long> courseIds) {
        if (courseIds.isEmpty()) return Map.of();

        QCourseLike courseLike = QCourseLike.courseLike;

        return queryFactory
                .select(courseLike.course.id, courseLike.count())
                .from(courseLike)
                .where(courseLike.course.id.in(courseIds))
                .groupBy(courseLike.course.id)
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(courseLike.course.id),
                        tuple -> tuple.get(courseLike.count())
                ));
    }
}
