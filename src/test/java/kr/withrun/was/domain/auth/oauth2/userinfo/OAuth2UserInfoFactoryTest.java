package kr.withrun.was.domain.auth.oauth2.userinfo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OAuth2UserInfoFactory")
class OAuth2UserInfoFactoryTest {

    @Test
    @DisplayName("returns Google parser for google registration")
    void returnsGoogleParserForGoogleRegistration() {
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.of("google", Map.of("sub", "abc"));

        assertThat(userInfo).isInstanceOf(GoogleOAuth2UserInfo.class);
    }

    @Test
    @DisplayName("returns Kakao parser for kakao registration")
    void returnsKakaoParserForKakaoRegistration() {
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.of("kakao", Map.of("id", 1L));

        assertThat(userInfo).isInstanceOf(KakaoOAuth2UserInfo.class);
    }

    @Test
    @DisplayName("throws for unsupported provider")
    void throwsForUnsupportedProvider() {
        assertThatThrownBy(() -> OAuth2UserInfoFactory.of("naver", Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported OAuth provider: naver");
    }
}
