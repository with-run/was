package kr.withrun.was.domain.reward.repository;

import jakarta.persistence.EntityManager;
import kr.withrun.was.domain.reward.entity.RewardDrawPool;
import kr.withrun.was.global.config.JpaAuditingConfig;
import kr.withrun.was.global.config.QuerydslConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.config.import=",
        "spring.cloud.aws.parameterstore.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:reward-draw-pool-repository;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({JpaAuditingConfig.class, QuerydslConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("리워드 draw pool 리포지토리")
class RewardDrawPoolRepositoryTest {

    @Autowired
    private RewardDrawPoolRepository rewardDrawPoolRepository;

    @Autowired
    private EntityManager entityManager;

    @DisplayName("현재 활성 pool 조회는 active=true 이고 미삭제인 pool만 반환한다")
    @Test
    void findsActiveRewardDrawPoolOnlyWhenNotDeleted() {
        RewardDrawPool inactivePool = rewardDrawPoolRepository.saveAndFlush(RewardDrawPool.create("Inactive Pool", 3, 40));
        RewardDrawPool deletedActivePool = rewardDrawPoolRepository.saveAndFlush(RewardDrawPool.create("Deleted Active Pool", 3, 40));
        deletedActivePool.activate();
        deletedActivePool.delete();
        RewardDrawPool activePool = rewardDrawPoolRepository.saveAndFlush(RewardDrawPool.create("Active Pool", 3, 40));
        activePool.activate();

        entityManager.flush();
        entityManager.clear();

        RewardDrawPool found = rewardDrawPoolRepository.findByActiveTrueAndDeletedAtIsNull().orElseThrow();

        assertThat(found.getId()).isEqualTo(activePool.getId());
        assertThat(found.getName()).isEqualTo("Active Pool");
        assertThat(found.isActive()).isTrue();
        assertThat(inactivePool.isActive()).isFalse();
    }
}
