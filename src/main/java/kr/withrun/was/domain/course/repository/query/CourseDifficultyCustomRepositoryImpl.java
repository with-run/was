package kr.withrun.was.domain.course.repository.query;

import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.withrun.was.domain.course.entity.QCourseDifficulty;
import kr.withrun.was.global.common.type.Difficulty;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CourseDifficultyCustomRepositoryImpl implements CourseDifficultyCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<Difficulty> findDifficultyByCourseId(Long courseId) {
        QCourseDifficulty courseDifficulty = QCourseDifficulty.courseDifficulty;

        return Optional.ofNullable(
                queryFactory
                        .select(courseDifficulty.difficulty)
                        .from(courseDifficulty)
                        .where(
                                courseDifficulty.course.id.eq(courseId),
                                courseDifficulty.deletedAt.isNull()
                        )
                        .fetchOne()
        );
    }
}
