package kr.withrun.was.domain.meta;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.withrun.was.domain.course.dto.CourseFilterResponse;
import kr.withrun.was.domain.course.dto.CourseNavigationMetaResponse;
import kr.withrun.was.domain.course.dto.CourseRegisterMetaResponse;
import kr.withrun.was.domain.course.dto.CourseSurveyMetaResponse;
import kr.withrun.was.domain.course.service.CourseService;
import kr.withrun.was.global.response.ApiResponse;
import kr.withrun.was.global.response.swagger.SuccessApiResponseDocs;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Meta", description = "메타 조회 API")
@Validated
@RestController
@RequestMapping("/api/meta")
public class CourseMetaController {

    private final CourseService courseService;

    public CourseMetaController(CourseService courseService) {
        this.courseService = courseService;
    }

    @Operation(
            summary = "코스 등록 메타 항목을 조회한다",
            description = "코스 등록 화면에서 사용하는 코스 유형, 공개 범위, 경로 형태, 난이도 선택 항목 목록을 조회한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "코스 등록 메타 항목 조회 성공",
                    content = @Content(schema = @Schema(implementation = SuccessApiResponseDocs.CourseRegisterMetaApiResponse.class))
            )
    })
    @GetMapping("/course/register")
    public ResponseEntity<ApiResponse<CourseRegisterMetaResponse>> findCourseRegisterMeta() {
        return ApiResponse.successEntity(courseService.findCourseRegisterMeta());
    }

    @Operation(
            summary = "설문 메타 항목을 조회한다",
            description = "설문 화면에서 사용하는 코스 유형과 난이도 선택 항목 목록을 조회한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "설문 메타 항목 조회 성공",
                    content = @Content(schema = @Schema(implementation = SuccessApiResponseDocs.CourseSurveyMetaApiResponse.class))
            )
    })
    @GetMapping("/survey")
    public ResponseEntity<ApiResponse<CourseSurveyMetaResponse>> findSurveyMeta() {
        return ApiResponse.successEntity(courseService.findSurveyMeta());
    }

    @Operation(
            summary = "코스 필터 항목을 조회한다",
            description = "코스 거리, 코스 유형과 난이도 설문에 사용할 선택 항목 목록을 조회한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "코스 필터 항목 조회 성공",
                    content = @Content(schema = @Schema(implementation = SuccessApiResponseDocs.CourseFilterApiResponse.class))
            )
    })
    @GetMapping("/course/filter/normal-run")
    public ResponseEntity<ApiResponse<CourseFilterResponse>> findCourseFilters() {
        return ApiResponse.successEntity(courseService.findCourseFilters());
    }

    @Operation(
            summary = "코스 navigation 메타 항목을 조회한다",
            description = "코스 navigation 에서 사용하는 maneuver 타입, 강도, 예시 액션과 안내 문구 목록을 조회한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "코스 navigation 메타 항목 조회 성공",
                    content = @Content(schema = @Schema(implementation = SuccessApiResponseDocs.CourseNavigationMetaApiResponse.class))
            )
    })
    @GetMapping("/course/navigation")
    public ResponseEntity<ApiResponse<CourseNavigationMetaResponse>> findCourseNavigationMeta() {
        return ApiResponse.successEntity(courseService.findCourseNavigationMeta());
    }
}
