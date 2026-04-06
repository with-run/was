package kr.withrun.was.domain.auth.controller;

import kr.withrun.was.global.config.AuthProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.List;

@Controller
@RequestMapping("/web/oauth2")
public class WebOAuthController {

    private static final String OAUTH_ORIGIN_COOKIE_NAME = "withrun_oauth_origin";

    private final AuthProperties authProperties;

    public WebOAuthController(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    @GetMapping("/authorization/{provider}")
    public ResponseEntity<Void> authorizeForWeb(
            @PathVariable String provider,
            @RequestParam String origin
    ) {
        String trimmedOrigin = StringUtils.trimWhitespace(origin);

        // 로그인 시작 origin 은 open redirect 방지를 위해 화이트리스트에 있는 값만 허용합니다.
        if (!isAllowedOrigin(trimmedOrigin)) {
            return ResponseEntity.badRequest().build();
        }

        String redirectPath = UriComponentsBuilder
                .fromPath("/oauth2/authorization/{provider}")
                .buildAndExpand(provider)
                .toUriString();

        // OAuth provider 를 다녀온 뒤에도 어떤 웹 도메인으로 돌아가야 하는지 기억해둡니다.
        ResponseCookie originCookie = ResponseCookie.from(OAUTH_ORIGIN_COOKIE_NAME, trimmedOrigin)
                .httpOnly(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofMinutes(10))
                .build();

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.SET_COOKIE, originCookie.toString())
                .location(URI.create(redirectPath))
                .build();
    }

    private boolean isAllowedOrigin(String origin) {
        if (!StringUtils.hasText(origin)) {
            return false;
        }

        List<String> allowedOrigins = authProperties.getAllowedOrigins();
        return allowedOrigins.contains(origin);
    }
}
