package kr.withrun.was.domain.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.withrun.was.domain.auth.dto.AuthCodeExchangeRequest;
import kr.withrun.was.domain.auth.dto.AuthExchangeResult;
import kr.withrun.was.domain.auth.dto.AuthMeResponse;
import kr.withrun.was.domain.auth.dto.ReissueAccessTokenResponse;
import kr.withrun.was.domain.auth.security.AuthenticatedUser;
import kr.withrun.was.domain.auth.service.AuthService;
import kr.withrun.was.global.response.ApiResponse;
import kr.withrun.was.global.response.ResponseCode;
import kr.withrun.was.global.response.swagger.ErrorApiResponseDoc;
import kr.withrun.was.global.response.swagger.SuccessApiResponseDocs;
import kr.withrun.was.global.response.swagger.SwaggerExampleValues;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증 상태 조회, access token 재발급, 로그아웃 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "현재 로그인 사용자를 조회한다",
            description = "Authorization 헤더의 Bearer access token을 기준으로 현재 로그인한 사용자 정보를 조회한다.",
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
                    description = "현재 사용자 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SuccessApiResponseDocs.AuthMeApiResponse.class),
                            examples = @ExampleObject(value = SwaggerExampleValues.AUTH_ME_SUCCESS)
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
    public ResponseEntity<ApiResponse<AuthMeResponse>> getCurrentUser(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ApiResponse.successEntity(authService.getCurrentUser(authenticatedUser));
    }

    @Operation(
            summary = "access token을 재발급한다",
            description = "브라우저에 저장된 refresh token 쿠키를 사용해 새로운 access token을 재발급한다.",
            parameters = {
                    @Parameter(
                            name = "refresh_token",
                            in = ParameterIn.COOKIE,
                            required = true,
                            description = "재발급에 사용할 refresh token 쿠키 값",
                            example = "eyJhbGciOiJIUzI1NiJ9.refresh-token"
                    )
            }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "access token 재발급 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SuccessApiResponseDocs.ReissueAccessTokenApiResponse.class),
                            examples = @ExampleObject(value = SwaggerExampleValues.REISSUE_ACCESS_TOKEN_SUCCESS)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "refresh token이 없거나 만료되었거나 유효하지 않음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorApiResponseDoc.class),
                            examples = {
                                    @ExampleObject(name = "쿠키 없음", value = SwaggerExampleValues.ERROR_UNAUTHORIZED),
                                    @ExampleObject(name = "만료된 토큰", value = SwaggerExampleValues.ERROR_TOKEN_EXPIRED),
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
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<ReissueAccessTokenResponse>> reissueAccessToken(
            @Parameter(hidden = true) HttpServletRequest request
    ) {
        // OAuth callback 직후나 access token 만료 후 모두 이 endpoint 하나로 access token 을 복구합니다.
        return ApiResponse.successEntity(authService.reissueAccessToken(request));
    }

    @PostMapping("/exchange")
    public ResponseEntity<ApiResponse<AuthExchangeResult>> exchangeMobileAuthCode(
            @RequestBody AuthCodeExchangeRequest request,
            @Parameter(hidden = true) HttpServletResponse response
    ) {
        // 모바일 OAuth 복귀 직후에는 refresh cookie가 없을 수 있으므로, code 교환으로 세션을 새로 세웁니다.
        AuthExchangeResult exchangeResult = authService.exchangeMobileAuthCode(request.code());
        response.addHeader(HttpHeaders.SET_COOKIE, exchangeResult.refreshTokenCookie().toString());
        return ApiResponse.successEntity(exchangeResult);
    }

    @Operation(
            summary = "로그아웃한다",
            description = "refresh token 쿠키를 만료시켜 로그아웃 상태로 전환한다.",
            parameters = {
                    @Parameter(
                            name = "refresh_token",
                            in = ParameterIn.COOKIE,
                            description = "만료 처리할 refresh token 쿠키 값. 쿠키가 없어도 성공 응답을 반환한다.",
                            example = "eyJhbGciOiJIUzI1NiJ9.refresh-token"
                    )
            }
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그아웃 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SuccessApiResponseDocs.LogoutApiResponse.class),
                            examples = @ExampleObject(value = SwaggerExampleValues.LOGOUT_SUCCESS)
                    )
            )
    })
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Parameter(hidden = true) HttpServletResponse response
    ) {
        // 로그아웃은 서버 세션 삭제보다 refresh cookie 만료가 핵심이므로 쿠키 정리만 수행합니다.
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                authService.expireRefreshTokenCookie().toString()
        );
        return ApiResponse.successEntity(ResponseCode.USER_LOGGED_OUT);
    }
}
