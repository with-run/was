package kr.withrun.was.domain.running.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import kr.withrun.was.domain.auth.security.AuthenticatedUser;
import kr.withrun.was.domain.running.dto.CompleteRunningSessionRequest;
import kr.withrun.was.domain.running.dto.CreateRunningSessionRequest;
import kr.withrun.was.domain.running.dto.CreateRunningSessionResponse;
import kr.withrun.was.domain.running.dto.PastRunningSessionsRequest;
import kr.withrun.was.domain.running.dto.PastRunningSessionsResponse;
import kr.withrun.was.domain.running.dto.RegisterRunningSessionCourseRequest;
import kr.withrun.was.domain.running.dto.RegisterRunningSessionCourseResponse;
import kr.withrun.was.domain.running.dto.RunningSessionDetailResponse;
import kr.withrun.was.domain.running.service.RunningSessionService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static kr.withrun.was.global.response.ResponseCode.CREATED;

@Validated
@Tag(name = "Running Session", description = "러닝 세션 생성, 종료, 조회 API")
@RestController
@RequestMapping("/api/running-sessions")
@RequiredArgsConstructor
public class RunningSessionController {

    private final RunningSessionService runningSessionService;

    @Operation(
            summary = "러닝 세션을 시작한다",
            description = "사용자와 러닝 모드 기준으로 새로운 러닝 세션을 생성한다. COURSE 와 GHOST 모드에서는 courseId 가 필요하다. GHOST 모드에서 ghostTargetRunningSessionId 를 비우면 같은 코스의 내 최고 리더보드 기록을 고스트 대상으로 사용하고, 없으면 대상 없이 시작한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "러닝 세션 생성 성공",
                    content = @Content(schema = @Schema(implementation = SuccessApiResponseDocs.CreateRunningSessionApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 본문 형식, 좌표 범위, 모드별 필수 필드 또는 고스트 대상 조건이 올바르지 않음",
                    content = @Content(schema = @Schema(implementation = ValidationErrorApiResponseDoc.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자, 코스 또는 고스트 대상 러닝 세션을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorApiResponseDoc.class))
            )
    })
    @PostMapping
    public ResponseEntity<ApiResponse<CreateRunningSessionResponse>> createRunningSession(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "러닝 시작 시점의 모드와 시작 좌표를 담은 요청 본문"
            )
            @Valid @RequestBody CreateRunningSessionRequest request
    ) {
        return ApiResponse.successEntity(CREATED, runningSessionService.createRunningSession(resolveUserId(authenticatedUser), request));
    }

    @Operation(
            summary = "러닝 세션을 종료한다",
            description = "진행 중인 러닝 세션을 종료하고 최종 거리, 칼로리, 페이스 등 요약 기록을 확정한다. 이미 종료된 세션은 다시 종료할 수 없다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "러닝 세션 종료 성공",
                    content = @Content(schema = @Schema(implementation = SuccessApiResponseDocs.RunningSessionDetailApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 본문 형식, 좌표 범위 또는 종료 기록 값이 올바르지 않음",
                    content = @Content(schema = @Schema(implementation = ValidationErrorApiResponseDoc.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "러닝 세션을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorApiResponseDoc.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 종료된 러닝 세션임",
                    content = @Content(schema = @Schema(implementation = ErrorApiResponseDoc.class))
            )
    })
    @PatchMapping("/{runningSessionId}")
    public ResponseEntity<ApiResponse<RunningSessionDetailResponse>> completeRunningSession(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Parameter(description = "종료 처리할 러닝 세션 ID", example = "91")
            @PathVariable Long runningSessionId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "러닝 종료 기록과 GPS/건강/스플릿 상세 데이터를 함께 담은 요청 본문"
            )
            @Valid @RequestBody CompleteRunningSessionRequest request
    ) {
        return ApiResponse.successEntity(
                runningSessionService.completeRunningSession(runningSessionId, resolveUserId(authenticatedUser), request)
        );
    }

    @Operation(
            summary = "과거 러닝 세션을 조회한다",
            description = "사용자 기준으로 과거 러닝 세션 목록을 조회한다. year, month, day 는 날짜 필터이고 cursor 는 이전 응답의 nextCursor 값을 그대로 전달하는 불투명 토큰이다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "과거 러닝 세션 조회 성공",
                    content = @Content(schema = @Schema(implementation = SuccessApiResponseDocs.PastRunningSessionsApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "userId, 날짜 필터 조합, pageSize 또는 cursor 값이 유효하지 않음",
                    content = @Content(schema = @Schema(implementation = ValidationErrorApiResponseDoc.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorApiResponseDoc.class))
            )
    })
    @GetMapping("/history/past")
    public ResponseEntity<ApiResponse<PastRunningSessionsResponse>> findPastRunningSessions(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @ParameterObject @ModelAttribute PastRunningSessionsRequest request
    ) {
        return ApiResponse.successEntity(runningSessionService.findPastRunningSessions(resolveUserId(authenticatedUser), request));
    }

    @Operation(
            summary = "완료한 러닝 기록을 코스로 등록한다",
            description = "완료된 러닝 세션 기록을 바탕으로 코스를 생성한다. mode 는 COMMUNITY 또는 PRIVATE 문자열 enum 이름으로 전달하며 난이도와 코스 유형도 문자열 enum 이름으로 전달한다. snapshotImageUrl 을 비우면 세션 스냅샷을 재사용한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "러닝 기록 코스 등록 성공",
                    content = @Content(schema = @Schema(implementation = SuccessApiResponseDocs.RegisterRunningSessionCourseApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 본문 형식, 난이도 또는 코스 유형 값이 유효하지 않음",
                    content = @Content(schema = @Schema(implementation = ValidationErrorApiResponseDoc.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "러닝 세션을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorApiResponseDoc.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "아직 완료되지 않은 러닝 세션이라 코스로 등록할 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorApiResponseDoc.class))
            )
    })
    @PostMapping("/{runningSessionId}/register-course")
    public ResponseEntity<ApiResponse<RegisterRunningSessionCourseResponse>> registerRunningSessionCourse(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Parameter(description = "코스로 등록할 완료된 러닝 세션 ID", example = "91")
            @PathVariable Long runningSessionId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "코스 제목, 공개 범위 mode, 난이도, 코스 유형, 스냅샷 이미지 URL을 담은 요청 본문"
            )
            @Valid @RequestBody RegisterRunningSessionCourseRequest request
    ) {
        return ApiResponse.successEntity(
                CREATED,
                runningSessionService.registerRunningSessionCourse(runningSessionId, resolveUserId(authenticatedUser), request)
        );
    }

    @Operation(
            summary = "러닝 세션 상세를 조회한다",
            description = "러닝 세션의 요약 정보와 GPS 샘플, 건강 샘플, 스플릿, 고스트 결과를 함께 조회한다. 응답의 completeState 는 세션 종료 상태를 나타내며 시작 직후 기본값은 FAIL 이고 종료 요청으로 최종 상태가 반영된다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "러닝 세션 상세 조회 성공",
                    content = @Content(schema = @Schema(implementation = SuccessApiResponseDocs.RunningSessionDetailApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "runningSessionId 경로 값이 유효하지 않음",
                    content = @Content(schema = @Schema(implementation = ValidationErrorApiResponseDoc.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "러닝 세션을 찾을 수 없거나 소유자가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorApiResponseDoc.class))
            )
    })
    @GetMapping("/{runningSessionId}/detail")
    public ResponseEntity<ApiResponse<RunningSessionDetailResponse>> findRunningSessionDetail(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Parameter(description = "상세 조회할 러닝 세션 ID", example = "91")
            @PathVariable @Positive Long runningSessionId
    ) {
        return ApiResponse.successEntity(runningSessionService.findRunningSessionDetail(runningSessionId, resolveUserId(authenticatedUser)));
    }

    private Long resolveUserId(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.userId() == null) {
            throw new CustomException(ResponseCode.UNAUTHORIZED);
        }

        return authenticatedUser.userId();
    }
}
