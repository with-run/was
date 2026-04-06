package kr.withrun.was.domain.user.repository.query;

import kr.withrun.was.domain.user.entity.User;

import java.util.Optional;

public interface UserQueryRepository {

    Optional<User> findNotDeletedUser(Long userId);
}
