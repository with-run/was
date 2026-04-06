package kr.withrun.was.domain.auth.oauth2.userinfo;

public interface OAuth2UserInfo {

    // 어떤 소셜 로그인 제공자인지 "google" or "kakao"
    String getProvider();

    // 그 provider 안에서의 사용자 고유 ID ("sub", "id")
    String getProviderUserId();

    // 있으면 Email, 없으면 null
    String getEmail();

    // 있으면 Profile Image, 없으면 null
    String getProfileImageUrl();
}
