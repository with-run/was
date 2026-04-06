package kr.withrun.was.domain.auth.oauth2.userinfo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GoogleOAuth2UserInfo")
class GoogleOAuth2UserInfoTest {

    @Test
    @DisplayName("parses Google user info attributes")
    void parsesGoogleUserInfoAttributes() {
        GoogleOAuth2UserInfo userInfo = new GoogleOAuth2UserInfo(Map.of(
                "sub", "google-user-123",
                "email", "runner@example.com",
                "picture", "https://image.example.com/profile.png"
        ));

        assertThat(userInfo.getProvider()).isEqualTo("google");
        assertThat(userInfo.getProviderUserId()).isEqualTo("google-user-123");
        assertThat(userInfo.getEmail()).isEqualTo("runner@example.com");
        assertThat(userInfo.getProfileImageUrl()).isEqualTo("https://image.example.com/profile.png");
    }

    @Test
    @DisplayName("returns null for optional attributes when they do not exist")
    void returnsNullForMissingOptionalAttributes() {
        GoogleOAuth2UserInfo userInfo = new GoogleOAuth2UserInfo(Map.of());

        assertThat(userInfo.getProvider()).isEqualTo("google");
        assertThat(userInfo.getProviderUserId()).isNull();
        assertThat(userInfo.getEmail()).isNull();
        assertThat(userInfo.getProfileImageUrl()).isNull();
    }
}
