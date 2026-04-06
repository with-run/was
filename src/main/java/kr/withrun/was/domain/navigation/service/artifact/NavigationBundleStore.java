package kr.withrun.was.domain.navigation.service.artifact;

import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleDocument;

/**
 * 생성된 네비게이션 번들을 외부 저장소에 보관하는 저장 인터페이스입니다.
 */
public interface NavigationBundleStore {

    /**
     * 코스별 latest navigation bundle 을 저장하고 저장 key 를 반환합니다.
     */
    String storeLatest(Long courseId, NavigationBundleDocument document);
}
