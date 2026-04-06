package kr.withrun.was.domain.auth.oauth2.handler;

import kr.withrun.was.domain.auth.dto.AuthTokenPair;
import kr.withrun.was.domain.auth.jwt.JwtProvider;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.entity.UserAuthAccount;
import kr.withrun.was.domain.user.repository.UserAuthAccountRepository;
import kr.withrun.was.domain.user.repository.UserRepository;
import kr.withrun.was.domain.user.type.Gender;
import kr.withrun.was.global.config.AuthProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth2SuccessHandler")
class OAuth2SuccessHandlerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserAuthAccountRepository userAuthAccountRepository;

    @Mock
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("reuses existing user and redirects to the frontend callback path")
    void reusesExistingUserAndRedirectsToFrontendCallbackPath() throws Exception {
        // access token 은 URL 쿼리로 넘기지 않고, callback 화면이 refresh cookie 기반으로 다시 복구한다.
        AuthProperties authProperties = new AuthProperties();
        authProperties.setFrontendBaseUrl("http://localhost:5173");
        OAuth2SuccessHandler oAuth2SuccessHandler = new OAuth2SuccessHandler(
                userRepository,
                userAuthAccountRepository,
                authProperties,
                jwtProvider
        );

        User existingUser = completedUser(1L);
        UserAuthAccount authAccount = UserAuthAccount.create(
                existingUser,
                "google",
                "google-user-123",
                "runner@example.com",
                "https://image.example.com/profile.png"
        );
        AuthTokenPair tokenPair = new AuthTokenPair("access-token", "refresh-token");
        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", "refresh-token")
                .httpOnly(true)
                .path("/")
                .sameSite("None")
                .build();

        when(userAuthAccountRepository.findByProviderAndProviderUserId("google", "google-user-123"))
                .thenReturn(Optional.of(authAccount));
        when(jwtProvider.generateTokenPair(existingUser, "google")).thenReturn(tokenPair);
        when(jwtProvider.createRefreshTokenCookie("refresh-token")).thenReturn(refreshCookie);

        MockHttpServletResponse response = new MockHttpServletResponse();

        oAuth2SuccessHandler.onAuthenticationSuccess(
                new MockHttpServletRequest(),
                response,
                authenticationToken(Map.of(
                        "sub", "google-user-123",
                        "email", "runner@example.com",
                        "picture", "https://image.example.com/profile.png"
                ))
        );

        assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:5173/auth/callback");
        assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).isEqualTo(refreshCookie.toString());

        verify(userRepository, never()).save(any(User.class));
        verify(userAuthAccountRepository, never()).save(any(UserAuthAccount.class));
    }

    @Test
    @DisplayName("creates pending user for first login and redirects to the shared callback path")
    void createsPendingUserForFirstLoginAndRedirectsToSharedCallbackPath() throws Exception {
        AuthProperties authProperties = new AuthProperties();
        authProperties.setFrontendBaseUrl("http://localhost:5173");
        OAuth2SuccessHandler oAuth2SuccessHandler = new OAuth2SuccessHandler(
                userRepository,
                userAuthAccountRepository,
                authProperties,
                jwtProvider
        );

        User savedUser = User.createPendingSocialUser();
        setField(savedUser, "id", 2L);

        AuthTokenPair tokenPair = new AuthTokenPair("access-token", "refresh-token");
        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", "refresh-token")
                .httpOnly(true)
                .path("/")
                .sameSite("None")
                .build();

        when(userAuthAccountRepository.findByProviderAndProviderUserId("google", "new-google-user"))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtProvider.generateTokenPair(savedUser, "google")).thenReturn(tokenPair);
        when(jwtProvider.createRefreshTokenCookie("refresh-token")).thenReturn(refreshCookie);

        MockHttpServletResponse response = new MockHttpServletResponse();

        oAuth2SuccessHandler.onAuthenticationSuccess(
                new MockHttpServletRequest(),
                response,
                authenticationToken(Map.of(
                        "sub", "new-google-user",
                        "email", "new@example.com",
                        "picture", "https://image.example.com/new.png"
                ))
        );

        assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:5173/auth/callback");
        assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).isEqualTo(refreshCookie.toString());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().isProfileCompleted()).isFalse();
        assertThat(userCaptor.getValue().getNickname())
                .isNotBlank()
                .hasSize(16);
        assertThat(userCaptor.getValue().getBirthDate()).isEqualTo(LocalDate.of(1900, 1, 1));
        assertThat(userCaptor.getValue().getGender()).isEqualTo(Gender.MALE);
        assertThat(userCaptor.getValue().getHeight()).isEqualTo(1.0);
        assertThat(userCaptor.getValue().getWeight()).isEqualTo(1.0);

        ArgumentCaptor<UserAuthAccount> authAccountCaptor = ArgumentCaptor.forClass(UserAuthAccount.class);
        verify(userAuthAccountRepository).save(authAccountCaptor.capture());
        assertThat(authAccountCaptor.getValue().getUser()).isSameAs(savedUser);
        assertThat(authAccountCaptor.getValue().getProvider()).isEqualTo("google");
        assertThat(authAccountCaptor.getValue().getProviderUserId()).isEqualTo("new-google-user");
        assertThat(authAccountCaptor.getValue().getEmail()).isEqualTo("new@example.com");
        assertThat(authAccountCaptor.getValue().getProfileImageUrl()).isEqualTo("https://image.example.com/new.png");
    }

    @Test
    @DisplayName("redirects mobile login to the app deep link with a short-lived auth code")
    void redirectsMobileLoginToAppDeepLinkWithShortLivedAuthCode() throws Exception {
        AuthProperties authProperties = new AuthProperties();
        authProperties.setFrontendBaseUrl("http://localhost:5173");
        authProperties.setMobileDeepLinkBase("withrun://auth/callback");
        OAuth2SuccessHandler oAuth2SuccessHandler = new OAuth2SuccessHandler(
                userRepository,
                userAuthAccountRepository,
                authProperties,
                jwtProvider
        );

        User existingUser = completedUser(1L);
        UserAuthAccount authAccount = UserAuthAccount.create(
                existingUser,
                "google",
                "google-user-123",
                "runner@example.com",
                "https://image.example.com/profile.png"
        );

        when(userAuthAccountRepository.findByProviderAndProviderUserId("google", "google-user-123"))
                .thenReturn(Optional.of(authAccount));
        when(jwtProvider.generateMobileAuthCode(existingUser, "google")).thenReturn("mobile-auth-code");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new jakarta.servlet.http.Cookie("withrun_oauth_target", "mobile"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        oAuth2SuccessHandler.onAuthenticationSuccess(
                request,
                response,
                authenticationToken(Map.of(
                        "sub", "google-user-123",
                        "email", "runner@example.com",
                        "picture", "https://image.example.com/profile.png"
                ))
        );

        assertThat(response.getRedirectedUrl())
                .isEqualTo("withrun://auth/callback?code=mobile-auth-code");

        verify(jwtProvider).generateMobileAuthCode(existingUser, "google");
        verify(jwtProvider, never()).createRefreshTokenCookie(any());
    }

    private OAuth2AuthenticationToken authenticationToken(Map<String, Object> attributes) {
        DefaultOAuth2User principal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "sub"
        );

        return new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "google");
    }

    private User completedUser(Long id) {
        User user = User.createPendingSocialUser();
        setField(user, "id", id);
        user.completeProfile(
                "runner",
                LocalDate.of(1997, 8, 9),
                Gender.FEMALE,
                166.0,
                54.0
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
