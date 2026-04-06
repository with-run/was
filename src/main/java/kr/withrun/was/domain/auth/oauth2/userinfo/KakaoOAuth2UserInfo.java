package kr.withrun.was.domain.auth.oauth2.userinfo;

import java.util.Map;

public class KakaoOAuth2UserInfo implements OAuth2UserInfo {

    // Kakao 사용자 정보 엔드포인트에서 내려준 원본 응답입니다.
    private final Map<String, Object> attributes;

    public KakaoOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProvider() {
        return "kakao";
    }

    @Override
    // Kakao는 최상위 "id" 값을 사용자 고유 식별자로 사용합니다.
    public String getProviderUserId() {
        Object id = attributes.get("id");
        return id != null ? id.toString() : null;
    }

    @Override
    // 이메일은 kakao_account 아래에 있으며, 동의 여부에 따라 없을 수 있습니다.
    public String getEmail() {
        Object kakaoAccountObject = attributes.get("kakao_account");
        if (!(kakaoAccountObject instanceof Map<?, ?> kakaoAccount)) {
            return null;
        }

        Object email = kakaoAccount.get("email");
        return email != null ? email.toString() : null;
    }

    @Override
    // 프로필 이미지 URL은 kakao_account.profile.profile_image_url 경로에 있습니다.
    public String getProfileImageUrl() {
        Object kakaoAccountObject = attributes.get("kakao_account");
        if (!(kakaoAccountObject instanceof Map<?, ?> kakaoAccount)) {
            return null;
        }

        Object profileObject = kakaoAccount.get("profile");
        if (!(profileObject instanceof Map<?, ?> profile)) {
            return null;
        }

        Object profileImageUrl = profile.get("profile_image_url");
        return profileImageUrl != null ? profileImageUrl.toString() : null;
    }
}
