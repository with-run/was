// JWT 관련 설정 값을 application.yml이나 Parameter Store에서 읽어오는 설정 클래스

package kr.withrun.was.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "auth.jwt")
public class JwtProperties {

    // access/refresh token 서명에 사용할 secret key
    private String accessSecret;
    // access token 만료 시간(초). 기본값 1시간
    private long accessExpirationSeconds = 3600;
    // refresh token 만료 시간(초). 기본값 14일
    private long refreshExpirationSeconds = 1209600;
    // 모바일 OAuth 완료 후 WebView 안에서 교환할 짧은 수명의 로그인 코드 만료 시간(초)
    private long mobileAuthCodeExpirationSeconds = 300;
    // 브라우저에 내려줄 refresh token 쿠키 이름
    private String refreshCookieName = "refresh_token";
    // refresh token 쿠키가 유효한 경로
    private String refreshCookiePath = "/";
    // refresh token 쿠키 SameSite 정책
    private String refreshCookieSameSite = "Lax";
    // 운영 환경에서는 true, 로컬 개발에서는 false로 시작
    private boolean refreshCookieSecure = false;
}
