package kr.withrun.was.domain.user.repository.query;

import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.withrun.was.domain.user.entity.QUser;
import kr.withrun.was.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserQueryRepositoryImpl implements UserQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<User> findNotDeletedUser(Long userId) {
        QUser user = QUser.user;

        return Optional.ofNullable(
                queryFactory
                        .selectFrom(user)
                        .where(
                                user.id.eq(userId),
                                user.deletedAt.isNull()
                        )
                        .fetchOne()
        );
    }
}
