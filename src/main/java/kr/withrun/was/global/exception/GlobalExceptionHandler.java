package kr.withrun.was.global.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import kr.withrun.was.global.response.ApiResponse;
import kr.withrun.was.global.response.ResponseCode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * CustomException 처리
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
        log.warn("CustomException: {} - {}", e.getResponseCode().getCode(), e.getMessage());

        return ResponseEntity
                .status(e.getResponseCode().getStatus())
                .body(ApiResponse.fail(e.getResponseCode()));
    }

    /**
     * @Valid 검증 실패 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<ValidationError>>> handleValidationException(
            MethodArgumentNotValidException e) {

        return buildValidationErrorResponse(e.getBindingResult(), "Validation failed");
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<List<ValidationError>>> handleBindException(BindException e) {
        return buildValidationErrorResponse(e.getBindingResult(), "Binding failed");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<List<ValidationError>>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e
    ) {
        ValidationError error = new ValidationError(e.getName(), e.getValue(), e.getMessage());
        log.warn("Method argument type mismatch: {}", error);

        return ResponseEntity
                .status(ResponseCode.INVALID_INPUT_VALUE.getStatus())
                .body(ApiResponse.fail(ResponseCode.INVALID_INPUT_VALUE, List.of(error)));
    }

    @ExceptionHandler({HandlerMethodValidationException.class, ConstraintViolationException.class})
    public ResponseEntity<ApiResponse<Void>> handleMethodValidationException(Exception e) {
        log.warn("Method validation failed: {}", e.getMessage());

        return ResponseEntity
                .status(ResponseCode.INVALID_INPUT_VALUE.getStatus())
                .body(ApiResponse.fail(ResponseCode.INVALID_INPUT_VALUE));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(NoResourceFoundException e) {
        log.warn("No resource found: {}", e.getResourcePath());

        return ResponseEntity
                .status(ResponseCode.ENTITY_NOT_FOUND.getStatus())
                .body(ApiResponse.fail(ResponseCode.ENTITY_NOT_FOUND));
    }

    private ResponseEntity<ApiResponse<List<ValidationError>>> buildValidationErrorResponse(
            BindingResult bindingResult,
            String logPrefix
    ) {
        List<ValidationError> errors = extractValidationErrors(bindingResult);
        log.warn("{}: {}", logPrefix, errors);
        ResponseCode responseCode = resolveResponseCode(bindingResult);

        return ResponseEntity
                .status(responseCode.getStatus())
                .body(ApiResponse.fail(responseCode, errors));
    }

    /**
     * 예상치 못한 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception e) {
        CustomException customException = findCustomException(e);
        if (customException != null) {
            return handleCustomException(customException);
        }

        log.error("Unexpected exception: ", e);

        return ResponseEntity
                .internalServerError()
                .body(ApiResponse.fail(ResponseCode.INTERNAL_SERVER_ERROR));
    }

    private List<ValidationError> extractValidationErrors(BindingResult bindingResult) {
        return bindingResult.getFieldErrors().stream()
                .map(error -> new ValidationError(
                        error.getField(),
                        error.getRejectedValue(),
                        error.getDefaultMessage()))
                .collect(Collectors.toList());
    }

    private ResponseCode resolveResponseCode(BindingResult bindingResult) {
        for (FieldError error : bindingResult.getFieldErrors()) {
            if (isSpecificNumericValidation(error, "radiusM")) {
                return ResponseCode.INVALID_RADIUS;
            }
            if (isSpecificNumericValidation(error, "targetDistanceM")) {
                return ResponseCode.INVALID_TARGET_DISTANCE;
            }
        }

        return ResponseCode.INVALID_INPUT_VALUE;
    }

    private boolean isSpecificNumericValidation(FieldError error, String fieldName) {
        if (!fieldName.equals(error.getField())) {
            return false;
        }

        Object rejectedValue = error.getRejectedValue();
        if (rejectedValue == null) {
            return false;
        }
        if (rejectedValue instanceof String stringValue && stringValue.isBlank()) {
            return false;
        }

        return true;
    }

    private CustomException findCustomException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof CustomException customException) {
                return customException;
            }
            current = current.getCause();
        }

        return null;
    }

    @Schema(name = "ValidationError", description = "입력값 검증 실패 상세 정보")
    public record ValidationError(
            @Schema(description = "검증에 실패한 필드명", example = "field")
            String field,

            @Schema(description = "거부된 입력값", example = "invalid-value", nullable = true)
            Object rejectedValue,

            @Schema(description = "검증 실패 사유", example = "must not be null")
            String message
    ) {
    }
}
