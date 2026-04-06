package kr.withrun.was.domain.navigation.dto.internal.bundle;

import kr.withrun.was.domain.navigation.type.NavigationBundleStatus;

import java.time.Instant;

/**
 * 번들 생성 파이프라인이 완료된 뒤 내부적으로 전달하는 결과 객체입니다.
 */
public record NavigationBundleGenerationResult(
        Long courseId,
        String storageKey,
        Instant generatedAt,
        NavigationBundleStatus status
) {
}
