package kr.withrun.was.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.withrun.was.domain.auth.jwt.JwtProvider;
import kr.withrun.was.domain.auth.oauth2.handler.OAuth2FailureHandler;
import kr.withrun.was.domain.auth.oauth2.handler.OAuth2SuccessHandler;
import kr.withrun.was.domain.auth.oauth2.service.CustomOAuth2UserService;
import kr.withrun.was.domain.auth.security.JwtAuthenticationFilter;
import kr.withrun.was.domain.auth.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration // 스프링 설정 파일임을 선언 (서버 기동 시 우선적으로 읽혀서 적용됨)
@RequiredArgsConstructor // final이 붙은 변수(authProperties)를 자동으로 연결(주입)해주는 롬복의 요술 어노테이션
public class SecurityConfig {

    // application.yml에서 읽어들인 환경설정(프론트엔드 주소 등) 바구니를 가져옵니다.
    private final AuthProperties authProperties;
    private final CustomOAuth2UserService customOAuth2UserService;
    // OAuth 로그인 성공 후 기존/신규 사용자를 처리하는 핸들러
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    // OAuth 로그인 실패 시 에러 처리 핸들러
    private final OAuth2FailureHandler oAuth2FailureHandler;

    // 건물의 메인 경비실(SecurityFilterChain) 규칙을 설정하는 곳
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RestAuthenticationEntryPoint restAuthenticationEntryPoint
    ) throws Exception {
        http
                // 1. CSRF (위조 요청 공격) 방어 기능 끄기
                // 보통 REST API 서버를 만들거나, 별도의 토큰을 쓸 때는 이 기능을 끕니다.
                .csrf(csrf -> csrf.disable())
                
                // 2. CORS (다른 도메인에서의 접속) 허용 설정
                // 바로 아래에 있는 corsConfigurationSource() 메서드의 규칙을 따릅니다.
                .cors(Customizer.withDefaults())
                
                // 3. 세션(임시 보관소) 정책
                // IF_REQUIRED: 스프링이 "필요할 때만" 세션을 만들도록 허락합니다. (소셜 로그인 진행 중에 세션이 필요함)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                )
                // 4. 출입 검문소 규칙 설정 (누구를 들여보내고 누구를 막을지)
                .authorizeHttpRequests(auth -> auth
                        // 소셜 로그인 관련 주소(/oauth2, /login/oauth2 등)는 로그인 안 한 사람도 100% 무사 통과(permitAll)
                        // /mobile/oauth2/** 는 모바일용, /web/oauth2/** 는 origin 을 기억하는 웹용 OAuth 진입점입니다.
                        .requestMatchers("/oauth2/**", "/login/oauth2/**", "/mobile/oauth2/**", "/web/oauth2/**").permitAll()
                        // 개발/운영에서 API 문서와 Swagger UI는 인증 없이 열어둡니다.
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // meta 조회 API는 앱 부트스트랩 단계에서 비인증으로 사용합니다.
                        .requestMatchers(HttpMethod.GET, "/api/meta/**").permitAll()
                        // reward showcase 조회 API는 앱 메인 전시 영역에서 비인증으로 사용합니다.
                        .requestMatchers(HttpMethod.GET, "/api/reward-items/showcase").permitAll()
                        // refresh 재발급과 모바일 code 교환은 로그인 직전 단계라 비인증 접근을 허용합니다.
                        .requestMatchers(HttpMethod.POST, "/api/auth/reissue", "/api/auth/exchange").permitAll()
                        // 현재 사용자 조회는 access token 인증이 필요합니다.
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                        // Step 4부터는 "내 프로필 저장" API만 access token 인증을 반드시 거치게 합니다.
                        .requestMatchers("/api/users/me/**").authenticated()
                        // 로그인 부트스트랩을 제외한 애플리케이션 API는 전부 access token 인증을 요구합니다.
                        .anyRequest().authenticated()
                )
                
                // 5. OAuth2 소셜 로그인 기능 켜주세요! (기능 on)
                .oauth2Login(oauth2 -> oauth2
                    .userInfoEndpoint(userInfo -> userInfo
                        .userService(customOAuth2UserService)
                )
                // 6. 사용자 정보 파싱이 끝난 뒤, 기존/신규 사용자 분기와 redirect 처리 수행
                .successHandler(oAuth2SuccessHandler)
                .failureHandler(oAuth2FailureHandler)
                )
                // UsernamePasswordAuthenticationFilter 전에 JWT를 먼저 읽어야
                // 컨트롤러에 들어갈 때 @AuthenticationPrincipal이 준비됩니다.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);


        return http.build();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtProvider jwtProvider,
            RestAuthenticationEntryPoint restAuthenticationEntryPoint
    ) {
        // 명시적 @Bean으로 등록해두면 실제 애플리케이션에서는 주입받아 쓰고,
        // @WebMvcTest 슬라이스 테스트에서는 이 필터가 자동 스캔되지 않아 기존 테스트를 깨지 않습니다.
        return new JwtAuthenticationFilter(jwtProvider, restAuthenticationEntryPoint);
    }

    @Bean
    public RestAuthenticationEntryPoint restAuthenticationEntryPoint(ObjectMapper objectMapper) {
        // 인증 실패 JSON 직렬화에 공통 ObjectMapper를 재사용합니다.
        return new RestAuthenticationEntryPoint(objectMapper);
    }

    // 🌐 CORS (Cross-Origin Resource Sharing) 세부 규칙 설정
    // 백엔드는 localhost:8080 인데 프론트엔드가 localhost:5173 이면 인터넷 브라우저가 "어? 둘이 주소가 다르네? 해킹 아니야?" 하고 자체단절시키는 방어막을 뚫어주는 합법적 통행증입니다.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 1. 우리 백엔드에 요청을 보낼 수 있는 허락된 프론트엔드 주소들 (yml 파일에서 가져옴)
        configuration.setAllowedOrigins(authProperties.getAllowedOrigins());
        
        // 2. 허락할 HTTP 통신 방법들 (조회, 생성, 수정, 삭제 등 전부 허용)
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        
        // 3. 요청 시 같이 보낼 수 있는 헤더 종류 (전부 허용)
        configuration.setAllowedHeaders(List.of("*"));
        
        // 4. 내장된 쿠키나 세션, 인증 정보(Credentials)를 주고받는 것을 허락! (로그인 처리에 필수)
        configuration.setAllowCredentials(true);

        // 5. 만들어진 이 길가 통행증(규칙)을 백엔드의 "모든 주소(/**)"에 일괄 적용!
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
