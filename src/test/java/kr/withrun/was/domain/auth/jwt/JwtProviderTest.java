package kr.withrun.was.domain.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import kr.withrun.was.domain.auth.dto.AuthTokenPair;
import kr.withrun.was.domain.auth.security.AuthenticatedUser;
import kr.withrun.was.domain.auth.security.JwtAuthenticationException;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.type.Gender;
import kr.withrun.was.global.config.JwtProperties;
import kr.withrun.was.global.response.ResponseCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import javax.crypto.SecretKey;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtProvider")
class JwtProviderTest {

    // 테스트에서는 항상 같은 secret을 써야 토큰 생성/파싱 결과를 예측할 수 있습니다.
    private static final String ACCESS_SECRET = "0123456789abcdef0123456789abcdef";

    @Test
    @DisplayName("generates access and refresh tokens with expected claims")
    void generatesAccessAndRefreshTokensWithExpectedClaims() {
        JwtProvider jwtProvider = new JwtProvider(jwtProperties());
        User user = completedUser(10L);

        AuthTokenPair tokenPair = jwtProvider.generateTokenPair(user, "google");

        Claims accessClaims = parseClaims(tokenPair.accessToken());
        Claims refreshClaims = parseClaims(tokenPair.refreshToken());

        assertThat(accessClaims.getSubject()).isEqualTo("10");
        assertThat(accessClaims.get("userId", Long.class)).isEqualTo(10L);
        assertThat(accessClaims.get("provider", String.class)).isEqualTo("google");
        assertThat(accessClaims.get("profileCompleted", Boolean.class)).isTrue();
        assertThat(accessClaims.get("tokenType", String.class)).isEqualTo("access");

        assertThat(refreshClaims.getSubject()).isEqualTo("10");
        assertThat(refreshClaims.get("userId", Long.class)).isEqualTo(10L);
        assertThat(refreshClaims.get("tokenType", String.class)).isEqualTo("refresh");
    }

    @Test
    @DisplayName("extracts authenticated user from access token")
    void extractsAuthenticatedUserFromAccessToken() {
        JwtProvider jwtProvider = new JwtProvider(jwtProperties());
        User user = completedUser(10L);

        AuthenticatedUser authenticatedUser = jwtProvider.extractAuthenticatedUser(
                jwtProvider.generateTokenPair(user, "google").accessToken()
        );

        assertThat(authenticatedUser.userId()).isEqualTo(10L);
        assertThat(authenticatedUser.provider()).isEqualTo("google");
        assertThat(authenticatedUser.profileCompleted()).isTrue();
    }

    @Test
    @DisplayName("rejects refresh token when extracting authenticated user")
    void rejectsRefreshTokenWhenExtractingAuthenticatedUser() {
        JwtProvider jwtProvider = new JwtProvider(jwtProperties());
        User user = completedUser(10L);
        String refreshToken = jwtProvider.generateTokenPair(user, "google").refreshToken();

        assertThatThrownBy(() -> jwtProvider.extractAuthenticatedUser(refreshToken))
                .isInstanceOf(JwtAuthenticationException.class)
                .extracting(exception -> ((JwtAuthenticationException) exception).getResponseCode())
                .isEqualTo(ResponseCode.TOKEN_INVALID);
    }

