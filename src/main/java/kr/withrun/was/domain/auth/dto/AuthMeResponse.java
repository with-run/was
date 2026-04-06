package kr.withrun.was.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.withrun.was.domain.auth.security.AuthenticatedUser;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.type.Gender;

import java.time.LocalDate;

@Schema(description = "현재 로그인 사용자 조회 응답")
public record AuthMeResponse(
        @Schema(description = "현재 사용자 ID", example = "10")
        Long userId,

        @Schema(description = "소셜 로그인 제공자", example = "google")
        String provider,

        @Schema(description = "프로필 완료 여부", example = "false")
        boolean profileCompleted,

        @Schema(description = "닉네임", example = "runner", nullable = true)
        String nickname,

        @Schema(description = "생년월일", example = "1998-04-05", nullable = true)
        LocalDate birthDate,

        @Schema(description = "성별", implementation = Gender.class, nullable = true)
        Gender gender,

        @Schema(description = "키(cm)", example = "165.5", nullable = true)
        Double height,

        @Schema(description = "몸무게(kg)", example = "52.3", nullable = true)
        Double weight
) {
    public static AuthMeResponse from(User user, AuthenticatedUser authenticatedUser) {
        boolean profileCompleted = user.isProfileCompleted();

        // pending user는 DB 필수값 때문에 임시 데이터를 들고 있어도, 프론트에는 아직 비어있는 프로필처럼 보여줍니다.
        return new AuthMeResponse(
                user.getId(),
                authenticatedUser.provider(),
                profileCompleted,
                profileCompleted ? user.getNickname() : null,
                profileCompleted ? user.getBirthDate() : null,
                profileCompleted ? user.getGender() : null,
                profileCompleted ? user.getHeight() : null,
                profileCompleted ? user.getWeight() : null
        );
    }
}
