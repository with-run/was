package kr.withrun.was.domain.user.entity;

import kr.withrun.was.domain.user.type.Gender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("User entity")
class UserTest {

    @Test
    @DisplayName("createPendingSocialUser creates an onboarding-pending user")
    void createPendingSocialUserCreatesPendingUser() {
        User user = User.createPendingSocialUser();

        assertThat(user.isProfileCompleted()).isFalse();
        assertThat(user.getNickname()).isNull();
        assertThat(user.getBirthDate()).isNull();
        assertThat(user.getGender()).isNull();
        assertThat(user.getHeight()).isNull();
        assertThat(user.getWeight()).isNull();
    }

    @Test
    @DisplayName("completeProfile fills profile fields and marks profile as completed")
    void completeProfileCompletesUserProfile() {
        User user = User.createPendingSocialUser();

        user.completeProfile(
                "  runner  ",
                LocalDate.of(1998, 4, 5),
                Gender.FEMALE,
                165.5,
                52.3
        );

        assertThat(user.getNickname()).isEqualTo("runner");
        assertThat(user.getBirthDate()).isEqualTo(LocalDate.of(1998, 4, 5));
        assertThat(user.getGender()).isEqualTo(Gender.FEMALE);
        assertThat(user.getHeight()).isEqualTo(165.5);
        assertThat(user.getWeight()).isEqualTo(52.3);
        assertThat(user.isProfileCompleted()).isTrue();
    }

    @Test
    @DisplayName("completeProfile rejects blank nickname")
    void completeProfileRejectsBlankNickname() {
        User user = User.createPendingSocialUser();

        assertThatThrownBy(() -> user.completeProfile(
                "   ",
                LocalDate.of(1998, 4, 5),
                Gender.MALE,
                180.0,
                70.0
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("nickname must not be blank");
    }

    @Test
    @DisplayName("completeProfile rejects null birth date")
    void completeProfileRejectsNullBirthDate() {
        User user = User.createPendingSocialUser();

        assertThatThrownBy(() -> user.completeProfile(
                "runner",
                null,
                Gender.MALE,
                180.0,
                70.0
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("birthDate must not be null");
    }

    @Test
    @DisplayName("completeProfile rejects null gender")
    void completeProfileRejectsNullGender() {
        User user = User.createPendingSocialUser();

        assertThatThrownBy(() -> user.completeProfile(
                "runner",
                LocalDate.of(1998, 4, 5),
                null,
                180.0,
                70.0
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("gender must not be null");
    }

    @Test
    @DisplayName("completeProfile rejects non-positive height")
    void completeProfileRejectsNonPositiveHeight() {
        User user = User.createPendingSocialUser();

        assertThatThrownBy(() -> user.completeProfile(
                "runner",
                LocalDate.of(1998, 4, 5),
                Gender.MALE,
                0.0,
                70.0
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("height must be positive");
    }

    @Test
    @DisplayName("completeProfile rejects non-positive weight")
    void completeProfileRejectsNonPositiveWeight() {
        User user = User.createPendingSocialUser();

        assertThatThrownBy(() -> user.completeProfile(
                "runner",
                LocalDate.of(1998, 4, 5),
                Gender.MALE,
                180.0,
                -1.0
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("weight must be positive");
    }
}
