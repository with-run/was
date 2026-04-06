package kr.withrun.was.domain.navigation.dto.internal.bundle;

/**
 * 클라이언트용 번들에 저장되는 음성 안내 항목입니다.
 */
public record NavigationBundleVoiceInstruction(
        double distanceAlongGeometry,
        String announcement,
        String ssmlAnnouncement
) {
}
