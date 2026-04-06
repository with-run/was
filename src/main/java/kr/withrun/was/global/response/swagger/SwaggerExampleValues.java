package kr.withrun.was.global.response.swagger;

public final class SwaggerExampleValues {

    private SwaggerExampleValues() {
    }

    public static final String AUTH_ME_SUCCESS = """
            {
              "success": true,
              "code": "S001",
              "message": "요청이 성공했습니다.",
              "data": {
                "userId": 10,
                "provider": "google",
                "profileCompleted": true,
                "nickname": "runner",
                "birthDate": "1998-04-05",
                "gender": "FEMALE",
                "height": 165.5,
                "weight": 52.3
              }
            }
            """;

    public static final String REISSUE_ACCESS_TOKEN_SUCCESS = """
            {
              "success": true,
              "code": "S001",
              "message": "요청이 성공했습니다.",
              "data": {
                "accessToken": "eyJhbGciOiJIUzI1NiJ9.new-access-token",
                "provider": "google",
                "profileCompleted": true
              }
            }
            """;

    public static final String LOGOUT_SUCCESS = """
            {
              "success": true,
              "code": "S102",
              "message": "로그아웃이 완료되었습니다."
            }
            """;

    public static final String USER_PROFILE_SUCCESS = """
            {
              "success": true,
              "code": "S001",
              "message": "요청이 성공했습니다.",
              "data": {
                "nickname": "runner",
                "birthDate": "1998-04-05",
                "gender": "FEMALE",
                "height": 165.5,
                "weight": 52.3
              }
            }
            """;

    public static final String USER_PROFILE_UPDATE_REQUEST = """
            {
              "nickname": "runner",
              "birthDate": "1998-04-05",
              "gender": "FEMALE",
              "height": 165.5,
              "weight": 52.3
            }
            """;

    public static final String USER_PROFILE_UPDATE_SUCCESS = """
            {
              "success": true,
              "code": "S001",
              "message": "요청이 성공했습니다."
            }
            """;

    public static final String USER_NICKNAME_AVAILABILITY_SUCCESS = """
            {
              "success": true,
              "code": "S001",
              "message": "요청이 성공했습니다.",
              "data": {
                "available": true
              }
            }
            """;

    // 프로필 화면의 회원탈퇴 버튼이 기대하는 표준 성공 응답 예시입니다.
    public static final String USER_DELETE_SUCCESS = """
            {
              "success": true,
              "code": "S103",
              "message": "회원탈퇴가 완료되었습니다."
            }
            """;

    public static final String USER_RUNNING_PREFERENCE_SUCCESS = """
            {
              "success": true,
              "code": "S001",
              "message": "요청이 성공했습니다.",
              "data": {
                "purposes": ["HEALTH_MAINTENANCE", "DIET"],
                "timeSlots": ["EVENING", "MORNING"],
                "preferredDistanceKm": 5.0,
                "preferredDifficulty": "MEDIUM",
                "courseTypes": ["RIVERSIDE", "PARK"]
              }
            }
            """;

    public static final String USER_RUNNING_PREFERENCE_UPDATE_REQUEST = """
            {
              "purposes": ["HEALTH_MAINTENANCE", "DIET"],
              "timeSlots": ["EVENING", "MORNING"],
              "preferredDistanceKm": 5.0,
              "preferredDifficulty": "MEDIUM",
              "courseTypes": ["RIVERSIDE", "PARK"]
            }
            """;

    public static final String USER_RUNNING_PREFERENCE_UPDATE_SUCCESS = """
            {
              "success": true,
              "code": "S001",
              "message": "요청이 성공했습니다."
            }
            """;

    public static final String ERROR_UNAUTHORIZED = """
            {
              "success": false,
              "code": "E100",
              "message": "인증이 필요합니다.",
              "traceId": "1a2b3c4d"
            }
            """;

    public static final String ERROR_TOKEN_EXPIRED = """
            {
              "success": false,
              "code": "E101",
              "message": "토큰이 만료되었습니다.",
              "traceId": "1a2b3c4d"
            }
            """;

    public static final String ERROR_TOKEN_INVALID = """
            {
              "success": false,
              "code": "E102",
              "message": "유효하지 않은 토큰입니다.",
              "traceId": "1a2b3c4d"
            }
            """;

    public static final String ERROR_USER_NOT_FOUND = """
            {
              "success": false,
              "code": "E200",
              "message": "사용자를 찾을 수 없습니다.",
              "traceId": "1a2b3c4d"
            }
            """;

    public static final String ERROR_NICKNAME_ALREADY_EXISTS = """
            {
              "success": false,
              "code": "E203",
              "message": "이미 사용 중인 닉네임입니다.",
              "traceId": "1a2b3c4d"
            }
            """;

    public static final String VALIDATION_ERROR_USER_PROFILE = """
            {
              "success": false,
              "code": "E001",
              "message": "잘못된 입력값입니다.",
              "data": [
                {
                  "field": "nickname",
                  "rejectedValue": " ",
                  "message": "must not be blank"
                }
              ],
              "traceId": "1a2b3c4d"
            }
            """;

    public static final String VALIDATION_ERROR_USER_RUNNING_PREFERENCE = """
            {
              "success": false,
              "code": "E001",
              "message": "잘못된 입력값입니다.",
              "data": [
                {
                  "field": "preferredDistanceKm",
                  "rejectedValue": -3.0,
                  "message": "must be greater than 0"
                }
              ],
              "traceId": "1a2b3c4d"
            }
            """;
}
