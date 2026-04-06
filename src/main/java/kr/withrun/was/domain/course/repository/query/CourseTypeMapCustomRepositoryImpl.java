package kr.withrun.was.domain.course.repository.query;

import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.withrun.was.domain.course.entity.QCourseTypeMap;
import kr.withrun.was.domain.course.type.CourseType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CourseTypeMapCustomRepositoryImpl implements CourseTypeMapCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<CourseType> findCourseTypesByCourseId(Long courseId) {
        QCourseTypeMap courseTypeMap = QCourseTypeMap.courseTypeMap;

        return queryFactory
                .select(courseTypeMap.courseType)
                .distinct()
                .from(courseTypeMap)
                .where(
                        courseTypeMap.course.id.eq(courseId),
                        courseTypeMap.deletedAt.isNull()
                )
                .fetch();
    }
}
