package kr.withrun.was.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.http.ResponseCookie;

// access token은 body로, refresh cookie는 헤더로 내려야 해서 둘을 함께 들고 다니는 응답 모델입니다.
public record AuthExchangeResult(
        String accessToken,
        @JsonIgnore ResponseCookie refreshTokenCookie
) {
}
