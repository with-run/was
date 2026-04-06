package kr.withrun.was.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Positive;
import kr.withrun.was.domain.user.type.Gender;

import java.time.LocalDate;

// 소셜 로그인 직후 온보딩과 이후 프로필 수정을 같은 요청 포맷으로 받기 위한 DTO입니다.
// 필드 제약은 "컨트롤러 레벨에서 걸러낼 수 있는 형식 오류"를 최대한 먼저 막는 역할을 합니다.
@Schema(description = "내 프로필 수정 요청")
public record UpdateUserProfileRequest(
        @Schema(description = "닉네임", example = "runner", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String nickname,

        @Schema(description = "생년월일", example = "1998-04-05", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @Past
        LocalDate birthDate,

        @Schema(description = "성별", example = "FEMALE", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        Gender gender,

        @Schema(description = "키(cm)", example = "165.5", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @Positive
        Double height,

        @Schema(description = "몸무게(kg)", example = "52.3", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @Positive
        Double weight
) {
}
