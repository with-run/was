package kr.withrun.was.domain.auth.oauth2.userinfo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("KakaoOAuth2UserInfo")
class KakaoOAuth2UserInfoTest {

    @Test
    @DisplayName("parses nested Kakao user info attributes")
    void parsesNestedKakaoUserInfoAttributes() {
        KakaoOAuth2UserInfo userInfo = new KakaoOAuth2UserInfo(Map.of(
                "id", 123456789L,
                "kakao_account", Map.of(
                        "email", "runner@kakao.com",
                        "profile", Map.of(
                                "profile_image_url", "https://image.example.com/kakao.png"
                        )
                )
        ));

        assertThat(userInfo.getProvider()).isEqualTo("kakao");
        assertThat(userInfo.getProviderUserId()).isEqualTo("123456789");
        assertThat(userInfo.getEmail()).isEqualTo("runner@kakao.com");
        assertThat(userInfo.getProfileImageUrl()).isEqualTo("https://image.example.com/kakao.png");
    }

    @Test
    @DisplayName("returns null when nested Kakao fields are missing")
    void returnsNullWhenNestedKakaoFieldsAreMissing() {
        KakaoOAuth2UserInfo userInfo = new KakaoOAuth2UserInfo(Map.of("id", 123456789L));

        assertThat(userInfo.getProvider()).isEqualTo("kakao");
        assertThat(userInfo.getProviderUserId()).isEqualTo("123456789");
        assertThat(userInfo.getEmail()).isNull();
        assertThat(userInfo.getProfileImageUrl()).isNull();
    }
}
