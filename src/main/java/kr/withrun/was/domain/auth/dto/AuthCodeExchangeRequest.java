package kr.withrun.was.domain.auth.dto;

// 외부 브라우저 OAuth 성공 후 앱이 WebView 안에서 교환할 짧은 로그인 코드를 받습니다.
public record AuthCodeExchangeRequest(
        String code
) {
}
