package kr.withrun.was.domain.auth.security;

// SecurityContext에 저장할 최소 인증 주체입니다.
// 현재 단계에서는 userId와 provider/profileCompleted만 있으면
// 프런트 분기와 "내 정보" 조회에 필요한 최소 정보를 전달할 수 있습니다.
public record AuthenticatedUser(
        Long userId,
        String provider,
        boolean profileCompleted
) {
}
