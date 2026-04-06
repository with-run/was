package kr.withrun.was.domain.auth.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.net.URI;

@Controller
@RequestMapping("/mobile/oauth2")
public class MobileOAuthController {

    private static final String OAUTH_TARGET_COOKIE_NAME = "withrun_oauth_target";

    @GetMapping("/authorization/{provider}")
    public ResponseEntity<Void> authorizeForMobile(@PathVariable String provider) {
        // 실제 OAuth 시작은 기존 /oauth2/authorization/{provider} 로 넘기고,
        // 그 전에 "이번 로그인은 모바일에서 시작했다"는 힌트 쿠키만 심어둡니다.
        String redirectPath = UriComponentsBuilder
                .fromPath("/oauth2/authorization/{provider}")
                .buildAndExpand(provider)
                .toUriString();

        ResponseCookie targetCookie = ResponseCookie.from(OAUTH_TARGET_COOKIE_NAME, "mobile")
                .httpOnly(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofMinutes(10))
                .build();

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.SET_COOKIE, targetCookie.toString())
                .location(URI.create(redirectPath))
                .build();
    }
}
