package kr.withrun.was.domain.running.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.withrun.was.domain.running.entity.RunningSession;
import kr.withrun.was.domain.running.type.RunningMode;
import kr.withrun.was.domain.running.type.RunningSessionCompleteState;
import org.springframework.util.StringUtils;

import java.util.List;

@Schema(description = "러닝 세션 시작 결과")
public record CreateRunningSessionResponse(
        @Schema(description = "생성된 러닝 세션 ID", example = "91")
        Long runningSessionId,

        @Schema(description = "생성된 세션의 러닝 모드")
        RunningMode mode,

        @Schema(description = "연결된 코스 ID. 자유 러닝이면 null 이다.", example = "12", nullable = true)
        Long courseId,

        @Schema(
                description = "서명된 네비게이션 번들 URL. 코스가 없거나 아직 생성되지 않았으면 null 이다.",
                example = "https://cdn.withrun.kr/navigation/latest/12.json?Expires=100&Signature=test-signature&Key-Pair-Id=K123",
                nullable = true
        )
        String navigationBundleUrl,

        @Schema(description = "목표 고스트 세션 ID. 고스트 러닝이 아니면 null 이다.", example = "77", nullable = true)
        Long ghostTargetRunningSessionId,

        @Schema(description = "고스트 대상 세션의 GPS 샘플 목록. GHOST 모드에서 fallback 대상이 없으면 빈 배열이고, 고스트 러닝이 아니면 null 이다.", nullable = true)
        List<RunningGpsSampleResponse> ghostTargetGpsSamples,

        @Schema(description = "현재 누적 거리(m). 시작 직후에는 0 이다.", example = "0")
        int distanceM,

        @Schema(description = "현재 누적 칼로리(kcal). 시작 직후에는 0 이다.", example = "0")
        int caloriesKcal,

        @Schema(description = "러닝 세션 종료 상태. 시작 직후 기본값은 FAIL 이다.", example = "FAIL")
        RunningSessionCompleteState completeState
) {

    public static CreateRunningSessionResponse from(
            RunningSession runningSession,
            List<RunningGpsSampleResponse> ghostTargetGpsSamples,
            String navigationBundleUrl
    ) {
        return new CreateRunningSessionResponse(
                runningSession.getId(),
                runningSession.getMode(),
                runningSession.getCourse() != null
                        ? runningSession.getCourse().getId()
                        : null,
                normalizeNavigationBundleUrl(navigationBundleUrl),
                runningSession.getGhostTargetRunningSession() != null
                        ? runningSession.getGhostTargetRunningSession().getId()
                        : null,
                ghostTargetGpsSamples,
                runningSession.getDistanceM(),
                runningSession.getCaloriesKcal(),
                runningSession.getCompleteState()
        );
    }

    private static String normalizeNavigationBundleUrl(String navigationBundleUrl) {
        return StringUtils.hasText(navigationBundleUrl) ? navigationBundleUrl : null;
    }
}
