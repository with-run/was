package kr.withrun.was.domain.auth.oauth2.userinfo;

import java.util.Map;

public class GoogleOAuth2UserInfo implements OAuth2UserInfo {

    // Google 사용자 정보 엔드포인트에서 내려준 원본 응답입니다.
    private final Map<String, Object> attributes;

    public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProvider() {
        return "google";
    }

    @Override
    // Google은 "sub" 값을 사용자 고유 식별자로 사용합니다.
    public String getProviderUserId() {
        Object sub = attributes.get("sub");
        return sub != null ? sub.toString() : null;
    }

    @Override
    // v1에서는 이메일을 선택 정보로 다루므로 없으면 null을 반환합니다.
    public String getEmail() {
        Object email = attributes.get("email");
        return email != null ? email.toString() : null;
    }

    @Override
    // Google 프로필 이미지는 "picture" 필드에 들어옵니다.
    public String getProfileImageUrl() {
        Object picture = attributes.get("picture");
        return picture != null ? picture.toString() : null;
    }
}
