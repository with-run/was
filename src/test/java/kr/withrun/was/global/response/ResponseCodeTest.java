package kr.withrun.was.global.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("응답 코드")
class ResponseCodeTest {

    @DisplayName("주변 코스 잘못된 요청 코드를 Bad Request 실패로 노출한다")
    @Test
    void exposesInvalidNearbyCourseRequestCodesAsCommonBadRequestFailures() {
        assertResponseCode(ResponseCode.INVALID_RADIUS, "E006", HttpStatus.BAD_REQUEST, "잘못된 반경 값입니다.");
        assertResponseCode(ResponseCode.INVALID_TARGET_DISTANCE, "E007", HttpStatus.BAD_REQUEST, "잘못된 목표 거리 값입니다.");
        assertResponseCode(ResponseCode.INVALID_CURSOR, "E008", HttpStatus.BAD_REQUEST, "잘못된 커서 값입니다.");
    }

    @DisplayName("러닝 세션 잘못된 요청 코드를 Bad Request 실패로 노출한다")
    @Test
    void exposesInvalidRunningSessionRequestCodesAsBadRequestFailures() {
        Map<String, ResponseCode> responseCodes = responseCodesByName();

        assertThat(responseCodes).containsKeys("INVALID_RUNNING_MODE", "INVALID_GHOST_TARGET");

        assertResponseCode(responseCodes.get("INVALID_RUNNING_MODE"), "E400", HttpStatus.BAD_REQUEST, "잘못된 러닝 모드입니다.");
        assertResponseCode(responseCodes.get("INVALID_GHOST_TARGET"), "E402", HttpStatus.BAD_REQUEST, "유효하지 않은 고스트 대상 러닝 세션입니다.");
    }

    @DisplayName("코스 없음 코드를 Not Found 실패로 노출한다")
    @Test
    void exposesCourseNotFoundAsNotFoundFailure() {
        assertResponseCode(ResponseCode.COURSE_NOT_FOUND, "E304", HttpStatus.NOT_FOUND, "코스를 찾을 수 없습니다.");
    }

    @DisplayName("코스 중복 코드를 Conflict 실패로 노출한다")
    @Test
    void exposesCourseAlreadyExistsAsConflictFailure() {
        assertResponseCode(ResponseCode.COURSE_ALREADY_EXISTS, "E307", HttpStatus.CONFLICT, "이미 등록된 코스입니다.");
    }

    @DisplayName("고스트 타깃 없음 코드를 Not Found 실패로 노출한다")
    @Test
    void exposesGhostTargetNotFoundAsNotFoundFailure() {
        Map<String, ResponseCode> responseCodes = responseCodesByName();

        assertThat(responseCodes).containsKey("GHOST_TARGET_NOT_FOUND");

        assertResponseCode(responseCodes.get("GHOST_TARGET_NOT_FOUND"), "E401", HttpStatus.NOT_FOUND, "고스트 대상 러닝 세션을 찾을 수 없습니다.");
    }

    @DisplayName("러닝 세션 오류 코드를 기대한 상태와 메시지로 노출한다")
    @Test
    void exposesRunningSessionErrorCodes() {
        Map<String, ResponseCode> responseCodes = responseCodesByName();

        assertThat(responseCodes).containsKeys("RUNNING_SESSION_NOT_FOUND", "RUNNING_SESSION_ALREADY_COMPLETED");

        assertResponseCode(responseCodes.get("RUNNING_SESSION_NOT_FOUND"), "E403", HttpStatus.NOT_FOUND, "러닝 세션을 찾을 수 없습니다.");
        assertResponseCode(responseCodes.get("RUNNING_SESSION_ALREADY_COMPLETED"), "E404", HttpStatus.CONFLICT, "이미 종료된 러닝 세션입니다.");
    }

    @DisplayName("고스트 러닝 결과 없음 코드를 Not Found 실패로 노출한다")
    @Test
    void exposesGhostRunningResultNotFoundAsNotFoundFailure() {
        assertResponseCode(
                ResponseCode.GHOST_RUNNING_RESULT_NOT_FOUND,
                "E406",
                HttpStatus.NOT_FOUND,
                "고스트 러닝 결과를 찾을 수 없습니다."
        );
    }

    private static Map<String, ResponseCode> responseCodesByName() {
        return Arrays.stream(ResponseCode.values())
                .collect(Collectors.toMap(ResponseCode::name, Function.identity()));
    }

    private static void assertResponseCode(ResponseCode responseCode, String code, HttpStatus status, String message) {
        ApiResponse<Void> response = ApiResponse.fail(responseCode);

        assertThat(responseCode)
                .extracting("code", "status", "message", "success")
                .containsExactly(code, status, message, false);
        assertThat(response)
                .extracting("success", "code", "message")
                .containsExactly(false, code, message);
    }
}
