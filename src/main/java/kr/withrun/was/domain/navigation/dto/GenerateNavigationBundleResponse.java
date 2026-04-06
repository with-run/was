package kr.withrun.was.domain.navigation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "latest navigation bundle 생성 요청 결과")
/**
 * latest navigation bundle 생성 API 의 응답 데이터를 담는 DTO 입니다.
 */
public record GenerateNavigationBundleResponse(
        @Schema(description = "코스 ID", example = "42")
        Long courseId,

        @Schema(description = "bundle 작업 상태", example = "READY")
        String bundleStatus
) {
}
