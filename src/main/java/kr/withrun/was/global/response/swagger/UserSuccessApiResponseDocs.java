package kr.withrun.was.global.response.swagger;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.withrun.was.domain.user.dto.NicknameAvailabilityResponse;
import kr.withrun.was.domain.user.dto.UserProfileResponse;
import kr.withrun.was.domain.user.dto.UserRunningPreferenceResponse;

public final class UserSuccessApiResponseDocs {

    private UserSuccessApiResponseDocs() {
    }

    @Schema(name = "UserProfileApiResponse", description = "현재 로그인 사용자 프로필 조회 성공 응답")
    public record UserProfileApiResponse(
            @Schema(description = "요청 성공 여부", example = "true")
            boolean success,
            @Schema(description = "성공 응답 코드", example = "S001")
            String code,
            @Schema(description = "성공 응답 메시지", example = "요청이 성공했습니다.")
            String message,
            @Schema(description = "현재 로그인 사용자 프로필 데이터")
            UserProfileResponse data
    ) {
    }

    @Schema(name = "UserProfileUpdateApiResponse", description = "현재 로그인 사용자 프로필 수정 성공 응답")
    public record UserProfileUpdateApiResponse(
            @Schema(description = "요청 성공 여부", example = "true")
            boolean success,
            @Schema(description = "성공 응답 코드", example = "S001")
            String code,
            @Schema(description = "성공 응답 메시지", example = "요청이 성공했습니다.")
            String message
    ) {
    }

    @Schema(name = "UserNicknameAvailabilityApiResponse", description = "현재 로그인 사용자 닉네임 사용 가능 여부 조회 성공 응답")
    public record UserNicknameAvailabilityApiResponse(
            @Schema(description = "요청 성공 여부", example = "true")
            boolean success,
            @Schema(description = "성공 응답 코드", example = "S001")
            String code,
            @Schema(description = "성공 응답 메시지", example = "요청이 성공했습니다.")
            String message,
            @Schema(description = "닉네임 사용 가능 여부 데이터")
            NicknameAvailabilityResponse data
    ) {
    }

    // 회원탈퇴는 추가 데이터 없이 성공 여부만 내려주기 때문에 별도 래퍼 문서를 둡니다.
    @Schema(name = "UserDeleteApiResponse", description = "현재 로그인 사용자 회원탈퇴 성공 응답")
    public record UserDeleteApiResponse(
            @Schema(description = "요청 성공 여부", example = "true")
            boolean success,
            @Schema(description = "성공 응답 코드", example = "S103")
            String code,
            @Schema(description = "성공 응답 메시지", example = "회원탈퇴가 완료되었습니다.")
            String message
    ) {
    }

    @Schema(name = "UserRunningPreferenceApiResponse", description = "현재 로그인 사용자 러닝 선호 설정 조회 성공 응답")
    public record UserRunningPreferenceApiResponse(
            @Schema(description = "요청 성공 여부", example = "true")
            boolean success,
            @Schema(description = "성공 응답 코드", example = "S001")
            String code,
            @Schema(description = "성공 응답 메시지", example = "요청이 성공했습니다.")
            String message,
            @Schema(description = "현재 로그인 사용자 러닝 선호 설정 데이터")
            UserRunningPreferenceResponse data
    ) {
    }

    @Schema(name = "UserRunningPreferenceUpdateApiResponse", description = "현재 로그인 사용자 러닝 선호 설정 수정 성공 응답")
    public record UserRunningPreferenceUpdateApiResponse(
            @Schema(description = "요청 성공 여부", example = "true")
            boolean success,
            @Schema(description = "성공 응답 코드", example = "S001")
            String code,
            @Schema(description = "성공 응답 메시지", example = "요청이 성공했습니다.")
            String message
    ) {
    }
}
