package kr.withrun.was.domain.user.repository;

// 테이블을 조회/저장하기 위한 JPA 레포지토리 인터페이스 파일
// UserAuthAccount 엔티티를 DB에서 찾고 저장하는 창구
import kr.withrun.was.domain.user.entity.UserAuthAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAuthAccountRepository extends JpaRepository<UserAuthAccount, Long> {

    Optional<UserAuthAccount> findByProviderAndProviderUserId(String provider, String providerUserId);
}
