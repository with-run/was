package kr.withrun.was.global.response.swagger;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import kr.withrun.was.global.exception.GlobalExceptionHandler.ValidationError;

import java.util.List;

@Schema(name = "ValidationErrorApiResponse", description = "입력값 검증 실패 응답 래퍼")
public record ValidationErrorApiResponseDoc(
        @Schema(description = "요청 성공 여부. 검증 실패 응답은 항상 false다.", example = "false")
        boolean success,

        @Schema(description = "오류 응답 코드", example = "E001")
        String code,

        @Schema(description = "오류 메시지", example = "잘못된 입력값입니다.")
        String message,

        @ArraySchema(
                schema = @Schema(implementation = ValidationError.class),
                arraySchema = @Schema(description = "필드별 검증 실패 목록", nullable = true)
        )
        List<ValidationError> data,

        @Schema(description = "오류 추적용 식별자", example = "1a2b3c4d")
        String traceId
) {
}
