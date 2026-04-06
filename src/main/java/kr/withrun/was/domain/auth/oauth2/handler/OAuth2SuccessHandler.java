// OAuth 로그인 성공 시 호출됨.
// OAuth2User에서 provider 정보 읽음
// 1. 기존 계정 조회
// 2. 신규 사용자 생성
// 3. 그 다음 JWT발급/redirect로 넘길 준비

package kr.withrun.was.domain.auth.oauth2.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.withrun.was.domain.auth.jwt.JwtProvider;
import kr.withrun.was.domain.auth.oauth2.userinfo.OAuth2UserInfo;
import kr.withrun.was.domain.auth.oauth2.userinfo.OAuth2UserInfoFactory;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.entity.UserAuthAccount;
import kr.withrun.was.domain.user.repository.UserAuthAccountRepository;
import kr.withrun.was.domain.user.repository.UserRepository;
import kr.withrun.was.global.config.AuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private static final String OAUTH_TARGET_COOKIE_NAME = "withrun_oauth_target";
    private static final String OAUTH_ORIGIN_COOKIE_NAME = "withrun_oauth_origin";

    private final UserRepository userRepository;
    private final UserAuthAccountRepository userAuthAccountRepository;
    private final AuthProperties authProperties;
    // OAuth 로그인 성공 후 우리 서비스용 JWT를 발급하는 담당자
    private final JwtProvider jwtProvider;

    // Override
    // 부모 인터페이스나 부모 클래스에 이미 있는 메서드를 내가 다시 구현한 것.
    // Transactional
    // 이 메서드 안에서 DB 작업을 하나의 트랜잭션으로 묶어라.
    // 중간에 하나라도 실패하면 rollback을 의미
    @Override
    @Transactional
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        // OAuth 로그인 성공 후 Spring Security가 넘겨준 인증 객체를 꺼냅니다.
        OAuth2AuthenticationToken oAuth2AuthenticationToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // 현재 로그인한 소셜 제공자 이름(google, kakao)을 가져옵니다.
        String registrationId = oAuth2AuthenticationToken.getAuthorizedClientRegistrationId();

        // provider별 raw 응답을 공통 형식으로 파싱합니다.
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.of(registrationId, oAuth2User.getAttributes());

        User user;

        // provider + providerUserId로 기존 소셜 계정을 먼저 찾습니다.
        UserAuthAccount authAccount = userAuthAccountRepository
                .findByProviderAndProviderUserId(userInfo.getProvider(), userInfo.getProviderUserId())
                .orElse(null);

        if (authAccount != null) {
            // 이미 가입된 계정이면 연결된 기존 User를 사용합니다.
            user = authAccount.getUser();
        } else {
            // 처음 로그인한 계정이면 온보딩 전 상태의 User를 먼저 생성합니다.
            user = userRepository.save(
                    User.createPendingSocialUser(generateRandomNickname())
            );

            // 생성한 User와 소셜 계정을 연결하는 매핑 정보를 저장합니다.
            UserAuthAccount newAuthAccount = UserAuthAccount.create(
                    user,
                    userInfo.getProvider(),
                    userInfo.getProviderUserId(),
                    userInfo.getEmail(),
                    userInfo.getProfileImageUrl()
            );
            userAuthAccountRepository.save(newAuthAccount);

        }

        boolean mobileLoginRequest = isMobileLoginRequest(request);

        if (mobileLoginRequest) {
            // 모바일은 외부 브라우저에서 받은 refresh cookie를 WebView가 공유하지 못할 수 있어,
            // 세션 대신 짧은 auth code만 딥링크로 넘기고 앱이 WebView 안에서 교환하게 합니다.
            String mobileAuthCode = jwtProvider.generateMobileAuthCode(user, userInfo.getProvider());
            response.addHeader(HttpHeaders.SET_COOKIE, clearOAuthTargetCookie().toString());
            response.addHeader(HttpHeaders.SET_COOKIE, clearOAuthOriginCookie().toString());
            response.sendRedirect(resolveMobileCallbackRedirectUrl(mobileAuthCode));
            return;
        }

        // 웹 로그인은 기존처럼 refresh cookie 기반 세션 복구를 유지합니다.
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                jwtProvider.createRefreshTokenCookie(
                        jwtProvider.generateTokenPair(user, userInfo.getProvider()).refreshToken()
                ).toString()
        );

        // mobile/web 분기에만 필요했던 힌트 쿠키는 callback redirect 전에 바로 제거합니다.
        response.addHeader(HttpHeaders.SET_COOKIE, clearOAuthTargetCookie().toString());
        // 웹 origin 힌트도 OAuth 완료 직후에는 더 이상 필요 없으므로 함께 제거합니다.
        response.addHeader(HttpHeaders.SET_COOKIE, clearOAuthOriginCookie().toString());

        String redirectUrl = resolveCallbackRedirectUrl(request);

        response.sendRedirect(redirectUrl);
    }

    private String resolveCallbackRedirectUrl(HttpServletRequest request) {
        String origin = resolveWebOriginCookie(request);
        if (StringUtils.hasText(origin)) {
            // 웹은 로그인 시작 origin 이 검증된 값으로 남아 있으면 그 도메인의 callback 으로 되돌립니다.
            return UriComponentsBuilder
                    .fromUriString(origin)
                    .path("/auth/callback")
                    .build(true)
                    .toUriString();
        }

        return UriComponentsBuilder
                .fromUriString(authProperties.getFrontendBaseUrl())
                .path("/auth/callback")
                .build(true)
                .toUriString();
    }

    private String resolveMobileCallbackRedirectUrl(String mobileAuthCode) {
        // 딥링크 query string은 WebViewScreen이 그대로 /auth/callback 쪽으로 전달해줍니다.
        return UriComponentsBuilder
                .fromUriString(authProperties.getMobileDeepLinkBase())
                .queryParam("code", mobileAuthCode)
                .build(true)
                .toUriString();
    }

    private String resolveWebOriginCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        return Arrays.stream(cookies)
                .filter(cookie -> OAUTH_ORIGIN_COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                // 쿠키를 다시 한번 화이트리스트로 검증해 신뢰 가능한 origin 만 사용합니다.
                .filter(this::isAllowedOrigin)
                .findFirst()
                .orElse(null);
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
        return ResponseCookie.from(OAUTH_TARGET_COOKIE_NAME, "")
                .httpOnly(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
    }

    private ResponseCookie clearOAuthOriginCookie() {
        return ResponseCookie.from(OAUTH_ORIGIN_COOKIE_NAME, "")
                .httpOnly(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
    }

    private boolean isAllowedOrigin(String origin) {
        return authProperties.getAllowedOrigins().contains(origin);
    }

    private String generateRandomNickname() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
