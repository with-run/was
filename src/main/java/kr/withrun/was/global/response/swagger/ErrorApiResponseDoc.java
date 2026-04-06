package kr.withrun.was.global.response.swagger;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ErrorApiResponse", description = "표준 오류 응답 래퍼")
public record ErrorApiResponseDoc(
        @Schema(description = "요청 성공 여부. 오류 응답은 항상 false다.", example = "false")
        boolean success,

        @Schema(description = "오류 응답 코드", example = "E003")
        String code,

        @Schema(description = "오류 메시지", example = "리소스를 찾을 수 없습니다.")
        String message,

        @Schema(description = "오류 추적용 식별자", example = "1a2b3c4d")
        String traceId
) {
}
