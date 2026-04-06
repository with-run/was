package kr.withrun.was.domain.navigation.dto.internal.bundle;

/**
 * 번들에 저장되는 원본 shape point 입니다.
 */
public record NavigationBundleShapePoint(
        double lat,
        double lon,
        Double elevationM
) {
}
