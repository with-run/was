package kr.withrun.was.domain.reward.service;

import kr.withrun.was.domain.reward.dto.RewardPointBalanceResponse;
import kr.withrun.was.domain.reward.dto.RewardPointHistoryPageResponse;
import kr.withrun.was.domain.reward.entity.RewardPointBalance;
import kr.withrun.was.domain.reward.entity.RewardPointHistory;
import kr.withrun.was.domain.reward.repository.RewardPointBalanceRepository;
import kr.withrun.was.domain.reward.repository.RewardPointHistoryRepository;
import kr.withrun.was.domain.reward.type.RewardPointReason;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("리워드 포인트 조회 서비스")
class RewardPointQueryServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RewardPointBalanceRepository rewardPointBalanceRepository;

    @Mock
    private RewardPointHistoryRepository rewardPointHistoryRepository;

    private RewardPointQueryService rewardPointQueryService;

    @BeforeEach
    void setUp() {
        rewardPointQueryService = new RewardPointQueryService(
                userRepository,
                rewardPointBalanceRepository,
                rewardPointHistoryRepository
        );
    }

    @DisplayName("잔액 행이 없어도 현재 잔액 0을 반환한다")
    @Test
    void returnsZeroBalanceWhenBalanceRowDoesNotExist() {
        User user = User.createPendingSocialUser("runner-query");
        setField(user, "id", 1L);
        when(userRepository.findNotDeletedUser(1L)).thenReturn(Optional.of(user));
        when(rewardPointBalanceRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        RewardPointBalanceResponse response = rewardPointQueryService.getRewardPointBalance(1L);

        assertThat(response.currentBalance()).isZero();
    }

    @DisplayName("지급 이력을 페이지 응답으로 변환한다")
    @Test
    void returnsPagedRewardPointHistories() {
        User user = User.createPendingSocialUser("runner-history-query");
        setField(user, "id", 2L);
        when(userRepository.findNotDeletedUser(2L)).thenReturn(Optional.of(user));
        RewardPointHistory rewardPointHistory = RewardPointHistory.create(
                user,
                1,
                RewardPointReason.GHOST_WIN,
                "reward:ghost-win:2:88"
        );
        setField(rewardPointHistory, "id", 10L);
        setField(rewardPointHistory, "createdAt", LocalDateTime.of(2026, 3, 29, 15, 0));
        when(rewardPointHistoryRepository.findAllByUserIdAndDeletedAtIsNull(
                2L,
                PageRequest.of(0, 20, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt", "id"))
        )).thenReturn(new PageImpl<>(List.of(rewardPointHistory), PageRequest.of(0, 20), 1));

        RewardPointHistoryPageResponse response = rewardPointQueryService.getRewardPointHistories(2L, 0, 20);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).rewardPointHistoryId()).isEqualTo(10L);
        assertThat(response.items().get(0).reason()).isEqualTo(RewardPointReason.GHOST_WIN);
        assertThat(response.hasNext()).isFalse();
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
