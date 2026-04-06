package kr.withrun.was.domain.user.repository;

import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.repository.query.UserQueryRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long>, UserQueryRepository {

    @Query("""
            select count(user) > 0
            from User user
            where user.nickname = :nickname
              and user.deletedAt is null
            """)
    boolean existsActiveUserByNickname(@Param("nickname") String nickname);

    // 현재 사용자 본인을 제외한 "다른 활성 사용자"가 같은 닉네임을 쓰는지만 확인합니다.
    // soft delete 된 사용자는 중복 검사 대상에서 제외해야 하므로 deletedAt is null 조건을 함께 둡니다.
    @Query("""
            select count(user) > 0
            from User user
            where user.nickname = :nickname
              and user.id <> :userId
              and user.deletedAt is null
            """)
    boolean existsByNicknameAndIdNot(@Param("nickname") String nickname, @Param("userId") Long userId);
}
