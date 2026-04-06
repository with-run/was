package kr.withrun.was.domain.course.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.withrun.was.domain.auth.security.AuthenticatedUser;
import kr.withrun.was.domain.course.dto.CourseDetailResponse;
import kr.withrun.was.domain.course.dto.CourseGhostDetailResponse;
import kr.withrun.was.domain.course.dto.CourseGhostLeaderboardRequest;
import kr.withrun.was.domain.course.dto.CourseGhostLeaderboardResponse;
import kr.withrun.was.domain.course.dto.NearbyCoursesRequest;
import kr.withrun.was.domain.course.dto.NearbyCoursesResponse;
import kr.withrun.was.domain.course.dto.NearbyGhostCoursesRequest;
import kr.withrun.was.domain.course.dto.NearbyGhostCoursesResponse;
import kr.withrun.was.domain.course.service.CourseService;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ApiResponse;
import kr.withrun.was.global.response.ResponseCode;
import kr.withrun.was.global.response.swagger.ErrorApiResponseDoc;
import kr.withrun.was.global.response.swagger.SuccessApiResponseDocs;
import kr.withrun.was.global.response.swagger.ValidationErrorApiResponseDoc;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "코스", description = "코스 조회 API")
@Validated
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @Operation(summary = "코스 상세 조회", description = "코스 상세 정보를 조회합니다. PRIVATE 코스는 소유자만 조회할 수 있으며 비소유자에게는 404를 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "코스 상세 조회 성공",
                    content = @Content(schema = @Schema(implementation = SuccessApiResponseDocs.CourseDetailApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "코스를 찾을 수 없거나 접근 권한이 없음",
                    content = @Content(schema = @Schema(implementation = ErrorApiResponseDoc.class))
            )
    })
    @GetMapping("/{courseId}")
    public ResponseEntity<ApiResponse<CourseDetailResponse>> findCourseDetail(
            @Parameter(description = "코스 ID", example = "12")
            @PathVariable Long courseId,
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ApiResponse.successEntity(courseService.findCourseDetail(courseId, resolveUserId(authenticatedUser)));
    }

    @Operation(summary = "코스 고스트 상세 조회", description = "사용자의 고스트 기록을 포함한 코스 상세 정보를 조회합니다. PRIVATE 코스는 소유자만 조회할 수 있으며 비소유자에게는 404를 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "코스 고스트 상세 조회 성공",
                    content = @Content(schema = @Schema(implementation = SuccessApiResponseDocs.CourseGhostDetailApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "코스를 찾을 수 없거나 접근 권한이 없음",
                    content = @Content(schema = @Schema(implementation = ErrorApiResponseDoc.class))
            )
    })
    @GetMapping("/{courseId}/ghost-detail")
    public ResponseEntity<ApiResponse<CourseGhostDetailResponse>> findCourseGhostDetail(
            @Parameter(description = "코스 ID", example = "12")
            @PathVariable Long courseId,
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ApiResponse.successEntity(courseService.findCourseGhostDetail(courseId, resolveUserId(authenticatedUser)));
    }

    @Operation(
            summary = "주변 코스 조회",
            description = "반경, 선호 코스 길이, 정렬 조건에 맞는 주변 코스 목록을 조회합니다. PRIVATE 코스는 feed 에 포함되지 않습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "주변 코스 조회 성공",
                    content = @Content(schema = @Schema(implementation = SuccessApiResponseDocs.NearbyCoursesApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 주변 코스 조회 요청",
                    content = @Content(schema = @Schema(implementation = ValidationErrorApiResponseDoc.class))
            )
    })
    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<NearbyCoursesResponse>> findNearbyCourses(
            @Valid @ParameterObject @ModelAttribute NearbyCoursesRequest request, @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.successEntity(courseService.findNearbyCourses(request, user));
    }

    @Operation(
            summary = "추천 코스 조회",
            description = "동일 요청 조건으로 추천 코스 3개를 조회합니다. PRIVATE 코스와 private interaction 은 추천 계산에서 제외됩니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "추천 코스 조회 성공",
                    content = @Content(schema = @Schema(implementation = SuccessApiResponseDocs.RecommendedNearbyCoursesApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 추천 코스 조회 요청",
                    content = @Content(schema = @Schema(implementation = ValidationErrorApiResponseDoc.class))
            )
    })
    @GetMapping("/nearby/recommended")
    public ResponseEntity<ApiResponse<NearbyCoursesResponse>> findRecommendedNearbyCourses(
            @Valid @ParameterObject @ModelAttribute NearbyCoursesRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.successEntity(courseService.findRecommendedNearbyCourses(request, user));
    }

    @Operation(
            summary = "주변 고스트 런 코스 조회",
            description = "반경, 선호 코스 길이, 정렬 조건에 맞는 주변 고스트 런 코스 목록을 조회합니다. PRIVATE 코스는 feed 에 포함되지 않습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "주변 고스트 런 코스 조회 성공",
                    content = @Content(schema = @Schema(implementation = SuccessApiResponseDocs.NearbyGhostCoursesApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 주변 고스트 런 코스 조회 요청",
                    content = @Content(schema = @Schema(implementation = ValidationErrorApiResponseDoc.class))
            )
    })
    @GetMapping("/nearby/ghost")
    public ResponseEntity<ApiResponse<NearbyGhostCoursesResponse>> findNearbyGhostCourses(
            @Valid @ParameterObject @ModelAttribute NearbyGhostCoursesRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.successEntity(courseService.findNearbyGhostCourses(request, user));
    }


    @Operation(summary = "코스 고스트 리더보드 조회", description = "코스의 고스트 리더보드를 조회합니다. PRIVATE 코스는 소유자만 조회할 수 있으며 비소유자에게는 404를 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "코스 고스트 리더보드 조회 성공",
                    content = @Content(schema = @Schema(implementation = SuccessApiResponseDocs.CourseGhostLeaderboardApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 리더보드 조회 파라미터",
                    content = @Content(schema = @Schema(implementation = ValidationErrorApiResponseDoc.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "코스를 찾을 수 없거나 접근 권한이 없음",
                    content = @Content(schema = @Schema(implementation = ErrorApiResponseDoc.class))
            )
    })
    @GetMapping("/{courseId}/ghost-leaderboard")
    public ResponseEntity<ApiResponse<CourseGhostLeaderboardResponse>> findCourseGhostLeaderboard(
            @Parameter(description = "코스 ID", example = "12")
            @PathVariable Long courseId,
            @Valid @ParameterObject @ModelAttribute CourseGhostLeaderboardRequest request,
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ApiResponse.successEntity(courseService.findCourseGhostLeaderboard(courseId, resolveUserId(authenticatedUser), request));
    }

    private Long resolveUserId(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.userId() == null) {
            throw new CustomException(ResponseCode.UNAUTHORIZED);
        }

        return authenticatedUser.userId();
    }
}
