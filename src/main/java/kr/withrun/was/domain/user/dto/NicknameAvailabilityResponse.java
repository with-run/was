package kr.withrun.was.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "닉네임 사용 가능 여부 응답")
public record NicknameAvailabilityResponse(
        @Schema(description = "현재 로그인 사용자가 해당 닉네임을 사용할 수 있는지 여부", example = "true")
        boolean available
) {
}
