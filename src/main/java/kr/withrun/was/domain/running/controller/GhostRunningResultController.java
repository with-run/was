package kr.withrun.was.domain.running.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.withrun.was.domain.running.dto.GhostRunningResultResponse;
import kr.withrun.was.domain.running.service.GhostRunningResultService;
import kr.withrun.was.global.response.ApiResponse;
import kr.withrun.was.global.response.swagger.ErrorApiResponseDoc;
import kr.withrun.was.global.response.swagger.SuccessApiResponseDocs;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "Ghost Running API", description = "고스트 러닝 관련 API")
@RestController
@RequestMapping("/api/ghost-runnings")
@RequiredArgsConstructor
public class GhostRunningResultController {

    private final GhostRunningResultService ghostRunningResultService;

    @Operation(
            summary = "고스트 러닝 결과를 조회한다",
            description = "특정 러닝 세션에 저장된 고스트 러닝 승패 결과와 시간/거리 차이를 조회한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "고스트 러닝 결과 조회 성공",
                    content = @Content(schema = @Schema(implementation = SuccessApiResponseDocs.GhostRunningResultApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "고스트 러닝 결과를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorApiResponseDoc.class))
            )
    })
    @GetMapping("/{runningSessionId}/result")
    public ResponseEntity<ApiResponse<GhostRunningResultResponse>> findGhostRunningResult(
            @Parameter(description = "고스트 결과를 조회할 러닝 세션 ID", example = "901")
            @PathVariable Long runningSessionId
    ) {
        return ApiResponse.successEntity(ghostRunningResultService.findGhostRunningResult(runningSessionId));
    }

}
