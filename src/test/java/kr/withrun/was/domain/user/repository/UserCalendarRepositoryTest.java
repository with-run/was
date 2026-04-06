package kr.withrun.was.domain.user.repository;

import jakarta.persistence.EntityManager;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.entity.UserCalendar;
import kr.withrun.was.domain.user.repository.query.UserCalendarCustomRepository;
import kr.withrun.was.domain.user.repository.query.dto.UserCalendarDailySummaryRow;
import kr.withrun.was.domain.user.repository.query.dto.UserCalendarSummaryRow;
import kr.withrun.was.domain.user.type.Gender;
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

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.config.import=",
        "spring.cloud.aws.parameterstore.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:user-calendar-repository;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({JpaAuditingConfig.class, QuerydslConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("사용자 캘린더 리포지토리")
class UserCalendarRepositoryTest {

    @Autowired
    private UserCalendarRepository userCalendarRepository;

    @Autowired
    private EntityManager entityManager;

    @DisplayName("전체 기간 누적 요약을 조회한다")
    @Test
    void findsLifetimeSummary() {
        assertThat(userCalendarRepository).isInstanceOf(UserCalendarCustomRepository.class);

        User user = persistUser("summary-runner");
        persistCalendar(user, LocalDate.of(2026, 3, 10), 5100, 1, 0, 1, false);
        persistCalendar(user, LocalDate.of(2026, 3, 11), 3200, 0, 2, 0, false);
        persistCalendar(user, LocalDate.of(2026, 3, 12), 0, 0, 0, 0, false);

        User deletedUser = persistUser("deleted-runner");
        deletedUser.delete();
        persistCalendar(deletedUser, LocalDate.of(2026, 3, 13), 9999, 9, 9, 9, false);

        UserCalendar deletedCalendar = persistCalendar(user, LocalDate.of(2026, 3, 14), 9999, 9, 9, 9, false);
        deletedCalendar.delete();

        entityManager.flush();
        entityManager.clear();

        UserCalendarSummaryRow summary = userCalendarRepository.findSummaryRow(user.getId());

        assertThat(summary.lifetimeRunCount()).isEqualTo(4);
        assertThat(summary.lifetimeDistanceM()).isEqualTo(8300);
        assertThat(summary.lifetimeCourseRunCount()).isEqualTo(1);
        assertThat(summary.lifetimeFreeRunCount()).isEqualTo(2);
        assertThat(summary.lifetimeGhostRunCount()).isEqualTo(1);
    }

    @DisplayName("스트릭 계산용 러닝 날짜를 최신순으로 조회하고 비러닝일과 삭제 데이터는 제외한다")
    @Test
    void findsRunningDatesDescendingForStreakCalculation() {
        User user = persistUser("streak-runner");
        persistCalendar(user, LocalDate.of(2026, 3, 9), 4000, 1, 0, 0, false);
        persistCalendar(user, LocalDate.of(2026, 3, 10), 4100, 0, 1, 0, false);
        persistCalendar(user, LocalDate.of(2026, 3, 11), 0, 0, 0, 0, false);
        UserCalendar deletedCalendar = persistCalendar(user, LocalDate.of(2026, 3, 12), 4200, 0, 0, 1, false);
        deletedCalendar.delete();
        persistCalendar(user, LocalDate.of(2026, 3, 13), 4300, 0, 0, 1, false);

        entityManager.flush();
        entityManager.clear();

        List<LocalDate> runningDates = userCalendarRepository.findRunningDates(user.getId());

        assertThat(runningDates).containsExactly(
                LocalDate.of(2026, 3, 13),
                LocalDate.of(2026, 3, 10),
                LocalDate.of(2026, 3, 9)
        );
    }

    @DisplayName("특정 월의 날짜별 러닝 요약을 오래된 날짜 순으로 조회한다")
    @Test
    void findsDailySummariesForMonth() {
        User user = persistUser("monthly-runner");
        persistCalendar(user, LocalDate.of(2026, 3, 8), 12000, 1, 1, 1, false);
        persistCalendar(user, LocalDate.of(2026, 3, 15), 8000, 0, 2, 0, false);
        persistCalendar(user, LocalDate.of(2026, 2, 28), 5000, 1, 0, 0, false);

        User anotherUser = persistUser("another-runner");
        persistCalendar(anotherUser, LocalDate.of(2026, 3, 8), 9999, 9, 9, 9, false);

        UserCalendar deletedCalendar = persistCalendar(user, LocalDate.of(2026, 3, 20), 7000, 1, 0, 0, false);
        deletedCalendar.delete();

        entityManager.flush();
        entityManager.clear();

        List<UserCalendarDailySummaryRow> summaries = userCalendarRepository.findDailySummaries(
                user.getId(),
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31)
        );

        assertThat(summaries).containsExactly(
                new UserCalendarDailySummaryRow(LocalDate.of(2026, 3, 8), 12000, 1800, 350, 1, 1, 1),
                new UserCalendarDailySummaryRow(LocalDate.of(2026, 3, 15), 8000, 1800, 350, 0, 2, 0)
        );
    }

    @DisplayName("사용자와 날짜로 활성 캘린더를 Querydsl 커스텀 메서드에서 조회한다")
    @Test
    void findsActiveCalendarByUserIdAndDateFromCustomRepository() throws NoSuchMethodException {
        User user = persistUser("calendar-runner");
        UserCalendar calendar = persistCalendar(user, LocalDate.of(2026, 3, 18), 6500, 0, 1, 0, false);
        persistCalendar(persistUser("other-runner-2"), LocalDate.of(2026, 3, 18), 7200, 1, 0, 0, false);

        entityManager.flush();
        entityManager.clear();

        assertThat(UserCalendarCustomRepository.class.getMethod(
                "findByUserIdAndCalendarDateAndDeletedAtIsNull",
                Long.class,
                LocalDate.class
        )).isNotNull();
        assertThat(userCalendarRepository.findByUserIdAndCalendarDateAndDeletedAtIsNull(
                user.getId(),
                LocalDate.of(2026, 3, 18)
        )).isPresent()
                .get()
                .extracting(UserCalendar::getId)
                .isEqualTo(calendar.getId());
    }

    private User persistUser(String nickname) {
        User user = instantiate(User.class);
        setField(user, "nickname", nickname);
        setField(user, "birthDate", LocalDate.of(1995, 3, 11));
        setField(user, "gender", Gender.MALE);
        setField(user, "height", 175.0);
        setField(user, "weight", 68.0);
        entityManager.persist(user);
        return user;
    }

    private UserCalendar persistCalendar(
            User user,
            LocalDate calendarDate,
            int totalDistanceM,
            int courseRunCount,
            int freeRunCount,
            int ghostRunCount,
            boolean deleted
    ) {
        UserCalendar userCalendar = instantiate(UserCalendar.class);
        setField(userCalendar, "user", user);
        setField(userCalendar, "calendarDate", calendarDate);
        setField(userCalendar, "totalDistanceM", totalDistanceM);
        setField(userCalendar, "courseRunCount", courseRunCount);
        setField(userCalendar, "freeRunCount", freeRunCount);
        setField(userCalendar, "ghostRunCount", ghostRunCount);
        setField(userCalendar, "totalDurationSec", 1800);
        setField(userCalendar, "totalCaloriesKcal", 350);
        if (deleted) {
            userCalendar.delete();
        }
        entityManager.persist(userCalendar);
        return userCalendar;
    }

    private <T> T instantiate(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
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