    @Test
    @DisplayName("throws when access token is expired")
    void throwsWhenAccessTokenIsExpired() {
        JwtProvider jwtProvider = new JwtProvider(jwtProperties());

        assertThatThrownBy(() -> jwtProvider.parseAccessToken(expiredAccessToken(10L)))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("extracts user id from refresh token")
    void extractsUserIdFromRefreshToken() {
        JwtProvider jwtProvider = new JwtProvider(jwtProperties());
        User user = completedUser(10L);
        String refreshToken = jwtProvider.generateTokenPair(user, "google").refreshToken();

        Long userId = jwtProvider.extractUserIdFromRefreshToken(refreshToken);

        assertThat(userId).isEqualTo(10L);
    }

    @Test
    @DisplayName("generates and extracts authenticated user from mobile auth code")
    void generatesAndExtractsAuthenticatedUserFromMobileAuthCode() {
        JwtProvider jwtProvider = new JwtProvider(jwtProperties());
        User user = completedUser(10L);

        String mobileAuthCode = jwtProvider.generateMobileAuthCode(user, "google");
        AuthenticatedUser authenticatedUser = jwtProvider.extractAuthenticatedUserFromMobileAuthCode(mobileAuthCode);
        Claims claims = parseClaims(mobileAuthCode);

        assertThat(claims.get("tokenType", String.class)).isEqualTo("mobile_auth_code");
        assertThat(authenticatedUser.userId()).isEqualTo(10L);
        assertThat(authenticatedUser.provider()).isEqualTo("google");
        assertThat(authenticatedUser.profileCompleted()).isTrue();
    }

    @Test
    @DisplayName("rejects access token when extracting mobile auth code")
    void rejectsAccessTokenWhenExtractingMobileAuthCode() {
        JwtProvider jwtProvider = new JwtProvider(jwtProperties());
        User user = completedUser(10L);
        String accessToken = jwtProvider.generateTokenPair(user, "google").accessToken();

        assertThatThrownBy(() -> jwtProvider.extractAuthenticatedUserFromMobileAuthCode(accessToken))
                .isInstanceOf(JwtAuthenticationException.class)
                .extracting(exception -> ((JwtAuthenticationException) exception).getResponseCode())
                .isEqualTo(ResponseCode.LOGIN_CODE_INVALID);
    }

    @Test
    @DisplayName("creates refresh token cookie with configured attributes")
    void createsRefreshTokenCookieWithConfiguredAttributes() {
        JwtProvider jwtProvider = new JwtProvider(jwtProperties());

        ResponseCookie cookie = jwtProvider.createRefreshTokenCookie("refresh-token");

        assertThat(cookie.getName()).isEqualTo("refresh_token");
        assertThat(cookie.getValue()).isEqualTo("refresh-token");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isFalse();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getSameSite()).isEqualTo("None");
        assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(1209600L);
    }

    @Test
    @DisplayName("expires refresh token cookie with configured attributes")
    void expiresRefreshTokenCookieWithConfiguredAttributes() {
        JwtProvider jwtProvider = new JwtProvider(jwtProperties());

        ResponseCookie cookie = jwtProvider.expireRefreshTokenCookie();

        assertThat(cookie.getName()).isEqualTo("refresh_token");
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isFalse();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getSameSite()).isEqualTo("None");
        assertThat(cookie.getMaxAge().getSeconds()).isZero();
    }

    @Test
    @DisplayName("throws when jwt secret is missing")
    void throwsWhenJwtSecretIsMissing() {
        JwtProperties jwtProperties = jwtProperties();
        jwtProperties.setAccessSecret(" ");
        JwtProvider jwtProvider = new JwtProvider(jwtProperties);

        assertThatThrownBy(() -> jwtProvider.generateTokenPair(completedUser(10L), "google"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("auth.jwt.access-secret is not configured");
    }

    private JwtProperties jwtProperties() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setAccessSecret(ACCESS_SECRET);
        jwtProperties.setAccessExpirationSeconds(3600L);
        jwtProperties.setRefreshExpirationSeconds(1209600L);
        jwtProperties.setMobileAuthCodeExpirationSeconds(300L);
        jwtProperties.setRefreshCookieName("refresh_token");
        jwtProperties.setRefreshCookiePath("/");
        jwtProperties.setRefreshCookieSameSite("None");
        jwtProperties.setRefreshCookieSecure(false);
        return jwtProperties;
    }

    private Claims parseClaims(String token) {
        // 프로덕션 코드의 parser와 같은 secret으로 직접 파싱해 claim 값을 검증합니다.
        SecretKey secretKey = Keys.hmacShaKeyFor(ACCESS_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private String expiredAccessToken(Long userId) {
        SecretKey secretKey = Keys.hmacShaKeyFor(ACCESS_SECRET.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userId", userId)
                .claim("provider", "google")
                .claim("profileCompleted", true)
                .claim("tokenType", "access")
                .issuedAt(Date.from(now.minusSeconds(120)))
                .expiration(Date.from(now.minusSeconds(60)))
                .signWith(secretKey)
                .compact();
    }

    private User completedUser(Long id) {
        // 토큰에 profileCompleted=true가 들어가도록, 엔티티 메서드를 사용해 실제 완료 사용자 상태를 만듭니다.
        User user = User.createPendingSocialUser();
        setField(user, "id", id);
        user.completeProfile(
                "runner",
                LocalDate.of(1999, 1, 2),
                Gender.MALE,
                180.0,
                72.5
        );
        return user;
    }

    private void setField(Object target, String fieldName, Object value) {
        // JPA 엔티티의 private id 필드는 테스트에서만 reflection으로 주입합니다.
        Class<?> currentClass = target.getClass();
        while (currentClass != null) {
            try {
                Field field = currentClass.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException exception) {
                currentClass = currentClass.getSuperclass();
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException("Failed to set field " + fieldName, exception);
            }
        }

        throw new IllegalArgumentException("Field not found: " + fieldName);
    }
}
