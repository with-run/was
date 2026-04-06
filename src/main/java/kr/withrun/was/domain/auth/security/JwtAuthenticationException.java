package kr.withrun.was.domain.auth.security;

import kr.withrun.was.global.response.ResponseCode;
import org.springframework.security.core.AuthenticationException;

// JWT 인증 단계에서 발생한 실패를 Spring Security 예외 체계로 올리기 위한 커스텀 예외입니다.
// ResponseCode를 함께 들고 다니면 AuthenticationEntryPoint가 상황별 401 JSON을 일관되게 만들 수 있습니다.
public class JwtAuthenticationException extends AuthenticationException {

    private final ResponseCode responseCode;

    public JwtAuthenticationException(ResponseCode responseCode, String message) {
        super(message);
        this.responseCode = responseCode;
    }

    public JwtAuthenticationException(ResponseCode responseCode, String message, Throwable cause) {
        super(message, cause);
        this.responseCode = responseCode;
    }

    // TOKEN_INVALID, TOKEN_EXPIRED 같은 최종 응답 코드를 EntryPoint에 전달합니다.
    public ResponseCode getResponseCode() {
        return responseCode;
    }
}
