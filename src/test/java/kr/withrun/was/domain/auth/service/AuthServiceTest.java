package kr.withrun.was.domain.auth.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import kr.withrun.was.domain.auth.dto.AuthExchangeResult;
import kr.withrun.was.domain.auth.dto.AuthMeResponse;
import kr.withrun.was.domain.auth.dto.AuthTokenPair;
import kr.withrun.was.domain.auth.dto.ReissueAccessTokenResponse;
import kr.withrun.was.domain.auth.jwt.JwtProvider;
import kr.withrun.was.domain.auth.security.AuthenticatedUser;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.entity.UserAuthAccount;
import kr.withrun.was.domain.user.repository.UserAuthAccountRepository;
import kr.withrun.was.domain.user.repository.UserRepository;
import kr.withrun.was.domain.user.type.Gender;
import kr.withrun.was.global.config.JwtProperties;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.crypto.SecretKey;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    private static final String ACCESS_SECRET = "0123456789abcdef0123456789abcdef";

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserAuthAccountRepository userAuthAccountRepository;

    private JwtProperties jwtProperties;
    private JwtProvider jwtProvider;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        jwtProperties = jwtProperties();
        jwtProvider = new JwtProvider(jwtProperties);
        authService = new AuthService(
                userRepository,
                userAuthAccountRepository,
                jwtProvider,
                jwtProperties
        );
    }

    @Test
    @DisplayName("returns current authenticated user summary")
    void returnsCurrentAuthenticatedUserSummary() {
        User user = completedUser(10L);
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(10L, "google", true);

        when(userRepository.findNotDeletedUser(10L)).thenReturn(Optional.of(user));

        AuthMeResponse response = authService.getCurrentUser(authenticatedUser);

        assertThat(response.userId()).isEqualTo(10L);
        assertThat(response.provider()).isEqualTo("google");
        assertThat(response.profileCompleted()).isTrue();
        assertThat(response.nickname()).isEqualTo("runner");
    }

    @Test
    @DisplayName("returns null profile fields for onboarding-pending user")
    void returnsNullProfileFieldsForPendingUser() {
        User user = User.createPendingSocialUser("pending-google-123");
        setField(user, "id", 10L);
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(10L, "google", false);

        when(userRepository.findNotDeletedUser(10L)).thenReturn(Optional.of(user));

        AuthMeResponse response = authService.getCurrentUser(authenticatedUser);

        assertThat(response.profileCompleted()).isFalse();
        assertThat(response.nickname()).isNull();
        assertThat(response.birthDate()).isNull();
        assertThat(response.gender()).isNull();
        assertThat(response.height()).isNull();
        assertThat(response.weight()).isNull();
    }

    @Test
    @DisplayName("reissues access token when refresh cookie is valid")
    void reissuesAccessTokenWhenRefreshCookieIsValid() {
        User user = completedUser(10L);
        UserAuthAccount authAccount = UserAuthAccount.create(
                user,
                "google",
                "google-user-123",
                "runner@example.com",
                "https://image.example.com/profile.png"
        );
        AuthTokenPair tokenPair = jwtProvider.generateTokenPair(user, "google");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("refresh_token", tokenPair.refreshToken()));

        when(userRepository.findNotDeletedUser(10L)).thenReturn(Optional.of(user));
        when(userAuthAccountRepository.findById(10L)).thenReturn(Optional.of(authAccount));

        ReissueAccessTokenResponse response = authService.reissueAccessToken(request);

        assertThat(response.provider()).isEqualTo("google");
        assertThat(response.profileCompleted()).isTrue();
        assertThat(jwtProvider.extractAuthenticatedUser(response.accessToken()).userId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("throws unauthorized when refresh cookie is missing")
    void throwsUnauthorizedWhenRefreshCookieIsMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThatThrownBy(() -> authService.reissueAccessToken(request))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getResponseCode())
                .isEqualTo(ResponseCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("throws token expired when refresh token is expired")
    void throwsTokenExpiredWhenRefreshTokenIsExpired() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("refresh_token", expiredRefreshToken(10L)));

        assertThatThrownBy(() -> authService.reissueAccessToken(request))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getResponseCode())
                .isEqualTo(ResponseCode.TOKEN_EXPIRED);
    }

    @Test
    @DisplayName("exchanges mobile auth code into access token and refresh cookie")
    void exchangesMobileAuthCodeIntoAccessTokenAndRefreshCookie() {
        User user = completedUser(10L);
        String mobileAuthCode = jwtProvider.generateMobileAuthCode(user, "google");

        when(userRepository.findNotDeletedUser(10L)).thenReturn(Optional.of(user));

        AuthExchangeResult result = authService.exchangeMobileAuthCode(mobileAuthCode);

        assertThat(jwtProvider.extractAuthenticatedUser(result.accessToken()).userId()).isEqualTo(10L);
        assertThat(result.refreshTokenCookie().getName()).isEqualTo("refresh_token");
        assertThat(result.refreshTokenCookie().isHttpOnly()).isTrue();
    }

    @Test
    @DisplayName("throws login code expired when mobile auth code is expired")
    void throwsLoginCodeExpiredWhenMobileAuthCodeIsExpired() {
        assertThatThrownBy(() -> authService.exchangeMobileAuthCode(expiredMobileAuthCode(10L)))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getResponseCode())
                .isEqualTo(ResponseCode.LOGIN_CODE_EXPIRED);
    }

    private JwtProperties jwtProperties() {
        JwtProperties properties = new JwtProperties();
        properties.setAccessSecret(ACCESS_SECRET);
        properties.setAccessExpirationSeconds(3600L);
        properties.setRefreshExpirationSeconds(1209600L);
        properties.setMobileAuthCodeExpirationSeconds(300L);
        properties.setRefreshCookieName("refresh_token");
        properties.setRefreshCookiePath("/");
        properties.setRefreshCookieSameSite("Lax");
        properties.setRefreshCookieSecure(false);
        return properties;
    }

    private String expiredRefreshToken(Long userId) {
        SecretKey secretKey = Keys.hmacShaKeyFor(ACCESS_SECRET.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userId", userId)
                .claim("tokenType", "refresh")
                .issuedAt(Date.from(now.minusSeconds(120)))
                .expiration(Date.from(now.minusSeconds(60)))
                .signWith(secretKey)
                .compact();
    }

    private String expiredMobileAuthCode(Long userId) {
        SecretKey secretKey = Keys.hmacShaKeyFor(ACCESS_SECRET.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userId", userId)
                .claim("provider", "google")
                .claim("profileCompleted", true)
                .claim("tokenType", "mobile_auth_code")
                .issuedAt(Date.from(now.minusSeconds(120)))
                .expiration(Date.from(now.minusSeconds(60)))
                .signWith(secretKey)
                .compact();
    }

    private User completedUser(Long id) {
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
