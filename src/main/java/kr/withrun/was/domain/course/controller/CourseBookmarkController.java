package kr.withrun.was.domain.course.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.withrun.was.domain.auth.security.AuthenticatedUser;
import kr.withrun.was.domain.course.dto.BookmarkedCoursesRequest;
import kr.withrun.was.domain.course.dto.BookmarkedCoursesResponse;
import kr.withrun.was.domain.course.dto.CourseBookmarkAddResponse;
import kr.withrun.was.domain.course.dto.CourseBookmarkRemoveResponse;
import kr.withrun.was.domain.course.service.CourseBookmarkService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Course Bookmark", description = "코스 북마크 추가, 해제, 목록 조회 API")
@Validated
@RestController
@RequestMapping("/api/courses")
public class CourseBookmarkController {

    private final CourseBookmarkService courseBookmarkService;

    public CourseBookmarkController(CourseBookmarkService courseBookmarkService) {
        this.courseBookmarkService = courseBookmarkService;
    }

    @Operation(
            summary = "코스를 북마크한다",
            description = "특정 사용자가 코스를 북마크한다. 이미 북마크된 경우에도 현재 북마크 상태를 그대로 반환한다. PRIVATE 코스는 소유자만 요청할 수 있으며 비소유자에게는 404를 반환한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "북마크 추가 성공",
                    content = @Content(schema = @Schema(implementation = SuccessApiResponseDocs.CourseBookmarkAddApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "코스 또는 사용자를 찾을 수 없거나 접근 권한이 없음",
                    content = @Content(schema = @Schema(implementation = ErrorApiResponseDoc.class))
            )
    })
    @PostMapping("/{courseId}/bookmark")
    public ResponseEntity<ApiResponse<CourseBookmarkAddResponse>> addBookmarkCourse(
            @Parameter(description = "북마크할 코스 ID", example = "12")
            @PathVariable Long courseId,
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ApiResponse.successEntity(courseBookmarkService.addBookmark(courseId, resolveUserId(authenticatedUser)));
    }

    @Operation(
            summary = "코스 북마크를 해제한다",
            description = "특정 사용자의 코스 북마크를 해제한다. 북마크가 없어도 최종 상태를 false 로 반환한다. PRIVATE 코스는 소유자만 요청할 수 있으며 비소유자에게는 404를 반환한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "북마크 해제 성공",
                    content = @Content(schema = @Schema(implementation = SuccessApiResponseDocs.CourseBookmarkRemoveApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "코스 또는 사용자를 찾을 수 없거나 접근 권한이 없음",
                    content = @Content(schema = @Schema(implementation = ErrorApiResponseDoc.class))
            )
    })
    @DeleteMapping("/{courseId}/bookmark")
    public ResponseEntity<ApiResponse<CourseBookmarkRemoveResponse>> removeBookmarkCourse(
            @Parameter(description = "북마크 해제할 코스 ID", example = "12")
            @PathVariable Long courseId,
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ApiResponse.successEntity(courseBookmarkService.removeBookmark(courseId, resolveUserId(authenticatedUser)));
    }

    @Operation(
            summary = "북마크한 코스 OFFICIAL, COMMUNITY 인 목록을 조회한다",
            description = "특정 사용자가 북마크한 OFFICIAL, COMMUNITY 코스를 최신 북마크 순으로 조회한다. PRIVATE 코스는 소유자여도 이 feed 에 포함되지 않는다. 각 항목에는 누적 좋아요 수와 해당 userId 기준 좋아요 여부가 포함된다. cursor 는 이전 응답의 nextCursor 값을 그대로 전달하는 불투명 토큰이다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "북마크 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = SuccessApiResponseDocs.BookmarkedCoursesApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "size 또는 cursor 값이 유효하지 않음",
                    content = @Content(schema = @Schema(implementation = ValidationErrorApiResponseDoc.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorApiResponseDoc.class))
            )
    })
    @GetMapping("/bookmarks")
    public ResponseEntity<ApiResponse<BookmarkedCoursesResponse>> findBookmarkedCourses(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @ParameterObject @ModelAttribute BookmarkedCoursesRequest request
    ) {
        return ApiResponse.successEntity(courseBookmarkService.findBookmarkedCourses(resolveUserId(authenticatedUser), request));
    }

    private Long resolveUserId(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.userId() == null) {
            throw new CustomException(ResponseCode.UNAUTHORIZED);
        }

        return authenticatedUser.userId();
    }
}
