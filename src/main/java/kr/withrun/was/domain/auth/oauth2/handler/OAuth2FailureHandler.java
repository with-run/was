package kr.withrun.was.domain.auth.oauth2.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.withrun.was.global.config.AuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class OAuth2FailureHandler implements AuthenticationFailureHandler {

    private static final String OAUTH_TARGET_COOKIE_NAME = "withrun_oauth_target";

    private final AuthProperties authProperties;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        String error = resolveError(request, exception);
        // 성공/실패 모두 같은 callback 화면으로 모아두면 프론트가 에러 분기를 한곳에서 처리할 수 있다.
        response.addHeader(HttpHeaders.SET_COOKIE, clearOAuthTargetCookie().toString());
        String redirectUrl = resolveCallbackRedirectUrl(request, error);

        response.sendRedirect(redirectUrl);
    }

    private String resolveCallbackRedirectUrl(HttpServletRequest request, String error) {
        // 모바일 로그인 요청이면 브라우저 대신 딥링크로 돌려보내고, 웹은 shared callback route로 보낸다.
        if (isMobileLoginRequest(request) && StringUtils.hasText(authProperties.getMobileDeepLinkBase())) {
            return UriComponentsBuilder
                    .fromUriString(authProperties.getMobileDeepLinkBase())
                    .queryParam("error", error)
                    .build(true)
                    .toUriString();
        }

        return UriComponentsBuilder
                .fromUriString(authProperties.getFrontendBaseUrl())
                .path("/auth/callback")
                .queryParam("error", error)
                .build(true)
                .toUriString();
    }

    private boolean isMobileLoginRequest(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return false;
        }

        return Arrays.stream(cookies)
                .anyMatch(cookie ->
                        OAUTH_TARGET_COOKIE_NAME.equals(cookie.getName()) && "mobile".equals(cookie.getValue())
                );
    }

    private ResponseCookie clearOAuthTargetCookie() {
        // mobile 전용 힌트 쿠키는 한 번 redirect 한 뒤 바로 비워서 다음 로그인에 섞이지 않게 한다.
        return ResponseCookie.from(OAUTH_TARGET_COOKIE_NAME, "")
                .httpOnly(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
    }

    private String resolveError(HttpServletRequest request, AuthenticationException exception) {
        // provider가 쿼리 파라미터로 직접 내려준 오류가 있으면 가장 우선으로 사용합니다.
        String requestError = request.getParameter("error");
        if ("access_denied".equals(requestError)) {
            return "access_denied";
        }

        if (exception instanceof OAuth2AuthenticationException oAuth2Exception) {
            String errorCode = oAuth2Exception.getError().getErrorCode();

            if ("access_denied".equals(errorCode)) {
                return "access_denied";
            }

            // 사용자가 로그인 도중 취소했거나, OAuth state/session이 끊긴 경우를 취소 계열로 묶습니다.
            if ("authorization_request_not_found".equals(errorCode)
                    || "invalid_state_parameter".equals(errorCode)
                    || "invalid_request".equals(errorCode)) {
                return "cancelled";
            }
        }

        return "server_error";
    }
}
