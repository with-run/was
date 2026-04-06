package kr.withrun.was.domain.user.repository;

import jakarta.persistence.EntityManager;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.entity.UserRunningPreference;
import kr.withrun.was.domain.user.type.Gender;
import kr.withrun.was.domain.user.type.Purpose;
import kr.withrun.was.global.common.type.Difficulty;
import kr.withrun.was.global.common.type.TimeSlot;
import kr.withrun.was.global.config.JpaAuditingConfig;
import kr.withrun.was.global.config.QuerydslConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;

@DataJpaTest(properties = {
        "spring.config.import=",
        "spring.cloud.aws.parameterstore.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:user-running-preference;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({JpaAuditingConfig.class, QuerydslConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("러닝 선호도 리포지토리")
class UserRunningPreferenceRepositoryTest {

    @Autowired
    private UserRunningPreferenceRepository userRunningPreferenceRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("기존 러닝 선호도를 다시 저장할 때 기존 다중 선택 항목을 교체할 수 있다")
    void updatesExistingRunningPreferenceWithReplacedSelections() {
        User user = persistUser("runner-pref");

        UserRunningPreference preference = UserRunningPreference.create(
                user,
                Difficulty.EASY,
                5.0
        );
        preference.replacePurposes(List.of(Purpose.DIET));
        preference.replaceTimeSlots(List.of(TimeSlot.MORNING));
        preference.replaceCourseTypes(List.of(CourseType.PARK));
        user.assignRunningPreference(preference);

        userRunningPreferenceRepository.saveAndFlush(preference);
        entityManager.clear();

        UserRunningPreference persistedPreference = userRunningPreferenceRepository.findById(user.getId())
                .orElseThrow();

        persistedPreference.updatePreferredSettings(Difficulty.HARD, 10.0);
        // 실제 수정 화면처럼 기존 값 일부를 유지한 채 다른 항목만 추가/변경하는 케이스를 재현한다.
        persistedPreference.replacePurposes(List.of(Purpose.DIET, Purpose.RACE_PREPARATION));
        persistedPreference.replaceTimeSlots(List.of(TimeSlot.MORNING, TimeSlot.EVENING));
        persistedPreference.replaceCourseTypes(List.of(CourseType.PARK, CourseType.TRACK));

        assertThatCode(() -> userRunningPreferenceRepository.saveAndFlush(persistedPreference))
                .doesNotThrowAnyException();
    }

    private User persistUser(String nickname) {
        User user = instantiateUser();
        setField(user, "nickname", nickname);
        setField(user, "birthDate", LocalDate.of(1994, 2, 18));
        setField(user, "gender", Gender.MALE);
        setField(user, "height", 176.5);
        setField(user, "weight", 68.3);
        entityManager.persist(user);
        entityManager.flush();
        return user;
    }

    private User instantiateUser() {
        try {
            Constructor<User> constructor = User.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to instantiate user", exception);
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
