package kr.withrun.was.domain.auth.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.withrun.was.domain.auth.jwt.JwtProvider;
import kr.withrun.was.global.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// 모든 API 요청에서 Authorization 헤더의 Bearer access token을 읽어 현재 사용자를 식별합니다.
// 토큰이 없을 때는 그냥 통과시키고, 토큰이 잘못됐을 때만 401 JSON을 직접 반환하는 것이 핵심 동작입니다.
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        // 헤더 자체가 없다면 "익명 요청"으로 보고 그대로 다음 필터로 넘깁니다.
        // 실제 접근 제어는 SecurityConfig의 authenticated() 규칙이 맡습니다.
        if (!StringUtils.hasText(authorizationHeader)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Bearer 스킴이 아니면 access token 전달 방식 자체가 잘못된 것이므로 즉시 401을 반환합니다.
        if (!authorizationHeader.startsWith("Bearer ")) {
            commenceWith(request, response, ResponseCode.TOKEN_INVALID, "bearer token is required", null);
            return;
        }

        String accessToken = authorizationHeader.substring(7).trim();
        // "Bearer " 뒤가 비어 있는 경우도 잘못된 토큰으로 처리합니다.
        if (!StringUtils.hasText(accessToken)) {
            commenceWith(request, response, ResponseCode.TOKEN_INVALID, "access token is blank", null);
            return;
        }

        // OAuth 로그인 직후에는 세션 기반 인증 객체가 먼저 들어올 수 있습니다.
        // 이 경우에도 Authorization 헤더가 있으면 JWT 인증을 우선 적용해야
        // API 컨트롤러에서 @AuthenticationPrincipal AuthenticatedUser를 안정적으로 받을 수 있습니다.
        Authentication existingAuthentication = SecurityContextHolder.getContext().getAuthentication();
        if (existingAuthentication != null
                && existingAuthentication.getPrincipal() instanceof AuthenticatedUser) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            AuthenticatedUser authenticatedUser = jwtProvider.extractAuthenticatedUser(accessToken);
            // 현재 단계에서는 role 기반 권한 분기가 없지만, authenticated() 규칙을 통과할 수 있도록
            // 최소한의 ROLE_USER 권한을 함께 넣어 SecurityContext에 저장합니다.
            UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                    authenticatedUser,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException exception) {
            SecurityContextHolder.clearContext();
            // 만료는 위조와 다르게 별도 코드를 내려주면 프런트가 재발급 분기를 태우기 쉬워집니다.
            commenceWith(request, response, ResponseCode.TOKEN_EXPIRED, "access token expired", exception);
        } catch (JwtAuthenticationException exception) {
            SecurityContextHolder.clearContext();
            restAuthenticationEntryPoint.commence(request, response, exception);
        } catch (JwtException | IllegalArgumentException exception) {
            SecurityContextHolder.clearContext();
            // 서명 위조, 형식 오류, refresh token 전달 등은 모두 유효하지 않은 토큰으로 묶어 응답합니다.
            commenceWith(request, response, ResponseCode.TOKEN_INVALID, "access token invalid", exception);
        }
    }

    private void commenceWith(
            HttpServletRequest request,
            HttpServletResponse response,
            ResponseCode responseCode,
            String message,
            Throwable cause
    ) throws IOException, ServletException {
        // ResponseCode를 보존한 예외로 감싸서 EntryPoint가 동일한 JSON 포맷으로 응답하게 합니다.
        restAuthenticationEntryPoint.commence(
                request,
                response,
                new JwtAuthenticationException(responseCode, message, cause)
        );
    }
}
