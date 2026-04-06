package kr.withrun.was.domain.auth.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import kr.withrun.was.domain.auth.jwt.JwtProvider;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.type.Gender;
import kr.withrun.was.global.config.JwtProperties;
import kr.withrun.was.global.response.ResponseCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.crypto.SecretKey;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtAuthenticationFilter")
class JwtAuthenticationFilterTest {

    private static final String ACCESS_SECRET = "0123456789abcdef0123456789abcdef";

    // Spring 컨텍스트 없이도 필터 단위 동작을 빠르게 확인할 수 있도록 직접 조립합니다.
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JwtProvider jwtProvider = new JwtProvider(jwtProperties());
    private final JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(
            jwtProvider,
            new RestAuthenticationEntryPoint(objectMapper)
    );

    @AfterEach
    void clearSecurityContext() {
        // SecurityContext는 static 저장소라 테스트 간 오염을 막기 위해 매번 비워줍니다.
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("유효한 access token이면 SecurityContext에 현재 사용자를 저장한다")
    void authenticatesRequestWithValidAccessToken() throws Exception {
        // 보호된 프로필 수정 API를 대상으로 access token 인증을 검증합니다.
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/users/me");
        request.addHeader(
                HttpHeaders.AUTHORIZATION,
                "Bearer " + jwtProvider.generateTokenPair(completedUser(7L), "google").accessToken()
        );
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isInstanceOf(AuthenticatedUser.class);
        assertThat(((AuthenticatedUser) authentication.getPrincipal()).userId()).isEqualTo(7L);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("세션 인증이 이미 있어도 Authorization 헤더가 있으면 JWT 인증으로 교체한다")
    void prefersJwtAuthenticationOverExistingSessionAuthentication() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        "oauth2-session-user",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                )
        );

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/me");
        request.addHeader(
                HttpHeaders.AUTHORIZATION,
                "Bearer " + jwtProvider.generateTokenPair(completedUser(11L), "google").accessToken()
        );
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuthenticationFilter.doFilter(request, response, new MockFilterChain());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isInstanceOf(AuthenticatedUser.class);
        assertThat(((AuthenticatedUser) authentication.getPrincipal()).userId()).isEqualTo(11L);
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 그대로 다음 필터로 넘긴다")
    void passesThroughWhenAuthorizationHeaderIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/users/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("잘못된 토큰이면 TOKEN_INVALID 401 응답을 반환한다")
    void returnsUnauthorizedWhenTokenIsInvalid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/users/me");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuthenticationFilter.doFilter(request, response, new MockFilterChain());

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(ResponseCode.TOKEN_INVALID.getStatus().value());
        assertThat(body.get("success").asBoolean()).isFalse();
        assertThat(body.get("code").asText()).isEqualTo(ResponseCode.TOKEN_INVALID.getCode());
    }

    @Test
    @DisplayName("만료된 토큰이면 TOKEN_EXPIRED 401 응답을 반환한다")
    void returnsUnauthorizedWhenTokenIsExpired() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/users/me");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + expiredAccessToken(9L));
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuthenticationFilter.doFilter(request, response, new MockFilterChain());

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(ResponseCode.TOKEN_EXPIRED.getStatus().value());
        assertThat(body.get("success").asBoolean()).isFalse();
        assertThat(body.get("code").asText()).isEqualTo(ResponseCode.TOKEN_EXPIRED.getCode());
    }

    private JwtProperties jwtProperties() {
        // 필터 테스트는 parse/verify만 필요하므로 provider가 기대하는 최소 JWT 설정만 채웁니다.
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setAccessSecret(ACCESS_SECRET);
        jwtProperties.setAccessExpirationSeconds(3600L);
        jwtProperties.setRefreshExpirationSeconds(1209600L);
        jwtProperties.setRefreshCookieName("refresh_token");
        jwtProperties.setRefreshCookiePath("/");
        jwtProperties.setRefreshCookieSameSite("Lax");
        jwtProperties.setRefreshCookieSecure(false);
        return jwtProperties;
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
