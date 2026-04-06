package kr.withrun.was.domain.reward.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import kr.withrun.was.domain.reward.dto.RewardItemListResponse;
import kr.withrun.was.domain.reward.dto.RewardItemResponse;
import kr.withrun.was.domain.reward.dto.UpdateRewardItemRequest;
import kr.withrun.was.domain.reward.service.RewardItemAdminService;
import kr.withrun.was.global.response.ApiResponse;
import kr.withrun.was.global.response.ResponseCode;
import kr.withrun.was.global.response.swagger.ErrorApiResponseDoc;
import kr.withrun.was.global.response.swagger.SuccessApiResponseDocs;
import kr.withrun.was.global.response.swagger.ValidationErrorApiResponseDoc;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@Tag(name = "Reward Admin", description = "관리자용 리워드 아이템 관리 API")
@RestController
@RequestMapping("/api/admin/reward-items")
@RequiredArgsConstructor
public class RewardItemAdminController {

    private final RewardItemAdminService rewardItemAdminService;

    @Operation(summary = "리워드 아이템을 생성한다", description = "제목과 이미지를 업로드해 리워드 아이템을 생성한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "리워드 아이템 생성 성공",
                    content = @Content(schema = @Schema(implementation = SuccessApiResponseDocs.CreatedApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "입력값 또는 파일이 유효하지 않음",
                    content = @Content(schema = @Schema(implementation = ValidationErrorApiResponseDoc.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "파일 업로드 실패",
                    content = @Content(schema = @Schema(implementation = ErrorApiResponseDoc.class))
            )
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<RewardItemResponse>> createRewardItem(
            @RequestPart(value = "title", required = false) String title,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        return ApiResponse.successEntity(ResponseCode.CREATED, rewardItemAdminService.createRewardItem(title, file));
    }

    @Operation(summary = "리워드 아이템 목록을 조회한다", description = "관리자용 리워드 아이템 목록을 조회한다.")
    @GetMapping
    public ResponseEntity<ApiResponse<RewardItemListResponse>> getRewardItems(
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(defaultValue = "20") @Positive int size
    ) {
        return ApiResponse.successEntity(rewardItemAdminService.getRewardItems(active, page, size));
    }

    @Operation(summary = "리워드 아이템 단건을 조회한다", description = "관리자용 리워드 아이템 단건을 조회한다.")
    @GetMapping("/{rewardItemId}")
    public ResponseEntity<ApiResponse<RewardItemResponse>> getRewardItem(
            @PathVariable @Positive Long rewardItemId
    ) {
        return ApiResponse.successEntity(rewardItemAdminService.getRewardItem(rewardItemId));
    }

    @Operation(summary = "리워드 아이템 메타데이터를 수정한다", description = "리워드 아이템 제목 또는 활성 상태를 수정한다.")
    @PatchMapping("/{rewardItemId}")
    public ResponseEntity<ApiResponse<RewardItemResponse>> updateRewardItem(
            @PathVariable @Positive Long rewardItemId,
            @Valid @RequestBody UpdateRewardItemRequest request
    ) {
        return ApiResponse.successEntity(rewardItemAdminService.updateRewardItem(rewardItemId, request));
    }

    @Operation(summary = "리워드 아이템 이미지를 교체한다", description = "리워드 아이템 이미지를 업로드해 교체한다.")
    @PutMapping(value = "/{rewardItemId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<RewardItemResponse>> updateRewardItemImage(
            @PathVariable @Positive Long rewardItemId,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        return ApiResponse.successEntity(rewardItemAdminService.updateRewardItemImage(rewardItemId, file));
    }
}
