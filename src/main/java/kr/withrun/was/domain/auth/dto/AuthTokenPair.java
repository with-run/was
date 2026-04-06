package kr.withrun.was.domain.auth.dto;

// 로그인 성공 후 access token과 refresh token을 한 번에 전달하기 위한 DTO
public record AuthTokenPair(
        String accessToken,
        String refreshToken
) {
}
