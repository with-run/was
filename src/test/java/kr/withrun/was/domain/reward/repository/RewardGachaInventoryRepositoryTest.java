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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.config.import=",
        "spring.cloud.aws.parameterstore.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:reward-gacha-inventory-repository;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({JpaAuditingConfig.class, QuerydslConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("리워드 인벤토리 리포지토리")
class RewardGachaInventoryRepositoryTest {

    @Autowired
    private RewardGachaDrawCardRepository rewardGachaDrawCardRepository;

    @Autowired
    private RewardGachaDrawRepository rewardGachaDrawRepository;

    @Autowired
    private EntityManager entityManager;

    @DisplayName("인벤토리 조회는 내 REWARD 카드만 최신 획득순으로 반환한다")
    @Test
    void findsOnlyRewardCardsForUserInLatestOrder() {
        User targetUser = persistUser("inv-runner-1");
        User otherUser = persistUser("inv-runner-2");
        RewardDrawPool rewardDrawPool = persistRewardDrawPool("Launch Pool", 3, 40);
        RewardItem rewardItemA = persistRewardItem("Reward A", "reward-item/1.png");
        RewardItem rewardItemB = persistRewardItem("Reward B", "reward-item/2.png");

        RewardGachaDraw olderDraw = rewardGachaDrawRepository.saveAndFlush(RewardGachaDraw.create(targetUser, rewardDrawPool, 3));
        RewardGachaDraw newerDraw = rewardGachaDrawRepository.saveAndFlush(RewardGachaDraw.create(targetUser, rewardDrawPool, 3));
        RewardGachaDraw otherUserDraw = rewardGachaDrawRepository.saveAndFlush(RewardGachaDraw.create(otherUser, rewardDrawPool, 3));
        setField(olderDraw, "createdAt", LocalDateTime.of(2026, 3, 30, 9, 0));
        setField(newerDraw, "createdAt", LocalDateTime.of(2026, 3, 30, 10, 0));
        setField(otherUserDraw, "createdAt", LocalDateTime.of(2026, 3, 30, 11, 0));

        RewardGachaDrawCard olderReward = rewardGachaDrawCardRepository.saveAndFlush(
                RewardGachaDrawCard.create(olderDraw, 1, RewardGachaCardType.REWARD, rewardItemA)
        );
        rewardGachaDrawCardRepository.saveAndFlush(
                RewardGachaDrawCard.create(newerDraw, 1, RewardGachaCardType.MISS, rewardItemA)
        );
        RewardGachaDrawCard newerReward = rewardGachaDrawCardRepository.saveAndFlush(
                RewardGachaDrawCard.create(newerDraw, 2, RewardGachaCardType.REWARD, rewardItemB)
        );
        RewardGachaDrawCard otherUserReward = rewardGachaDrawCardRepository.saveAndFlush(
                RewardGachaDrawCard.create(otherUserDraw, 1, RewardGachaCardType.REWARD, rewardItemA)
        );
        otherUserReward.delete();

        entityManager.flush();
        entityManager.clear();

        Page<RewardGachaDrawCard> inventory = rewardGachaDrawCardRepository.findInventoryByUserId(
                userId(targetUser),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt", "id"))
        );

        assertThat(inventory.getContent()).hasSize(2);
        assertThat(inventory.getContent()).extracting(RewardGachaDrawCard::getId)
                .containsExactly(newerReward.getId(), olderReward.getId());
        assertThat(inventory.getContent()).extracting(card -> card.getRewardItem().getTitle())
                .containsExactly("Reward B", "Reward A");
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
