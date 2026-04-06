package kr.withrun.was.domain.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.withrun.was.global.response.ApiResponse;
import kr.withrun.was.global.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

// 인증 실패를 HTML redirect 대신 JSON 401 응답으로 바꿔주는 진입점입니다.
// OAuth 페이지로 보내면 API 클라이언트와 테스트가 모두 불편해지므로, Step 4부터는 API 스타일 응답으로 고정합니다.
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        // JwtAuthenticationException이면 내부에 담긴 세부 코드(TOKEN_INVALID, TOKEN_EXPIRED 등)를 그대로 사용하고,
        // 그 외 AuthenticationException은 일반적인 UNAUTHORIZED로 처리합니다.
        ResponseCode responseCode = resolveResponseCode(authException);

        response.setStatus(responseCode.getStatus().value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.fail(responseCode));
    }

    private ResponseCode resolveResponseCode(AuthenticationException authException) {
        if (authException instanceof JwtAuthenticationException jwtAuthenticationException) {
            return jwtAuthenticationException.getResponseCode();
        }

        return ResponseCode.UNAUTHORIZED;
    }
}
