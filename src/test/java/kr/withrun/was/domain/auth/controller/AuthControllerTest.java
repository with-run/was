package kr.withrun.was.domain.auth.controller;

import kr.withrun.was.domain.auth.dto.AuthExchangeResult;
import kr.withrun.was.domain.auth.dto.AuthMeResponse;
import kr.withrun.was.domain.auth.dto.ReissueAccessTokenResponse;
import kr.withrun.was.domain.auth.jwt.JwtProvider;
import kr.withrun.was.domain.auth.service.AuthService;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.type.Gender;
import kr.withrun.was.global.response.ResponseCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        properties = {
                "spring.config.import=",
                "spring.cloud.aws.parameterstore.enabled=false"
        }
)
@AutoConfigureMockMvc
@DisplayName("AuthController")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("유효한 access token이면 현재 사용자 정보를 반환한다")
    void returnsCurrentUserWhenAccessTokenIsValid() throws Exception {
        given(authService.getCurrentUser(any())).willReturn(
                new AuthMeResponse(
                        10L,
                        "google",
                        true,
                        "runner",
                        LocalDate.of(1998, 4, 5),
                        Gender.FEMALE,
                        165.5,
                        52.3
                )
        );

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(10L))
                .andExpect(jsonPath("$.data.provider").value("google"))
                .andExpect(jsonPath("$.data.profileCompleted").value(true));

        then(authService).should().getCurrentUser(any());
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 현재 사용자 조회는 인증 필요 응답을 반환한다")
    void returnsUnauthorizedWhenAuthorizationHeaderIsMissing() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.UNAUTHORIZED.getCode()));

        then(authService).should(never()).getCurrentUser(any());
    }

    @Test
    @DisplayName("refresh cookie가 있으면 access token 재발급 응답을 반환한다")
    void reissuesAccessTokenWhenRefreshCookieExists() throws Exception {
        given(authService.reissueAccessToken(any())).willReturn(
                new ReissueAccessTokenResponse("new-access-token", "kakao", false)
        );

        mockMvc.perform(post("/api/auth/reissue")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.data.provider").value("kakao"))
                .andExpect(jsonPath("$.data.profileCompleted").value(false));

        then(authService).should().reissueAccessToken(any());
    }

    @Test
    @DisplayName("mobile auth code를 교환하면 access token과 refresh cookie를 반환한다")
    void exchangesMobileAuthCodeAndReturnsRefreshCookie() throws Exception {
        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", "refresh-token")
                .httpOnly(true)
                .path("/")
                .sameSite("None")
                .build();
        given(authService.exchangeMobileAuthCode("mobile-code")).willReturn(
                new AuthExchangeResult("new-access-token", refreshCookie)
        );

        mockMvc.perform(post("/api/auth/exchange")
                        .contentType("application/json")
                        .content("""
                                {"code":"mobile-code"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refresh_token=refresh-token")));

        then(authService).should().exchangeMobileAuthCode("mobile-code");
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 로그아웃은 인증 필요 응답을 반환한다")
    void logoutRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.UNAUTHORIZED.getCode()));

        then(authService).should(never()).expireRefreshTokenCookie();
    }

    @Test
    @DisplayName("유효한 access token이면 로그아웃은 refresh cookie 만료 헤더를 내려준다")
    void logoutExpiresRefreshCookie() throws Exception {
        ResponseCookie expiredCookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .path("/")
                .sameSite("None")
                .maxAge(0)
                .build();
        given(authService.expireRefreshTokenCookie()).willReturn(expiredCookie);

        mockMvc.perform(post("/api/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.USER_LOGGED_OUT.getCode()))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refresh_token=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=None")));

        then(authService).should().expireRefreshTokenCookie();
    }

    private String bearerToken(Long userId) {
        return "Bearer " + jwtProvider.generateTokenPair(completedUser(userId), "google").accessToken();
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
