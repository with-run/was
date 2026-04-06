package kr.withrun.was.domain.running.repository;

import jakarta.persistence.EntityManager;
import kr.withrun.was.domain.running.entity.GhostRunningResult;
import kr.withrun.was.domain.running.entity.RunningSession;
import kr.withrun.was.domain.running.repository.query.RunningSessionCustomRepository;
import kr.withrun.was.domain.running.repository.query.dto.PastRunningSessionHistoryRow;
import kr.withrun.was.domain.running.type.GhostResultStatus;
import kr.withrun.was.domain.running.type.RunningMode;
import kr.withrun.was.domain.running.type.RunningSessionCompleteState;
import kr.withrun.was.domain.user.entity.User;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.config.import=",
        "spring.cloud.aws.parameterstore.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:running-session-history;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({JpaAuditingConfig.class, QuerydslConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("러닝 세션 리포지토리")
class RunningSessionRepositoryTest {

    @Autowired
    private RunningSessionRepository runningSessionRepository;

    @Autowired
    private EntityManager entityManager;

    @DisplayName("과거 러닝 row를 startedAt, runningSessionId 역순으로 조회하고 고스트 결과를 포함한다")
    @Test
    void findsPastRunningSessionHistoryRowsOrderedWithGhostResult() {
        assertThat(runningSessionRepository).isInstanceOf(RunningSessionCustomRepository.class);

        User user = persistUser("runner-history");
        RunningSession olderSession = persistRunningSession(user, LocalDateTime.of(2026, 3, 15, 19, 30), RunningMode.FREE, true);
        RunningSession newerGhostSession = persistRunningSession(user, LocalDateTime.of(2026, 3, 16, 6, 10), RunningMode.GHOST, true);
        persistGhostRunningResult(newerGhostSession, olderSession, GhostResultStatus.WIN);

        entityManager.flush();
        entityManager.clear();

        List<PastRunningSessionHistoryRow> rows = runningSessionRepository.findPastRunningSessionHistoryRows(
                user.getId(),
                null,
                null,
                null,
                null,
                10
        );

        assertThat(rows).hasSize(2);
        assertThat(rows)
                .extracting(PastRunningSessionHistoryRow::runningSessionId)
                .containsExactly(newerGhostSession.getId(), olderSession.getId());
        assertThat(rows)
                .extracting(PastRunningSessionHistoryRow::ghostResultStatus)
                .containsExactly(GhostResultStatus.WIN, null);
    }

    @DisplayName("연월일 범위와 커서를 함께 적용해 pageSize + 1건까지만 조회한다")
    @Test
    void filtersPastRunningSessionHistoryRowsByRangeAndCursor() {
        User user = persistUser("runner-range");
        persistRunningSession(user, LocalDateTime.of(2026, 3, 16, 20, 0), RunningMode.FREE, true);
        RunningSession cursorSession = persistRunningSession(user, LocalDateTime.of(2026, 3, 16, 18, 0), RunningMode.COURSE, false);
        RunningSession olderSameDaySession = persistRunningSession(user, LocalDateTime.of(2026, 3, 16, 7, 0), RunningMode.FREE, true);
        persistRunningSession(user, LocalDateTime.of(2026, 3, 15, 23, 59), RunningMode.GHOST, true);

        entityManager.flush();
        entityManager.clear();

        List<PastRunningSessionHistoryRow> rows = runningSessionRepository.findPastRunningSessionHistoryRows(
                user.getId(),
                LocalDateTime.of(2026, 3, 16, 0, 0),
                LocalDateTime.of(2026, 3, 17, 0, 0),
                cursorSession.getStartedAt(),
                cursorSession.getId(),
                1
        );

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().runningSessionId()).isEqualTo(olderSameDaySession.getId());
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

    private RunningSession persistRunningSession(
            User user,
            LocalDateTime startedAt,
            RunningMode runningMode,
            boolean completedSuccessfully
    ) {
        RunningSession runningSession = instantiate(RunningSession.class);
        setField(runningSession, "user", user);
        setField(runningSession, "startedAt", startedAt);
        setField(runningSession, "distanceM", 7200);
        setField(runningSession, "durationSec", 2100);
        setField(runningSession, "caloriesKcal", 430);
        setField(runningSession, "elevationGainM", 42);
        setField(runningSession, "snapshotImageUrl", "https://cdn.withrun.app/snapshots/" + startedAt.getHour() + ".png");
        setField(runningSession, "startLatitude", 37.5);
        setField(runningSession, "startLongitude", 127.0);
        setField(runningSession, "isPublic", runningMode == RunningMode.GHOST);
        setField(
                runningSession,
                "completeState",
                completedSuccessfully ? RunningSessionCompleteState.SUCCESS : null
        );
        setField(runningSession, "mode", runningMode);
        entityManager.persist(runningSession);
        return runningSession;
    }

    private void persistGhostRunningResult(
            RunningSession runningSession,
            RunningSession ghostTargetRunningSession,
            GhostResultStatus resultStatus
    ) {
        GhostRunningResult ghostRunningResult = instantiate(GhostRunningResult.class);
        setField(ghostRunningResult, "resultStatus", resultStatus);
        setField(ghostRunningResult, "timeGapSec", 15);
        setField(ghostRunningResult, "distanceGapM", 20);
        setField(ghostRunningResult, "runningSession", runningSession);
        setField(ghostRunningResult, "ghostTargetRunningSession", ghostTargetRunningSession);
        setField(ghostRunningResult, "targetUser", ghostTargetRunningSession.getUser());
        entityManager.persist(ghostRunningResult);
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
