package kr.withrun.was.domain.user.service;

import kr.withrun.was.domain.user.dto.UpdateUserProfileRequest;
import kr.withrun.was.domain.user.dto.NicknameAvailabilityResponse;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.repository.UserRepository;
import kr.withrun.was.domain.user.type.Gender;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserProfileService")
class UserProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserProfileService userProfileService;

    @Test
    @DisplayName("pending 사용자는 프로필 저장 후 profileCompleted=true가 된다")
    void completesPendingUserProfile() {
        // 아직 온보딩이 끝나지 않은 소셜 로그인 사용자를 가정합니다.
        User user = User.createPendingSocialUser();
        setField(user, "id", 1L);
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "runner",
                LocalDate.of(1998, 4, 5),
                Gender.FEMALE,
                165.5,
                52.3
        );

        given(userRepository.findNotDeletedUser(1L)).willReturn(Optional.of(user));
        given(userRepository.existsByNicknameAndIdNot("runner", 1L)).willReturn(false);

        userProfileService.updateProfile(1L, request);

        assertThat(user.isProfileCompleted()).isTrue();
        assertThat(user.getNickname()).isEqualTo("runner");
        assertThat(user.getBirthDate()).isEqualTo(LocalDate.of(1998, 4, 5));
        assertThat(user.getGender()).isEqualTo(Gender.FEMALE);
        assertThat(user.getHeight()).isEqualTo(165.5);
        assertThat(user.getWeight()).isEqualTo(52.3);
    }

    @Test
    @DisplayName("기존 완료 사용자는 같은 API로 프로필을 다시 수정할 수 있다")
    void updatesCompletedUserProfile() {
        // 이 API는 온보딩 전용이 아니라 추후 "내 프로필 수정"에도 재사용할 수 있어야 합니다.
        User user = completedUser(2L, "before");
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "after",
                LocalDate.of(2000, 1, 1),
                Gender.MALE,
                181.2,
                73.0
        );

        given(userRepository.findNotDeletedUser(2L)).willReturn(Optional.of(user));
        given(userRepository.existsByNicknameAndIdNot("after", 2L)).willReturn(false);

        userProfileService.updateProfile(2L, request);

        assertThat(user.isProfileCompleted()).isTrue();
        assertThat(user.getNickname()).isEqualTo("after");
        assertThat(user.getBirthDate()).isEqualTo(LocalDate.of(2000, 1, 1));
        assertThat(user.getHeight()).isEqualTo(181.2);
        assertThat(user.getWeight()).isEqualTo(73.0);
    }

    @Test
    @DisplayName("다른 사용자가 이미 쓰는 닉네임이면 충돌 예외를 반환한다")
    void throwsWhenNicknameAlreadyExists() {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "runner",
                LocalDate.of(1998, 4, 5),
                Gender.FEMALE,
                165.5,
                52.3
        );

        given(userRepository.findNotDeletedUser(1L)).willReturn(Optional.of(User.createPendingSocialUser()));
        given(userRepository.existsByNicknameAndIdNot("runner", 1L)).willReturn(true);

        assertThatThrownBy(() -> userProfileService.updateProfile(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getResponseCode())
                .isEqualTo(ResponseCode.NICKNAME_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("다른 활성 사용자가 닉네임을 사용 중이면 사용 불가를 반환한다")
    void returnsUnavailableWhenNicknameAlreadyExists() {
        given(userRepository.existsActiveUserByNickname("runner-next")).willReturn(true);

        NicknameAvailabilityResponse response = userProfileService.getNicknameAvailability("  runner-next  ");

        assertThat(response.available()).isFalse();
    }

    @Test
    @DisplayName("활성 사용자가 쓰지 않는 닉네임이면 사용 가능을 반환한다")
    void returnsAvailableWhenNicknameCanBeUsed() {
        given(userRepository.existsActiveUserByNickname("runner")).willReturn(false);

        NicknameAvailabilityResponse response = userProfileService.getNicknameAvailability("  runner  ");

        assertThat(response.available()).isTrue();
    }

    @Test
    @DisplayName("삭제되었거나 없는 사용자는 USER_NOT_FOUND를 반환한다")
    void throwsWhenUserIsNotFound() {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "runner",
                LocalDate.of(1998, 4, 5),
                Gender.FEMALE,
                165.5,
                52.3
        );

        given(userRepository.findNotDeletedUser(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.updateProfile(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getResponseCode())
                .isEqualTo(ResponseCode.USER_NOT_FOUND);

        then(userRepository).should(never()).existsByNicknameAndIdNot("runner", 1L);
    }

    @Test
    @DisplayName("trim 후 닉네임 길이가 40자를 넘으면 잘못된 입력 예외를 반환한다")
    void throwsWhenTrimmedNicknameIsTooLong() {
        String longNickname = "a".repeat(41);
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "  " + longNickname + "  ",
                LocalDate.of(1998, 4, 5),
                Gender.FEMALE,
                165.5,
                52.3
        );

        given(userRepository.findNotDeletedUser(1L)).willReturn(Optional.of(User.createPendingSocialUser()));

        assertThatThrownBy(() -> userProfileService.updateProfile(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getResponseCode())
                .isEqualTo(ResponseCode.INVALID_INPUT_VALUE);

        then(userRepository).should(never()).existsByNicknameAndIdNot(longNickname, 1L);
    }

    private User completedUser(Long id, String nickname) {
        User user = User.createPendingSocialUser();
        setField(user, "id", id);
        user.completeProfile(
                nickname,
                LocalDate.of(1999, 1, 2),
                Gender.MALE,
                180.0,
                72.5
        );
        return user;
    }

    private void setField(Object target, String fieldName, Object value) {
        // 서비스 단위 테스트에서도 영속화 없이 엔티티 id를 고정하기 위해 reflection을 사용합니다.
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
