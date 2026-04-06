package kr.withrun.was.domain.navigation.type;

/**
 * latest navigation bundle 생성 실패 원인을 구분하기 위한 enum 입니다.
 */
public enum NavigationBundleFailureCode {
    COURSE_NOT_FOUND,
    INVALID_COURSE_COORDINATES,
    VALHALLA_TRACE_ROUTE_FAILED,
    VALHALLA_TRACE_ATTRIBUTES_FAILED,
    BUNDLE_VALIDATION_FAILED,
    BUNDLE_STORAGE_FAILED
}
