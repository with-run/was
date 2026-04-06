package kr.withrun.was.domain.auth.service;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import kr.withrun.was.domain.auth.dto.AuthExchangeResult;
import kr.withrun.was.domain.auth.dto.AuthMeResponse;
import kr.withrun.was.domain.auth.dto.ReissueAccessTokenResponse;
import kr.withrun.was.domain.auth.dto.AuthTokenPair;
import kr.withrun.was.domain.auth.jwt.JwtProvider;
import kr.withrun.was.domain.auth.security.AuthenticatedUser;
import kr.withrun.was.domain.auth.security.JwtAuthenticationException;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.entity.UserAuthAccount;
import kr.withrun.was.domain.user.repository.UserAuthAccountRepository;
import kr.withrun.was.domain.user.repository.UserRepository;
import kr.withrun.was.global.config.JwtProperties;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserAuthAccountRepository userAuthAccountRepository;
    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;

    public AuthMeResponse getCurrentUser(AuthenticatedUser authenticatedUser) {
        // JWT 필터가 넣어준 현재 사용자 정보가 없으면 "로그인 안 됨"으로 간주합니다.
        if (authenticatedUser == null || authenticatedUser.userId() == null) {
            throw new CustomException(ResponseCode.UNAUTHORIZED);
        }

        User user = userRepository.findNotDeletedUser(authenticatedUser.userId())
                .orElseThrow(() -> new CustomException(ResponseCode.USER_NOT_FOUND));

        return AuthMeResponse.from(user, authenticatedUser);
    }

    public ReissueAccessTokenResponse reissueAccessToken(HttpServletRequest request) {
        String refreshToken = extractRefreshToken(request);
        Long userId = extractUserIdFromRefreshToken(refreshToken);

        User user = userRepository.findNotDeletedUser(userId)
                .orElseThrow(() -> new CustomException(ResponseCode.USER_NOT_FOUND));

        String provider = userAuthAccountRepository.findById(userId)
                .map(UserAuthAccount::getProvider)
                .orElseThrow(() -> new CustomException(ResponseCode.UNAUTHORIZED));

        String accessToken = jwtProvider.generateAccessToken(user, provider);
        return new ReissueAccessTokenResponse(accessToken, provider, user.isProfileCompleted());
    }

    public ResponseCookie expireRefreshTokenCookie() {
        return jwtProvider.expireRefreshTokenCookie();
    }

    @Transactional
    public AuthExchangeResult exchangeMobileAuthCode(String code) {
        // 딥링크로 받은 code를 WebView 안에서 교환해, 이제부터는 WebView 저장소 기준으로 세션을 유지합니다.
        AuthenticatedUser authenticatedUser = extractAuthenticatedUserFromMobileAuthCode(code);
        User user = userRepository.findNotDeletedUser(authenticatedUser.userId())
                .orElseThrow(() -> new CustomException(ResponseCode.USER_NOT_FOUND));

        AuthTokenPair tokenPair = jwtProvider.generateTokenPair(user, authenticatedUser.provider());

        return new AuthExchangeResult(
                tokenPair.accessToken(),
                jwtProvider.createRefreshTokenCookie(tokenPair.refreshToken())
        );
    }

    private String extractRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            throw new CustomException(ResponseCode.UNAUTHORIZED);
        }

        // 여러 쿠키 중 refresh token 이름과 일치하는 값만 골라냅니다.
        return Arrays.stream(cookies)
                .filter(cookie -> jwtProperties.getRefreshCookieName().equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElseThrow(() -> new CustomException(ResponseCode.UNAUTHORIZED));
    }

    private Long extractUserIdFromRefreshToken(String refreshToken) {
        try {
            return jwtProvider.extractUserIdFromRefreshToken(refreshToken);
        } catch (ExpiredJwtException exception) {
            // 재로그인 대신 재발급 분기를 태울 수 있도록 만료와 위조를 구분합니다.
            throw new CustomException(ResponseCode.TOKEN_EXPIRED);
        } catch (JwtAuthenticationException exception) {
            throw new CustomException(exception.getResponseCode());
        } catch (JwtException | IllegalArgumentException exception) {
            throw new CustomException(ResponseCode.TOKEN_INVALID);
        }
    }

    private AuthenticatedUser extractAuthenticatedUserFromMobileAuthCode(String code) {
        try {
            return jwtProvider.extractAuthenticatedUserFromMobileAuthCode(code);
        } catch (ExpiredJwtException exception) {
            // 모바일 code는 refresh token보다 훨씬 짧게 쓰이므로 만료/위조를 별도 응답 코드로 구분합니다.
            throw new CustomException(ResponseCode.LOGIN_CODE_EXPIRED);
        } catch (JwtAuthenticationException exception) {
            throw new CustomException(ResponseCode.LOGIN_CODE_INVALID);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new CustomException(ResponseCode.LOGIN_CODE_INVALID);
        }
    }
}
