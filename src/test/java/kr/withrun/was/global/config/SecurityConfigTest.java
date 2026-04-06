package kr.withrun.was.global.config;

import kr.withrun.was.domain.auth.dto.AuthExchangeResult;
import kr.withrun.was.domain.auth.dto.ReissueAccessTokenResponse;
import kr.withrun.was.domain.auth.service.AuthService;
import kr.withrun.was.domain.reward.dto.RewardItemShowcaseListResponse;
import kr.withrun.was.domain.reward.service.RewardItemQueryService;
import kr.withrun.was.global.response.ResponseCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        properties = {
                "spring.config.import=",
                "spring.cloud.aws.parameterstore.enabled=false",
                "app.auth.allowed-origins[0]=http://localhost:5173"
        }
)
@AutoConfigureMockMvc
@DisplayName("SecurityConfig")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private RewardItemQueryService rewardItemQueryService;

    @ParameterizedTest(name = "비인증 요청 차단: {0}")
    @MethodSource("authenticatedRoutes")
    @DisplayName("로그인 부트스트랩이 아닌 API는 인증이 필요하다")
    void requiresAuthenticationForApplicationApis(String ignored, MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.UNAUTHORIZED.getCode()));
    }

    @Test
    @DisplayName("Spring OAuth 진입점은 공개 상태를 유지한다")
    void keepsSpringOAuthEndpointsPublic() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(HttpHeaders.LOCATION, org.hamcrest.Matchers.containsString("accounts.google.com")));
    }

    @Test
    @DisplayName("웹 OAuth 진입점은 공개 상태를 유지한다")
    void keepsWebOAuthEndpointPublic() throws Exception {
        mockMvc.perform(get("/web/oauth2/authorization/google")
                        .queryParam("origin", "http://localhost:5173"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(HttpHeaders.LOCATION, org.hamcrest.Matchers.containsString("/oauth2/authorization/google")));
    }

    @Test
    @DisplayName("모바일 OAuth 진입점은 공개 상태를 유지한다")
    void keepsMobileOAuthEndpointPublic() throws Exception {
        mockMvc.perform(get("/mobile/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(HttpHeaders.LOCATION, org.hamcrest.Matchers.containsString("/oauth2/authorization/google")));
    }

    @Test
    @DisplayName("모바일 auth code 교환은 access token 없이도 허용한다")
    void keepsExchangeEndpointPublic() throws Exception {
        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", "refresh-token")
                .httpOnly(true)
                .path("/")
                .sameSite("None")
                .build();
        given(authService.exchangeMobileAuthCode("mobile-code"))
                .willReturn(new AuthExchangeResult("new-access-token", refreshCookie));

        mockMvc.perform(post("/api/auth/exchange")
                        .contentType("application/json")
                        .content("""
                                {"code":"mobile-code"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("refresh token 재발급은 access token 없이도 허용한다")
    void keepsReissueEndpointPublic() throws Exception {
        given(authService.reissueAccessToken(any()))
                .willReturn(new ReissueAccessTokenResponse("new-access-token", "google", true));

        mockMvc.perform(post("/api/auth/reissue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("meta API 는 access token 없이도 허용한다")
    void keepsMetaEndpointsPublic() throws Exception {
        mockMvc.perform(get("/api/meta/course/register"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("reward showcase API 는 access token 없이도 허용한다")
    void keepsRewardShowcaseEndpointPublic() throws Exception {
        given(rewardItemQueryService.getRewardItemShowcase())
                .willReturn(new RewardItemShowcaseListResponse(List.of()));

        mockMvc.perform(get("/api/reward-items/showcase"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> authenticatedRoutes() {
        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.of(
                        "course detail",
                        get("/api/courses/12")
                ),
                org.junit.jupiter.params.provider.Arguments.of(
                        "nickname availability",
                        get("/api/users/nickname-availability").queryParam("nickname", "runner-next")
                ),
                org.junit.jupiter.params.provider.Arguments.of(
                        "running session register course",
                        post("/api/running-sessions/91/register-course")
                                .contentType("application/json")
                                .content("""
                                        {
                                          "title": "한강 야간 러닝 10K",
                                          "mode": "COMMUNITY",
                                          "difficulty": "MEDIUM",
                                          "courseTypes": ["RIVERSIDE", "URBAN"],
                                          "routeType": "LOOP"
                                        }
                                        """)
                ),
                org.junit.jupiter.params.provider.Arguments.of(
                        "logout",
                        post("/api/auth/logout")
                )
        );
    }
}
