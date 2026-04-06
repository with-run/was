package kr.withrun.was.domain.navigation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import kr.withrun.was.domain.navigation.dto.GenerateNavigationBundleResponse;
import kr.withrun.was.domain.navigation.dto.GetLatestNavigationBundleResponse;
import kr.withrun.was.domain.navigation.service.NavigationBundleService;
import kr.withrun.was.global.response.ApiResponse;
import kr.withrun.was.global.response.ResponseCode;
import kr.withrun.was.global.response.swagger.ErrorApiResponseDoc;
import kr.withrun.was.global.response.swagger.NavigationApiResponseDocs;
import kr.withrun.was.global.response.swagger.ValidationErrorApiResponseDoc;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "네비게이션", description = "latest navigation bundle 생성 및 조회 API")
@Validated
@RestController
/**
 * 네비게이션 번들 생성/조회 엔드포인트를 노출하는 컨트롤러입니다.
 */
public class NavigationController {

    private final NavigationBundleService navigationBundleService;

    /**
     * 네비게이션 API 요청을 서비스 계층으로 위임하기 위한 생성자입니다.
     */
    public NavigationController(NavigationBundleService navigationBundleService) {
        this.navigationBundleService = navigationBundleService;
    }

    @Operation(summary = "latest navigation bundle 을 생성한다", description = "기존 courseId 기준으로 latest navigation bundle 을 동기 생성한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "latest navigation bundle 생성 완료",
                    content = @Content(schema = @Schema(implementation = NavigationApiResponseDocs.GenerateNavigationBundleApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "courseId 가 유효하지 않음",
                    content = @Content(schema = @Schema(implementation = ValidationErrorApiResponseDoc.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "코스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorApiResponseDoc.class))
            )
    })
    @PostMapping("/api/admin/courses/{courseId}/navigation-bundles:generate")
    /**
     * 지정한 코스의 latest navigation bundle 을 동기 생성합니다.
     */
    public ResponseEntity<ApiResponse<GenerateNavigationBundleResponse>> generateNavigationBundle(
            @Parameter(description = "코스 ID", example = "42") @PathVariable @Positive Long courseId
    ) {
        return ApiResponse.successEntity(navigationBundleService.generateBundle(courseId));
    }

    @Operation(summary = "latest navigation bundle 상태를 조회한다", description = "단일 endpoint 에서 PENDING 또는 READY 상태와 다운로드 URL 을 함께 반환한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "latest navigation bundle READY",
                    content = @Content(schema = @Schema(implementation = NavigationApiResponseDocs.GetLatestNavigationBundleApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "202",
                    description = "latest navigation bundle PENDING",
                    content = @Content(schema = @Schema(implementation = NavigationApiResponseDocs.PendingNavigationBundleApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "courseId 가 유효하지 않음",
                    content = @Content(schema = @Schema(implementation = ValidationErrorApiResponseDoc.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "코스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorApiResponseDoc.class))
            )
    })
    @GetMapping("/api/courses/{courseId}/navigation-bundle")
    /**
     * 지정한 코스의 latest navigation bundle 준비 상태와 다운로드 정보를 조회합니다.
     */
    public ResponseEntity<ApiResponse<GetLatestNavigationBundleResponse>> getNavigationBundle(
            @Parameter(description = "코스 ID", example = "42")
            @PathVariable @Positive Long courseId
    ) {
        GetLatestNavigationBundleResponse response = navigationBundleService.getLatestBundle(courseId);
        if ("READY".equals(response.status())) {
            return ApiResponse.successEntity(response);
        }
        return ResponseEntity.status(ResponseCode.NAVIGATION_BUNDLE_PENDING.getStatus())
                .body(ApiResponse.fail(ResponseCode.NAVIGATION_BUNDLE_PENDING, response));
    }

}
