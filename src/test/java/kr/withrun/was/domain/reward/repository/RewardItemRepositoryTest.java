package kr.withrun.was.domain.reward.repository;

import jakarta.persistence.EntityManager;
import kr.withrun.was.domain.reward.entity.RewardItem;
import kr.withrun.was.global.config.JpaAuditingConfig;
import kr.withrun.was.global.config.QuerydslConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.config.import=",
        "spring.cloud.aws.parameterstore.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:reward-item-repository;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({JpaAuditingConfig.class, QuerydslConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("리워드 아이템 리포지토리")
class RewardItemRepositoryTest {

    @Autowired
    private RewardItemRepository rewardItemRepository;

    @Autowired
    private EntityManager entityManager;

    @DisplayName("활성 아이템만 필터링해 조회할 수 있다")
    @Test
    void findsActiveRewardItemsOnly() {
        persistRewardItem("Active Reward", "reward-item/1.png", true, false);
        persistRewardItem("Inactive Reward", "reward-item/2.png", false, false);
        entityManager.flush();
        entityManager.clear();

        var page = rewardItemRepository.findAllByDeletedAtIsNullAndActive(
                true,
                PageRequest.of(0, 20)
        );

        assertThat(page.getContent())
                .extracting(RewardItem::getTitle)
                .containsExactly("Active Reward");
    }

    @DisplayName("soft delete 된 아이템은 기본 조회에서 제외된다")
    @Test
    void excludesSoftDeletedRewardItemFromDefaultQueries() {
        RewardItem rewardItem = persistRewardItem("Deleted Reward", "reward-item/3.png", true, true);
        entityManager.flush();
        entityManager.clear();

        var page = rewardItemRepository.findAllByDeletedAtIsNull(PageRequest.of(0, 20));

        assertThat(page.getContent()).isEmpty();
        assertThat(rewardItemRepository.findByIdAndDeletedAtIsNull(rewardItem.getId())).isEmpty();
    }

    @DisplayName("생성 시 isActive=true 기본값이 적용된다")
    @Test
    void appliesActiveDefaultWhenRewardItemIsCreated() {
        RewardItem rewardItem = instantiateRewardItemWithDefaultActive(
                "Default Active Reward",
                "reward-item/4.png"
        );
        rewardItemRepository.save(rewardItem);

        assertThat(rewardItem.isActive()).isTrue();
    }

    @DisplayName("showcase 조회는 활성 + 미삭제 reward item 을 createdAt/id 내림차순으로 반환한다")
    @Test
    void findsShowcaseRewardItemsInStableOrder() {
        RewardItem older = persistRewardItem("Older Reward", "reward-item/11.png", true, false);
        RewardItem newest = persistRewardItem("Newest Reward", "reward-item/10.png", true, false);
        RewardItem activeStandalone = persistRewardItem("Active Standalone Reward", "reward-item/14.png", true, false);
        persistRewardItem("Inactive Reward", "reward-item/12.png", false, false);
        persistRewardItem("Deleted Reward", "reward-item/13.png", true, true);

        setField(older, "createdAt", LocalDateTime.of(2026, 3, 29, 12, 0));
        setField(newest, "createdAt", LocalDateTime.of(2026, 3, 29, 12, 0));
        setField(activeStandalone, "createdAt", LocalDateTime.of(2026, 3, 29, 12, 1));

        entityManager.flush();
        entityManager.clear();

        List<RewardItem> rewardItems = rewardItemRepository.findAllByDeletedAtIsNullAndActiveTrueOrderByCreatedAtDescIdDesc();

        assertThat(rewardItems)
                .extracting(RewardItem::getTitle)
                .containsExactly("Active Standalone Reward", "Newest Reward", "Older Reward");
    }

    private RewardItem persistRewardItem(String title, String imageUrl, boolean isActive, boolean deleted) {
        RewardItem rewardItem = instantiate(RewardItem.class);
        setField(rewardItem, "title", title);
        setField(rewardItem, "imageUrl", imageUrl);
        setField(rewardItem, "active", isActive);
        entityManager.persist(rewardItem);

        if (deleted) {
            rewardItem.delete();
        }

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

    private RewardItem instantiateRewardItemWithDefaultActive(String title, String imageUrl) {
        try {
            var constructor = RewardItem.class.getDeclaredConstructor(String.class, String.class, Boolean.class);
            constructor.setAccessible(true);
            return constructor.newInstance(title, imageUrl, null);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to instantiate RewardItem with default active", exception);
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
