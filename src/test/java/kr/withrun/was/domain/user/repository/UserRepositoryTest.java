package kr.withrun.was.domain.user.repository;

import jakarta.persistence.EntityManager;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.repository.query.UserQueryRepository;
import kr.withrun.was.domain.user.type.Gender;
import kr.withrun.was.global.config.JpaAuditingConfig;
import kr.withrun.was.global.config.QuerydslConfig;
import org.hibernate.annotations.SQLDelete;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.config.import=",
        "spring.cloud.aws.parameterstore.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:user-repository;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({JpaAuditingConfig.class, QuerydslConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("사용자 리포지토리")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @DisplayName("사용자 엔티티에 명시적인 SQLDelete가 선언되어 있다")
    @Test
    void declaresExplicitSqlDeleteOnUser() {
        SQLDelete sqlDelete = User.class.getAnnotation(SQLDelete.class);

        assertThat(sqlDelete).isNotNull();
        assertThat(sqlDelete.sql()).contains("users");
        assertThat(sqlDelete.sql()).contains("user_id");
    }

    @DisplayName("사용자 엔티티는 isActive 필드를 사용하지 않는다")
    @Test
    void doesNotDeclareIsActiveField() {
        assertThat(hasDeclaredField(User.class, "isActive")).isFalse();
    }

    @DisplayName("활성 사용자만 조회한다")
    @Test
    void findsActiveUser() {
        assertThat(userRepository).isInstanceOf(UserQueryRepository.class);

        User activeUser = persistUser("runner-active");
        entityManager.flush();
        entityManager.clear();

        User foundUser = userRepository.findNotDeletedUser(activeUser.getId())
                .orElseThrow();

        assertThat(foundUser.getId()).isEqualTo(activeUser.getId());
    }

    @DisplayName("삭제된 사용자는 조회하지 않는다")
    @Test
    void doesNotFindDeletedUser() {
        User deletedUser = persistUser("runner-deleted");
        deletedUser.delete();
        entityManager.flush();
        entityManager.clear();

        assertThat(userRepository.findNotDeletedUser(deletedUser.getId())).isEmpty();
    }

    private User persistUser(String nickname) {
        User user = instantiateUser();
        setField(user, "nickname", nickname);
        setField(user, "birthDate", LocalDate.of(1995, 3, 11));
        setField(user, "gender", Gender.MALE);
        setField(user, "height", 175.0);
        setField(user, "weight", 68.0);
        entityManager.persist(user);
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

    private boolean hasDeclaredField(Class<?> type, String fieldName) {
        try {
            type.getDeclaredField(fieldName);
            return true;
        } catch (NoSuchFieldException exception) {
            return false;
        }
    }
}
