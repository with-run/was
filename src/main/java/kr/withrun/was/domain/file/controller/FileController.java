package kr.withrun.was.domain.file.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.withrun.was.domain.file.service.S3FileService;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ApiResponse;
import kr.withrun.was.global.response.ResponseCode;
import kr.withrun.was.global.response.swagger.ErrorApiResponseDoc;
import kr.withrun.was.global.response.swagger.SuccessApiResponseDocs;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static kr.withrun.was.global.response.ResponseCode.CREATED;

@Validated
@Tag(name = "File", description = "파일 업로드 API")
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final S3FileService s3FileService;

    @Operation(
            summary = "코스 스냅샷 이미지를 업로드한다",
            description = "multipart/form-data 파일을 받아 S3에 업로드한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "코스 스냅샷 업로드 성공",
                    content = @Content(schema = @Schema(implementation = SuccessApiResponseDocs.CreatedApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "업로드 파일이 비어 있거나 courseId가 유효하지 않음",
                    content = @Content(schema = @Schema(implementation = ErrorApiResponseDoc.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "S3 업로드 실패 또는 버킷 설정 누락",
                    content = @Content(schema = @Schema(implementation = ErrorApiResponseDoc.class))
            )
    })
    @PostMapping(
            value = "/course-snapshots/{courseId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<Void>> uploadCourseSnapshot(
            @PathVariable Long courseId,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ResponseCode.EMPTY_FILE);
        }

        try {
            s3FileService.uploadCourseSnapshot(file.getBytes(), courseId);
            return ApiResponse.successEntity(CREATED);
        } catch (IOException exception) {
            throw new CustomException(ResponseCode.S3_UPLOAD_FAILED);
        }
    }
}
