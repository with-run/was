package kr.withrun.was.domain.course.repository.query;

import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.withrun.was.domain.course.entity.QCourseReview;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CourseReviewCustomRepositoryImpl implements CourseReviewCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Double findAverageRatingByCourseId(Long courseId) {
        QCourseReview courseReview = QCourseReview.courseReview;

        return queryFactory
                .select(courseReview.rating.avg())
                .from(courseReview)
                .where(
                        courseReview.course.id.eq(courseId),
                        courseReview.deletedAt.isNull()
                )
                        .fetchOne();
    }

    @Override
    public boolean existsByCourseIdAndUserId(Long courseId, Long userId) {
        QCourseReview courseReview = QCourseReview.courseReview;

        Integer fetchedId = queryFactory
                .selectOne()
                .from(courseReview)
                .where(
                        courseReview.course.id.eq(courseId),
                        courseReview.user.id.eq(userId)
                )
                .fetchFirst();

        return fetchedId != null;
    }
}
