package kr.withrun.was.domain.navigation.exception;

import kr.withrun.was.domain.navigation.type.NavigationBundleFailureCode;

/**
 * 네비게이션 번들 생성 과정의 도메인 실패 코드를 함께 전달하는 예외입니다.
 */
public class NavigationBundleGenerationException extends RuntimeException {

    private final NavigationBundleFailureCode failureCode;

    /**
     * 실패 코드와 메시지로 예외를 생성합니다.
     */
    public NavigationBundleGenerationException(NavigationBundleFailureCode failureCode, String message) {
        super(message);
        this.failureCode = failureCode;
    }

    /**
     * 실패 코드와 원인 예외를 함께 보존하는 예외를 생성합니다.
     */
    public NavigationBundleGenerationException(
            NavigationBundleFailureCode failureCode,
            String message,
            Throwable cause
    ) {
        super(message, cause);
        this.failureCode = failureCode;
    }

    /**
     * 상위 계층에서 응답 변환에 사용할 실패 코드를 반환합니다.
     */
    public NavigationBundleFailureCode getFailureCode() {
        return failureCode;
    }
}
