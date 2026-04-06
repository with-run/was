package kr.withrun.was.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import kr.withrun.was.domain.auth.security.AuthenticatedUser;
import kr.withrun.was.domain.auth.service.AuthService;
import kr.withrun.was.domain.user.dto.NicknameAvailabilityResponse;
import kr.withrun.was.domain.user.dto.UpdateUserProfileRequest;
import kr.withrun.was.domain.user.dto.UserProfileResponse;
import kr.withrun.was.domain.user.service.UserProfileService;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ApiResponse;
import kr.withrun.was.global.response.ResponseCode;
import kr.withrun.was.global.response.swagger.ErrorApiResponseDoc;
import kr.withrun.was.global.response.swagger.SwaggerExampleValues;
import kr.withrun.was.global.response.swagger.UserSuccessApiResponseDocs;
import kr.withrun.was.global.response.swagger.ValidationErrorApiResponseDoc;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User Profile", description = "현재 로그인한 사용자 프로필 조회 및 수정 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final AuthService authService;

    @Operation(
            summary = "닉네임을 사용할 수 있는지 확인한다",
            description = "인증 없이 nickname 하나만 받아 현재 활성 사용자 기준으로 사용 가능한 닉네임인지 확인한다.",
            parameters = {
                    @Parameter(
                            name = "nickname",
                            in = ParameterIn.QUERY,
                            required = true,
                            description = "사용 가능 여부를 확인할 닉네임",
                            example = "runner-next"
                    )
            }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "닉네임 사용 가능 여부 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserSuccessApiResponseDocs.UserNicknameAvailabilityApiResponse.class),
                            examples = @ExampleObject(value = SwaggerExampleValues.USER_NICKNAME_AVAILABILITY_SUCCESS)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "닉네임 값이 비어 있거나 길이 제한을 초과함",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorApiResponseDoc.class),
                            examples = @ExampleObject(value = SwaggerExampleValues.VALIDATION_ERROR_USER_PROFILE)
                    )
            )
    })
    @GetMapping("/nickname-availability")
    public ResponseEntity<ApiResponse<NicknameAvailabilityResponse>> getNicknameAvailability(
            @RequestParam String nickname
    ) {
        return ApiResponse.successEntity(userProfileService.getNicknameAvailability(nickname));
    }

    @Operation(
            summary = "현재 로그인 사용자의 프로필을 조회한다",
            description = "Authorization 헤더의 Bearer access token을 기준으로 현재 로그인한 사용자의 프로필 정보를 조회한다.",
            parameters = {
                    @Parameter(
                            name = HttpHeaders.AUTHORIZATION,
                            in = ParameterIn.HEADER,
                            required = true,
                            description = "현재 로그인 사용자를 식별하는 Bearer access token",
                            example = "Bearer eyJhbGciOiJIUzI1NiJ9.access-token"
                    )
            }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "프로필 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserSuccessApiResponseDocs.UserProfileApiResponse.class),
                            examples = @ExampleObject(value = SwaggerExampleValues.USER_PROFILE_SUCCESS)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증이 없거나 access token이 유효하지 않음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorApiResponseDoc.class),
                            examples = {
                                    @ExampleObject(name = "인증 없음", value = SwaggerExampleValues.ERROR_UNAUTHORIZED),
                                    @ExampleObject(name = "유효하지 않은 토큰", value = SwaggerExampleValues.ERROR_TOKEN_INVALID)
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorApiResponseDoc.class),
                            examples = @ExampleObject(value = SwaggerExampleValues.ERROR_USER_NOT_FOUND)
                    )
            )
    })
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ApiResponse.successEntity(
                userProfileService.getProfile(resolveUserId(authenticatedUser))
        );
    }

    @Operation(
            summary = "현재 로그인 사용자의 프로필을 수정한다",
            description = "Authorization 헤더의 Bearer access token을 기준으로 현재 로그인한 사용자의 프로필 정보를 수정한다.",
            parameters = {
                    @Parameter(
                            name = HttpHeaders.AUTHORIZATION,
                            in = ParameterIn.HEADER,
                            required = true,
                            description = "현재 로그인 사용자를 식별하는 Bearer access token",
                            example = "Bearer eyJhbGciOiJIUzI1NiJ9.access-token"
                    )
            }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "프로필 수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserSuccessApiResponseDocs.UserProfileUpdateApiResponse.class),
                            examples = @ExampleObject(value = SwaggerExampleValues.USER_PROFILE_UPDATE_SUCCESS)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 본문 형식 또는 값이 유효하지 않음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ValidationErrorApiResponseDoc.class),
                            examples = @ExampleObject(value = SwaggerExampleValues.VALIDATION_ERROR_USER_PROFILE)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증이 없거나 access token이 유효하지 않음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorApiResponseDoc.class),
                            examples = {
                                    @ExampleObject(name = "인증 없음", value = SwaggerExampleValues.ERROR_UNAUTHORIZED),
                                    @ExampleObject(name = "유효하지 않은 토큰", value = SwaggerExampleValues.ERROR_TOKEN_INVALID)
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorApiResponseDoc.class),
                            examples = @ExampleObject(value = SwaggerExampleValues.ERROR_USER_NOT_FOUND)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 사용 중인 닉네임임",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorApiResponseDoc.class),
                            examples = @ExampleObject(value = SwaggerExampleValues.ERROR_NICKNAME_ALREADY_EXISTS)
                    )
            )
    })
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<Void>> updateProfile(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "수정할 현재 로그인 사용자 프로필 정보",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UpdateUserProfileRequest.class),
                            examples = @ExampleObject(value = SwaggerExampleValues.USER_PROFILE_UPDATE_REQUEST)
                    )
            )
            @org.springframework.web.bind.annotation.RequestBody UpdateUserProfileRequest request
    ) {
        userProfileService.updateProfile(resolveUserId(authenticatedUser), request);
        return ApiResponse.successEntity(ResponseCode.OK);
    }

    @Operation(
            summary = "현재 로그인 사용자를 회원탈퇴 처리한다",
            description = "Authorization 헤더의 Bearer access token을 기준으로 현재 로그인한 사용자를 soft delete 처리하고 refresh token 쿠키를 만료시킨다.",
            parameters = {
                    @Parameter(
                            name = HttpHeaders.AUTHORIZATION,
                            in = ParameterIn.HEADER,
                            required = true,
                            description = "현재 로그인 사용자를 식별하는 Bearer access token",
                            example = "Bearer eyJhbGciOiJIUzI1NiJ9.access-token"
                    )
            }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "회원탈퇴 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserSuccessApiResponseDocs.UserDeleteApiResponse.class),
                            examples = @ExampleObject(value = SwaggerExampleValues.USER_DELETE_SUCCESS)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증이 없거나 access token이 유효하지 않음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorApiResponseDoc.class),
                            examples = {
                                    @ExampleObject(name = "인증 없음", value = SwaggerExampleValues.ERROR_UNAUTHORIZED),
                                    @ExampleObject(name = "유효하지 않은 토큰", value = SwaggerExampleValues.ERROR_TOKEN_INVALID)
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorApiResponseDoc.class),
                            examples = @ExampleObject(value = SwaggerExampleValues.ERROR_USER_NOT_FOUND)
                    )
            )
    })
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteProfile(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Parameter(hidden = true) HttpServletResponse response
    ) {
        // 탈퇴 직후 브라우저가 기존 refresh cookie로 다시 세션을 복구하지 못하도록 쿠키도 함께 만료시킨다.
        userProfileService.deleteProfile(resolveUserId(authenticatedUser));
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                authService.expireRefreshTokenCookie().toString()
        );
        return ApiResponse.successEntity(ResponseCode.USER_DELETED);
    }

    private Long resolveUserId(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.userId() == null) {
            throw new CustomException(ResponseCode.UNAUTHORIZED);
        }

        return authenticatedUser.userId();
    }
}
