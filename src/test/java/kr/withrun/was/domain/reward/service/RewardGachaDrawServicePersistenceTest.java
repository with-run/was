package kr.withrun.was.domain.reward.service;

import jakarta.persistence.EntityManager;
import kr.withrun.was.domain.file.service.CloudFrontSignedUrlService;
import kr.withrun.was.domain.reward.dto.RewardGachaDrawResponse;
import kr.withrun.was.domain.reward.entity.RewardDrawPool;
import kr.withrun.was.domain.reward.entity.RewardGachaDraw;
import kr.withrun.was.domain.reward.entity.RewardPointBalance;
import kr.withrun.was.domain.reward.entity.RewardItem;
import kr.withrun.was.domain.reward.repository.RewardDrawPoolItemRepository;
import kr.withrun.was.domain.reward.repository.RewardDrawPoolRepository;
import kr.withrun.was.domain.reward.repository.RewardGachaDrawCardRepository;
import kr.withrun.was.domain.reward.repository.RewardGachaDrawRepository;
import kr.withrun.was.domain.reward.repository.RewardPointBalanceRepository;
import kr.withrun.was.domain.reward.repository.RewardPointHistoryRepository;
import kr.withrun.was.domain.reward.repository.RewardItemRepository;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.repository.UserRepository;
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
        "spring.datasource.url=jdbc:h2:mem:reward-gacha-draw-service;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({
        JpaAuditingConfig.class,
        QuerydslConfig.class,
        RewardGachaDrawService.class,
        RewardGachaDrawServicePersistenceTest.TestConfig.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("리워드 가챠 draw 서비스 영속성")
class RewardGachaDrawServicePersistenceTest {

    @Autowired
    private RewardGachaDrawService rewardGachaDrawService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RewardItemRepository rewardItemRepository;

    @Autowired
    private RewardDrawPoolRepository rewardDrawPoolRepository;

    @Autowired
    private RewardPointBalanceRepository rewardPointBalanceRepository;

    @Autowired
    private RewardGachaDrawRepository rewardGachaDrawRepository;

    @Autowired
    private RewardGachaDrawCardRepository rewardGachaDrawCardRepository;

    @Autowired
    private RewardPointHistoryRepository rewardPointHistoryRepository;

    @Autowired
    private RewardDrawPoolItemRepository rewardDrawPoolItemRepository;

    @Autowired
    private EntityManager entityManager;

    @DisplayName("MISS 카드 메타데이터는 포인트 차감 bulk update 이후에도 응답에 포함된다")
    @Test
    void drawReturnsMissCardMetadataAfterPointBalanceBulkUpdate() {
        User user = userRepository.saveAndFlush(User.createPendingSocialUser("draw-user"));

        RewardPointBalance rewardPointBalance = RewardPointBalance.create(user);
        entityManager.persist(rewardPointBalance);
        entityManager.flush();
        entityManager.createNativeQuery("update reward_point_balances set current_balance = 5 where user_id = :userId")
                .setParameter("userId", user.getId())
                .executeUpdate();

        RewardItem missRewardItem = rewardItemRepository.saveAndFlush(
                RewardItem.builder()
                        .title("Miss Reward")
                        .imageUrl("reward-item/miss.png")
                        .active(true)
                        .build()
        );

        RewardDrawPool rewardDrawPool = rewardDrawPoolRepository.saveAndFlush(
                RewardDrawPool.create("Launch Pool", 3, 100, missRewardItem)
        );
        rewardDrawPool.activate();

        entityManager.flush();
        entityManager.clear();

        RewardGachaDrawResponse response = rewardGachaDrawService.draw(user.getId());

        entityManager.flush();
        entityManager.clear();

        assertThat(response.rewardGachaDrawId()).isNotNull();
        assertThat(response.cards()).hasSize(3);
        assertThat(response.cards())
                .allSatisfy(card -> {
                    assertThat(card.cardType().name()).isEqualTo("MISS");
                    assertThat(card.rewardItemId()).isEqualTo(missRewardItem.getId());
                    assertThat(card.title()).isEqualTo("Miss Reward");
                    assertThat(card.imageUrl()).endsWith("/reward-item/miss.png");
                });
        assertThat(rewardGachaDrawRepository.findAll()).hasSize(1);
        assertThat(rewardGachaDrawCardRepository.findAll()).hasSize(3);
        assertThat(rewardPointHistoryRepository.findAll()).hasSize(1);
        assertThat(rewardPointBalanceRepository.findByIdAndDeletedAtIsNull(user.getId()))
                .get()
                .extracting(RewardPointBalance::getCurrentBalance)
                .isEqualTo(4);
        assertThat(rewardDrawPoolItemRepository.findAll()).isEmpty();
    }

    @DisplayName("활성 pool 설정이 3장을 넘겨도 draw 결과와 저장 레코드는 최대 3장으로 제한된다")
    @Test
    void drawCapsPersistedCardsPerDrawToThree() {
        User user = userRepository.saveAndFlush(User.createPendingSocialUser("capped-draw-user"));

        RewardPointBalance rewardPointBalance = RewardPointBalance.create(user);
        entityManager.persist(rewardPointBalance);
        entityManager.flush();
        entityManager.createNativeQuery("update reward_point_balances set current_balance = 5 where user_id = :userId")
                .setParameter("userId", user.getId())
                .executeUpdate();

        RewardItem missRewardItem = rewardItemRepository.saveAndFlush(
                RewardItem.builder()
                        .title("Miss Reward")
                        .imageUrl("reward-item/miss.png")
                        .active(true)
                        .build()
        );

        RewardDrawPool rewardDrawPool = rewardDrawPoolRepository.saveAndFlush(
                RewardDrawPool.create("Launch Pool", 5, 100, missRewardItem)
        );
        rewardDrawPool.activate();

        entityManager.flush();
        entityManager.clear();

        RewardGachaDrawResponse response = rewardGachaDrawService.draw(user.getId());

        entityManager.flush();
        entityManager.clear();

        RewardGachaDraw persistedDraw = rewardGachaDrawRepository.findAll().getFirst();

        assertThat(response.rewardGachaDrawId()).isNotNull();
        assertThat(response.cards()).hasSize(3);
        assertThat(persistedDraw.getCardsPerDraw()).isEqualTo(3);
        assertThat(rewardGachaDrawRepository.findAll()).hasSize(1);
        assertThat(rewardGachaDrawCardRepository.findAll()).hasSize(3);
        assertThat(rewardPointHistoryRepository.findAll()).hasSize(1);
        assertThat(rewardPointBalanceRepository.findByIdAndDeletedAtIsNull(user.getId()))
                .get()
                .extracting(RewardPointBalance::getCurrentBalance)
                .isEqualTo(4);
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        CloudFrontSignedUrlService cloudFrontSignedUrlService() {
            return new CloudFrontSignedUrlService();
        }

        @Bean
        RewardDrawRandomGenerator rewardDrawRandomGenerator() {
            return new RewardDrawRandomGenerator() {
                @Override
                public int nextInt(int bound) {
                    return 0;
                }
            };
        }
    }
}
