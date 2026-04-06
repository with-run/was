package kr.withrun.was.domain.reward.repository;

import jakarta.persistence.EntityManager;
import kr.withrun.was.domain.reward.entity.RewardDrawPool;
import kr.withrun.was.domain.reward.entity.RewardGachaDraw;
import kr.withrun.was.domain.reward.entity.RewardGachaDrawCard;
import kr.withrun.was.domain.reward.entity.RewardItem;
import kr.withrun.was.domain.reward.type.RewardGachaCardType;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.global.config.JpaAuditingConfig;
import kr.withrun.was.global.config.QuerydslConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.config.import=",
        "spring.cloud.aws.parameterstore.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:reward-gacha-draw-repository;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({JpaAuditingConfig.class, QuerydslConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("리워드 가챠 draw 리포지토리")
class RewardGachaDrawRepositoryTest {

    @Autowired
    private RewardGachaDrawRepository rewardGachaDrawRepository;

    @Autowired
    private RewardGachaDrawCardRepository rewardGachaDrawCardRepository;

    @Autowired
    private EntityManager entityManager;

    @DisplayName("한 번의 draw 결과는 헤더와 카드 행으로 저장된다")
    @Test
    void storesRewardGachaDrawHeaderWithCards() {
        User user = persistUser("runner-gacha");
        RewardDrawPool rewardDrawPool = persistRewardDrawPool("Launch Pool", 3, 40);
        RewardItem rewardItem = persistRewardItem("Reward A", "reward-item/1.png");

        RewardGachaDraw rewardGachaDraw = rewardGachaDrawRepository.saveAndFlush(
                RewardGachaDraw.create(user, rewardDrawPool, 3)
        );
        rewardGachaDrawCardRepository.saveAllAndFlush(List.of(
                RewardGachaDrawCard.create(rewardGachaDraw, 1, RewardGachaCardType.REWARD, rewardItem),
                RewardGachaDrawCard.create(rewardGachaDraw, 2, RewardGachaCardType.MISS, null),
                RewardGachaDrawCard.create(rewardGachaDraw, 3, RewardGachaCardType.REWARD, rewardItem)
        ));
        entityManager.clear();

        RewardGachaDraw foundDraw = rewardGachaDrawRepository.findById(rewardGachaDraw.getId()).orElseThrow();
        List<RewardGachaDrawCard> cards = rewardGachaDrawCardRepository
                .findAllByRewardGachaDrawIdAndDeletedAtIsNullOrderByCardIndexAsc(foundDraw.getId());

        assertThat(userId(foundDraw.getUser())).isEqualTo(userId(user));
        assertThat(foundDraw.getRewardDrawPool().getId()).isEqualTo(rewardDrawPool.getId());
        assertThat(foundDraw.getCardsPerDraw()).isEqualTo(3);
        assertThat(cards).hasSize(3);
        assertThat(cards).extracting(RewardGachaDrawCard::getCardType)
                .containsExactly(RewardGachaCardType.REWARD, RewardGachaCardType.MISS, RewardGachaCardType.REWARD);
        assertThat(cards.get(0).getRewardItem().getId()).isEqualTo(rewardItem.getId());
        assertThat(cards.get(1).getRewardItem()).isNull();
    }

    private User persistUser(String nickname) {
        User user = User.createPendingSocialUser(nickname);
        entityManager.persist(user);
        entityManager.flush();
        return user;
    }

    private RewardDrawPool persistRewardDrawPool(String name, int cardsPerDraw, int missWeight) {
        RewardDrawPool rewardDrawPool = RewardDrawPool.create(name, cardsPerDraw, missWeight);
        entityManager.persist(rewardDrawPool);
        entityManager.flush();
        return rewardDrawPool;
    }

    private RewardItem persistRewardItem(String title, String imageUrl) {
        RewardItem rewardItem = instantiate(RewardItem.class);
        setField(rewardItem, "title", title);
        setField(rewardItem, "imageUrl", imageUrl);
        setField(rewardItem, "active", true);
        entityManager.persist(rewardItem);
        entityManager.flush();
        return rewardItem;
    }

    private Long userId(User user) {
        return (Long) entityManager.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(user);
    }

    private <T> T instantiate(Class<T> type) {
        try {
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to instantiate " + type.getSimpleName(), exception);
        }
    }

    private void setField(Object target, String fieldName, Object value) {
        Class<?> currentClass = target.getClass();
        while (currentClass != null) {
            try {
                Field field = currentClass.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException exception) {
                currentClass = currentClass.getSuperclass();
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException("Failed to set field " + fieldName, exception);
            }
        }

        throw new IllegalArgumentException("Field not found: " + fieldName);
    }
}
