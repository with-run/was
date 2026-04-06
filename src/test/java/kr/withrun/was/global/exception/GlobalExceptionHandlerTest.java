package kr.withrun.was.global.exception;

import kr.withrun.was.global.response.ApiResponse;
import kr.withrun.was.global.response.ResponseCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("전역 예외 처리기")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @DisplayName("반경 바인딩 오류가 발생하면 잘못된 반경 응답을 반환한다")
    @Test
    void returnsInvalidRadiusForBindableRadiusErrors() throws Exception {
        BindException bindException = bindExceptionWith(fieldError("radiusM", "abc", "typeMismatch"));

        ResponseEntity<ApiResponse<List<GlobalExceptionHandler.ValidationError>>> response =
                globalExceptionHandler.handleBindException(bindException);

        assertThat(response.getStatusCode()).isEqualTo(ResponseCode.INVALID_RADIUS.getStatus());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ResponseCode.INVALID_RADIUS.getCode());
    }

    @DisplayName("목표 거리 바인딩 오류가 발생하면 잘못된 목표 거리 응답을 반환한다")
    @Test
    void returnsInvalidTargetDistanceForBindableTargetDistanceErrors() throws Exception {
        BindException bindException = bindExceptionWith(fieldError("targetDistanceM", "abc", "typeMismatch"));

        ResponseEntity<ApiResponse<List<GlobalExceptionHandler.ValidationError>>> response =
                globalExceptionHandler.handleBindException(bindException);

        assertThat(response.getStatusCode()).isEqualTo(ResponseCode.INVALID_TARGET_DISTANCE.getStatus());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ResponseCode.INVALID_TARGET_DISTANCE.getCode());
    }

    @DisplayName("반경 누락은 일반적인 잘못된 입력 응답으로 유지한다")
    @Test
    void keepsMissingRadiusAsGenericInvalidInputValue() throws Exception {
        BindException bindException = bindExceptionWith(fieldError("radiusM", null, "NotNull"));

        ResponseEntity<ApiResponse<List<GlobalExceptionHandler.ValidationError>>> response =
                globalExceptionHandler.handleBindException(bindException);

        assertThat(response.getStatusCode()).isEqualTo(ResponseCode.INVALID_INPUT_VALUE.getStatus());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ResponseCode.INVALID_INPUT_VALUE.getCode());
        assertThat(response.getBody().getData()).hasSize(1);
        assertThat(response.getBody().getData().getFirst().field()).isEqualTo("radiusM");
    }

    @DisplayName("예상치 못한 예외에 감싸진 CustomException도 원래 응답 코드로 복구한다")
    @Test
    void unwrapsNestedCustomExceptionFromUnexpectedExceptionHandler() {
        Exception wrapped = new IllegalStateException(
                "wrapped",
                new CustomException(ResponseCode.MISSING_COURSE_COORDINATE_ELEVATION)
        );

        ResponseEntity<ApiResponse<Void>> response = globalExceptionHandler.handleUnexpectedException(wrapped);

        assertThat(response.getStatusCode()).isEqualTo(ResponseCode.MISSING_COURSE_COORDINATE_ELEVATION.getStatus());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ResponseCode.MISSING_COURSE_COORDINATE_ELEVATION.getCode());
    }

    @DisplayName("요청 파라미터 숫자 변환 오류가 발생하면 잘못된 입력 응답을 반환한다")
    @Test
    void returnsInvalidInputValueForMethodArgumentTypeMismatch() {
        MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
                "abc",
                Integer.class,
                "size",
                null,
                new NumberFormatException("For input string: abc")
        );

        ResponseEntity<ApiResponse<List<GlobalExceptionHandler.ValidationError>>> response =
                globalExceptionHandler.handleMethodArgumentTypeMismatchException(exception);

        assertThat(response.getStatusCode()).isEqualTo(ResponseCode.INVALID_INPUT_VALUE.getStatus());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ResponseCode.INVALID_INPUT_VALUE.getCode());
        assertThat(response.getBody().getData()).hasSize(1);
        assertThat(response.getBody().getData().getFirst().field()).isEqualTo("size");
        assertThat(response.getBody().getData().getFirst().rejectedValue()).isEqualTo("abc");
    }

    @DisplayName("없는 경로 요청은 리소스 없음 응답을 반환한다")
    @Test
    void returnsEntityNotFoundForNoResourceFoundException() {
        NoResourceFoundException exception = new NoResourceFoundException(HttpMethod.GET, "/api/courses/surveys");

        ResponseEntity<ApiResponse<Void>> response = globalExceptionHandler.handleNoResourceFoundException(exception);

        assertThat(response.getStatusCode()).isEqualTo(ResponseCode.ENTITY_NOT_FOUND.getStatus());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ResponseCode.ENTITY_NOT_FOUND.getCode());
    }

    private BindException bindExceptionWith(FieldError fieldError) {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "nearbyCoursesRequest");
        bindingResult.addError(fieldError);
        return new BindException(bindingResult);
    }

    private FieldError fieldError(String field, Object rejectedValue, String code) {
        return new FieldError("nearbyCoursesRequest", field, rejectedValue, false, new String[]{code}, null, code);
    }
}
