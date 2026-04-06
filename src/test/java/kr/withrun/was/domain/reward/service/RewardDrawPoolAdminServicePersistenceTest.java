package kr.withrun.was.domain.reward.service;

import jakarta.persistence.EntityManager;
import kr.withrun.was.domain.file.service.CloudFrontSignedUrlService;
import kr.withrun.was.domain.reward.dto.RewardDrawPoolResponse;
import kr.withrun.was.domain.reward.entity.RewardDrawPool;
import kr.withrun.was.domain.reward.repository.RewardDrawPoolRepository;
import kr.withrun.was.global.config.JpaAuditingConfig;
import kr.withrun.was.global.config.QuerydslConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.config.import=",
        "spring.cloud.aws.parameterstore.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:reward-draw-pool-admin-service;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({
        JpaAuditingConfig.class,
        QuerydslConfig.class,
        RewardDrawPoolAdminService.class,
        RewardDrawPoolAdminServicePersistenceTest.TestConfig.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("리워드 draw pool 관리자 서비스 영속성")
class RewardDrawPoolAdminServicePersistenceTest {

    @Autowired
    private RewardDrawPoolAdminService rewardDrawPoolAdminService;

    @Autowired
    private RewardDrawPoolRepository rewardDrawPoolRepository;

    @Autowired
    private EntityManager entityManager;

    @DisplayName("활성화는 bulk deactivate 이후에도 대상 pool 상태를 DB에 반영한다")
    @Test
    void persistsTargetActivationAfterBulkDeactivate() {
        RewardDrawPool currentlyActivePool = rewardDrawPoolRepository.saveAndFlush(
                RewardDrawPool.create("Current Pool", 3, 40)
        );
        currentlyActivePool.activate();

        RewardDrawPool targetPool = rewardDrawPoolRepository.saveAndFlush(
                RewardDrawPool.create("Target Pool", 3, 40)
        );

        entityManager.flush();
        entityManager.clear();

        RewardDrawPoolResponse response = rewardDrawPoolAdminService.activateRewardDrawPool(targetPool.getId());

        entityManager.flush();
        entityManager.clear();

        RewardDrawPool reloadedCurrentPool = rewardDrawPoolRepository.findByIdAndDeletedAtIsNull(currentlyActivePool.getId()).orElseThrow();
        RewardDrawPool reloadedTargetPool = rewardDrawPoolRepository.findByIdAndDeletedAtIsNull(targetPool.getId()).orElseThrow();

        assertThat(response.isActive()).isTrue();
        assertThat(reloadedCurrentPool.isActive()).isFalse();
        assertThat(reloadedTargetPool.isActive()).isTrue();
        assertThat(rewardDrawPoolRepository.findByActiveTrueAndDeletedAtIsNull())
                .get()
                .extracting(RewardDrawPool::getId)
                .isEqualTo(targetPool.getId());
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        CloudFrontSignedUrlService cloudFrontSignedUrlService() {
            return new CloudFrontSignedUrlService();
        }
    }
}
