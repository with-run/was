package kr.withrun.was.domain.reward.repository;

import jakarta.persistence.EntityManager;
import kr.withrun.was.domain.reward.entity.RewardDrawPool;
import kr.withrun.was.domain.reward.entity.RewardDrawPoolItem;
import kr.withrun.was.domain.reward.entity.RewardItem;
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
        "spring.datasource.url=jdbc:h2:mem:reward-draw-pool-item-repository;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({JpaAuditingConfig.class, QuerydslConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("리워드 draw pool item 리포지토리")
class RewardDrawPoolItemRepositoryTest {

    @Autowired
    private RewardDrawPoolItemRepository rewardDrawPoolItemRepository;

    @Autowired
    private RewardDrawPoolRepository rewardDrawPoolRepository;

    @Autowired
    private RewardItemRepository rewardItemRepository;

    @Autowired
    private EntityManager entityManager;

    @DisplayName("pool item 목록 조회는 특정 pool의 미삭제 item만 반환한다")
    @Test
    void findsPoolItemsByPoolIdExcludingDeletedRows() {
        RewardDrawPool targetPool = rewardDrawPoolRepository.saveAndFlush(RewardDrawPool.create("Target Pool", 3, 40));
        RewardDrawPool otherPool = rewardDrawPoolRepository.saveAndFlush(RewardDrawPool.create("Other Pool", 3, 40));
        RewardItem firstRewardItem = rewardItemRepository.saveAndFlush(rewardItem("First Reward", "reward-item/1.png", true));
        RewardItem secondRewardItem = rewardItemRepository.saveAndFlush(rewardItem("Second Reward", "reward-item/2.png", true));
        RewardItem thirdRewardItem = rewardItemRepository.saveAndFlush(rewardItem("Third Reward", "reward-item/3.png", true));

        RewardDrawPoolItem keptItem = rewardDrawPoolItemRepository.saveAndFlush(
                RewardDrawPoolItem.create(targetPool, firstRewardItem, 60)
        );
        RewardDrawPoolItem deletedItem = rewardDrawPoolItemRepository.saveAndFlush(
                RewardDrawPoolItem.create(targetPool, secondRewardItem, 40)
        );
        deletedItem.delete();
        rewardDrawPoolItemRepository.saveAndFlush(RewardDrawPoolItem.create(otherPool, thirdRewardItem, 100));

        entityManager.flush();
        entityManager.clear();

        List<RewardDrawPoolItem> items = rewardDrawPoolItemRepository
                .findAllByRewardDrawPoolIdAndDeletedAtIsNullOrderByIdAsc(targetPool.getId());

        assertThat(items).hasSize(1);
        assertThat(items.get(0).getId()).isEqualTo(keptItem.getId());
        assertThat(items.get(0).getWeight()).isEqualTo(60);
        assertThat(items.get(0).getRewardItem().getTitle()).isEqualTo("First Reward");
    }

    private RewardItem rewardItem(String title, String imageUrl, boolean isActive) {
        RewardItem rewardItem = instantiate(RewardItem.class);
        setField(rewardItem, "title", title);
        setField(rewardItem, "imageUrl", imageUrl);
        setField(rewardItem, "active", isActive);
        return rewardItem;
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
