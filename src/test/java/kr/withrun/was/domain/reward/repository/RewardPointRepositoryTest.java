package kr.withrun.was.domain.reward.repository;

import jakarta.persistence.EntityManager;
import kr.withrun.was.domain.reward.entity.RewardPointBalance;
import kr.withrun.was.domain.reward.entity.RewardPointHistory;
import kr.withrun.was.domain.reward.type.RewardPointReason;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.global.config.JpaAuditingConfig;
import kr.withrun.was.global.config.QuerydslConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
        "spring.config.import=",
        "spring.cloud.aws.parameterstore.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:reward-point-repository;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({JpaAuditingConfig.class, QuerydslConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("리워드 포인트 리포지토리")
class RewardPointRepositoryTest {

    @Autowired
    private RewardPointBalanceRepository rewardPointBalanceRepository;

    @Autowired
    private RewardPointHistoryRepository rewardPointHistoryRepository;

    @Autowired
    private EntityManager entityManager;

    @DisplayName("포인트 잔액은 사용자와 shared PK 1:1로 저장된다")
    @Test
    void storesRewardPointBalanceWithSharedPrimaryKey() {
        User user = persistUser("runner-balance");

        RewardPointBalance savedBalance = rewardPointBalanceRepository.saveAndFlush(RewardPointBalance.create(user));
        entityManager.clear();

        RewardPointBalance foundBalance = rewardPointBalanceRepository.findByIdAndDeletedAtIsNull(userId(user)).orElseThrow();

        assertThat(savedBalance.getId()).isEqualTo(userId(user));
        assertThat(foundBalance.getId()).isEqualTo(userId(user));
        assertThat(foundBalance.getCurrentBalance()).isZero();
    }

    @DisplayName("같은 사용자와 중복 방지 키 조합은 한 번만 저장된다")
    @Test
    void rejectsDuplicateHistoryForSameUserAndIdempotencyKey() {
        User user = persistUser("runner-history");

        rewardPointHistoryRepository.saveAndFlush(
                RewardPointHistory.create(user, 1, RewardPointReason.DAILY_RUNNING, "reward:daily-running:1:2026-03-29")
        );

        assertThatThrownBy(() -> rewardPointHistoryRepository.saveAndFlush(
                RewardPointHistory.create(user, 1, RewardPointReason.DAILY_RUNNING, "reward:daily-running:1:2026-03-29")
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @DisplayName("현재 잔액이 충분하면 포인트를 1 차감한다")
    @Test
    void decreasesRewardPointBalanceWhenEnough() {
        User user = persistUser("runner-ok-1");
        RewardPointBalance rewardPointBalance = rewardPointBalanceRepository.saveAndFlush(RewardPointBalance.create(user));
        entityManager.clear();

        rewardPointBalanceRepository.increaseBalance(userId(user), 3);

        int updatedRowCount = rewardPointBalanceRepository.decreaseBalanceIfEnough(userId(user), 1);
        entityManager.clear();

        RewardPointBalance foundBalance = rewardPointBalanceRepository.findByIdAndDeletedAtIsNull(userId(user)).orElseThrow();
        assertThat(rewardPointBalance.getId()).isEqualTo(userId(user));
        assertThat(updatedRowCount).isEqualTo(1);
        assertThat(foundBalance.getCurrentBalance()).isEqualTo(2);
    }

    @DisplayName("현재 잔액이 부족하면 포인트 차감이 수행되지 않는다")
    @Test
    void doesNotDecreaseRewardPointBalanceWhenInsufficient() {
        User user = persistUser("runner-no-1");
        rewardPointBalanceRepository.saveAndFlush(RewardPointBalance.create(user));
        entityManager.clear();

        int updatedRowCount = rewardPointBalanceRepository.decreaseBalanceIfEnough(userId(user), 1);
        entityManager.clear();

        RewardPointBalance foundBalance = rewardPointBalanceRepository.findByIdAndDeletedAtIsNull(userId(user)).orElseThrow();
        assertThat(updatedRowCount).isZero();
        assertThat(foundBalance.getCurrentBalance()).isZero();
    }

    private Long userId(User user) {
        return (Long) entityManager.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(user);
    }

    private User persistUser(String nickname) {
        User user = User.createPendingSocialUser(nickname);
        entityManager.persist(user);
        entityManager.flush();
        return user;
    }
}
