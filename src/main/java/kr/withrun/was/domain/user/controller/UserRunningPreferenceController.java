package kr.withrun.was.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.withrun.was.domain.auth.security.AuthenticatedUser;
import kr.withrun.was.domain.user.dto.UpdateUserRunningPreferenceRequest;
import kr.withrun.was.domain.user.dto.UserRunningPreferenceResponse;
import kr.withrun.was.domain.user.service.UserRunningPreferenceService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User Running Preference", description = "현재 로그인한 사용자 러닝 선호 설정 조회 및 수정 API")
@RestController
@RequestMapping("/api/users/me/running-preferences")
@RequiredArgsConstructor
public class UserRunningPreferenceController {

    private final UserRunningPreferenceService userRunningPreferenceService;

    @Operation(
            summary = "현재 로그인 사용자의 러닝 선호 설정을 조회한다",
            description = "Authorization 헤더의 Bearer access token을 기준으로 현재 로그인한 사용자의 러닝 기본 설정을 조회한다.",
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
                    description = "러닝 선호 설정 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserSuccessApiResponseDocs.UserRunningPreferenceApiResponse.class),
                            examples = @ExampleObject(value = SwaggerExampleValues.USER_RUNNING_PREFERENCE_SUCCESS)
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
    @GetMapping
    public ResponseEntity<ApiResponse<UserRunningPreferenceResponse>> getRunningPreference(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ApiResponse.successEntity(
                userRunningPreferenceService.getRunningPreference(resolveUserId(authenticatedUser))
        );
    }

    @Operation(
            summary = "현재 로그인 사용자의 러닝 선호 설정을 수정한다",
            description = "Authorization 헤더의 Bearer access token을 기준으로 현재 로그인한 사용자의 러닝 기본 설정을 저장하거나 갱신한다.",
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
                    description = "러닝 선호 설정 수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserSuccessApiResponseDocs.UserRunningPreferenceUpdateApiResponse.class),
                            examples = @ExampleObject(value = SwaggerExampleValues.USER_RUNNING_PREFERENCE_UPDATE_SUCCESS)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 본문 형식 또는 값이 유효하지 않음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ValidationErrorApiResponseDoc.class),
                            examples = @ExampleObject(value = SwaggerExampleValues.VALIDATION_ERROR_USER_RUNNING_PREFERENCE)
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
    @PutMapping
    public ResponseEntity<ApiResponse<Void>> updateRunningPreference(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "저장할 현재 로그인 사용자 러닝 선호 설정",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UpdateUserRunningPreferenceRequest.class),
                            examples = @ExampleObject(value = SwaggerExampleValues.USER_RUNNING_PREFERENCE_UPDATE_REQUEST)
                    )
            )
            @org.springframework.web.bind.annotation.RequestBody UpdateUserRunningPreferenceRequest request
    ) {
        userRunningPreferenceService.updateRunningPreference(resolveUserId(authenticatedUser), request);
        return ApiResponse.successEntity(ResponseCode.OK);
    }

    private Long resolveUserId(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.userId() == null) {
            throw new CustomException(ResponseCode.UNAUTHORIZED);
        }

        return authenticatedUser.userId();
    }
}
