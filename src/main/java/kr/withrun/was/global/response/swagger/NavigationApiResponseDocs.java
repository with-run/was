package kr.withrun.was.global.response.swagger;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.withrun.was.domain.navigation.dto.GenerateNavigationBundleResponse;
import kr.withrun.was.domain.navigation.dto.GetLatestNavigationBundleResponse;

public final class NavigationApiResponseDocs {

    private NavigationApiResponseDocs() {
    }

    @Schema(name = "GenerateNavigationBundleApiResponse", description = "latest navigation bundle 생성 성공 응답")
    public record GenerateNavigationBundleApiResponse(
            @Schema(description = "요청 성공 여부", example = "true")
            boolean success,
            @Schema(description = "성공 응답 코드", example = "S001")
            String code,
            @Schema(description = "성공 응답 메시지", example = "요청이 성공했습니다.")
            String message,
            @Schema(description = "latest navigation bundle 생성 결과 데이터")
            GenerateNavigationBundleResponse data
    ) {
    }

    @Schema(name = "GetLatestNavigationBundleApiResponse", description = "latest navigation bundle READY 응답")
    public record GetLatestNavigationBundleApiResponse(
            @Schema(description = "요청 성공 여부", example = "true")
            boolean success,
            @Schema(description = "성공 응답 코드", example = "S001")
            String code,
            @Schema(description = "성공 응답 메시지", example = "요청이 성공했습니다.")
            String message,
            @Schema(description = "latest navigation bundle 조회 결과 데이터")
            GetLatestNavigationBundleResponse data
    ) {
    }

    @Schema(name = "PendingNavigationBundleApiResponse", description = "latest navigation bundle PENDING 응답")
    public record PendingNavigationBundleApiResponse(
            @Schema(description = "요청 성공 여부", example = "false")
            boolean success,
            @Schema(description = "오류 응답 코드", example = "E600")
            String code,
            @Schema(description = "오류 메시지", example = "최신 네비게이션 번들이 아직 준비되지 않았습니다.")
            String message,
            @Schema(description = "latest navigation bundle 조회 결과 데이터")
            GetLatestNavigationBundleResponse data,
            @Schema(description = "오류 추적용 식별자", example = "1a2b3c4d")
            String traceId
    ) {
    }

}
