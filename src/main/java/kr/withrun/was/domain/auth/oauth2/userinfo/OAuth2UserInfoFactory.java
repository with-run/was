package kr.withrun.was.domain.auth.oauth2.userinfo;

import java.util.Map;

public final class OAuth2UserInfoFactory {

    // 이 클래스는 객체를 만들어서 쓸 필요가 없기 때문에 생성자 금지
    private OAuth2UserInfoFactory() {
    }

    public static OAuth2UserInfo of(String registrationId, Map<String, Object> attributes) {
        if ("google".equals(registrationId)) {
            return new GoogleOAuth2UserInfo(attributes);
        }

        if ("kakao".equals(registrationId)) {
            return new KakaoOAuth2UserInfo(attributes);
        }

        throw new IllegalArgumentException("Unsupported OAuth provider: " + registrationId);
    }
}
