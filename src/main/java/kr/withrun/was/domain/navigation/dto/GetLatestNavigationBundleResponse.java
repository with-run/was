package kr.withrun.was.domain.navigation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "latest navigation bundle 조회 결과")
/**
 * latest navigation bundle 조회 API 의 상태/다운로드 정보를 담는 DTO 입니다.
 */
public record GetLatestNavigationBundleResponse(
        @Schema(description = "코스 ID", example = "42")
        Long courseId,

        @Schema(description = "bundle 상태", example = "READY")
        String status,

        @Schema(
                description = "다운로드 URL",
                example = "https://cdn.withrun.kr/navigation/latest/42.json",
                nullable = true
        )
        String downloadUrl
) {
}
