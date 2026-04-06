package kr.withrun.was.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.withrun.was.domain.auth.security.AuthenticatedUser;
import kr.withrun.was.domain.user.dto.UserCalendarMonthlyRequest;
import kr.withrun.was.domain.user.dto.UserCalendarMonthlyResponse;
import kr.withrun.was.domain.user.dto.UserCalendarSummaryResponse;
import kr.withrun.was.domain.user.dto.UserCalendarWeeklyResponse;
import kr.withrun.was.domain.user.service.UserCalendarService;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ApiResponse;
import kr.withrun.was.global.response.ResponseCode;
import kr.withrun.was.global.response.swagger.ErrorApiResponseDoc;
import kr.withrun.was.global.response.swagger.SuccessApiResponseDocs;
import kr.withrun.was.global.response.swagger.ValidationErrorApiResponseDoc;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User Calendar", description = "사용자 러닝 캘린더 및 누적 요약 API")
@Validated
@RestController
@RequestMapping("/api/calendars")
@RequiredArgsConstructor
public class UserCalendarController {

    private final UserCalendarService userCalendarService;

    @Operation(
            summary = "월간 캘린더를 조회한다",
            description = "현재 로그인한 사용자의 월간 러닝 캘린더와 일자별 집계 정보를 조회한다. year 와 month 는 쿼리 파라미터로 전달한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "월간 캘린더 조회 성공",
                    content = @Content(schema = @Schema(implementation = SuccessApiResponseDocs.UserCalendarMonthlyApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "year 또는 month 값이 유효하지 않음",
                    content = @Content(schema = @Schema(implementation = ValidationErrorApiResponseDoc.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorApiResponseDoc.class))
            )
    })
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserCalendarMonthlyResponse>> findMonthlyCalendar(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @ParameterObject @ModelAttribute UserCalendarMonthlyRequest request
    ) {
        return ApiResponse.successEntity(
                userCalendarService.findMonthlyCalendar(resolveUserId(authenticatedUser), request.year(), request.month())
        );
    }

    @Operation(
            summary = "러닝 요약을 조회한다",
            description = "현재 로그인한 사용자의 누적 러닝 횟수, 거리, 연속 러닝 일수 같은 요약 지표를 조회한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "러닝 요약 조회 성공",
                    content = @Content(schema = @Schema(implementation = SuccessApiResponseDocs.UserCalendarSummaryApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorApiResponseDoc.class))
            )
    })
    @GetMapping("/me/summary")
    public ResponseEntity<ApiResponse<UserCalendarSummaryResponse>> findSummary(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ApiResponse.successEntity(userCalendarService.findSummary(resolveUserId(authenticatedUser)));
    }

    @Operation(
            summary = "주간 러닝 요약을 조회한다",
            description = "현재 로그인한 사용자의 이번 주(일요일~토요일) 총 러닝 횟수, 거리, 소모 칼로리, 운동 시간을 조회한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "주간 러닝 요약 조회 성공",
                    content = @Content(schema = @Schema(implementation = SuccessApiResponseDocs.UserCalendarWeeklyApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorApiResponseDoc.class))
            )
    })
    @GetMapping("/me/weekly")
    public ResponseEntity<ApiResponse<UserCalendarWeeklyResponse>> findWeeklySummary(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ApiResponse.successEntity(userCalendarService.findWeeklySummary(resolveUserId(authenticatedUser)));
    }

    private Long resolveUserId(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.userId() == null) {
            throw new CustomException(ResponseCode.UNAUTHORIZED);
        }

        return authenticatedUser.userId();
    }
}
