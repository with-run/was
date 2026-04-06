package kr.withrun.was.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "access token 재발급 응답")
public record ReissueAccessTokenResponse(
        @Schema(description = "새 access token")
        String accessToken,

        @Schema(description = "소셜 로그인 제공자", example = "google")
        String provider,

        @Schema(description = "프로필 완료 여부", example = "true")
        boolean profileCompleted
) {
}
