package kr.withrun.was.domain.navigation.service.bundle;

import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.repository.CourseRepository;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleDocument;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleGenerationResult;
import kr.withrun.was.domain.navigation.exception.NavigationBundleGenerationException;
import kr.withrun.was.domain.navigation.service.artifact.NavigationBundleStore;
import kr.withrun.was.domain.navigation.service.bundle.polyline.PolylineBundleDocumentFactory;
import kr.withrun.was.domain.navigation.type.NavigationBundleFailureCode;
import kr.withrun.was.domain.navigation.type.NavigationBundleStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
/**
 * 코스 좌표를 기반으로 latest navigation bundle 을 동기 생성하고 저장하는 서비스입니다.
 */
public class NavigationBundleGenerationService {

    private final CourseRepository courseRepository;
    private final PolylineBundleDocumentFactory polylineBundleDocumentFactory;
    private final NavigationBundleDocumentValidator navigationBundleDocumentValidator;
    private final NavigationBundleStore navigationBundleStore;

    /**
     * 번들 생성 파이프라인에 필요한 구성 요소를 주입받는 생성자입니다.
     */
    public NavigationBundleGenerationService(
            CourseRepository courseRepository,
            PolylineBundleDocumentFactory polylineBundleDocumentFactory,
            NavigationBundleDocumentValidator navigationBundleDocumentValidator,
            NavigationBundleStore navigationBundleStore
    ) {
        this.courseRepository = courseRepository;
        this.polylineBundleDocumentFactory = polylineBundleDocumentFactory;
        this.navigationBundleDocumentValidator = navigationBundleDocumentValidator;
        this.navigationBundleStore = navigationBundleStore;
    }

    @Transactional
    /**
     * 코스를 읽어 Valhalla 호출부터 S3 저장, URL 반영까지 한 번에 수행합니다.
     */
    public NavigationBundleGenerationResult generate(Long courseId) {
        Course course = courseRepository.findNotDeletedCourse(courseId)
                .orElseThrow(() -> new NavigationBundleGenerationException(
                        NavigationBundleFailureCode.COURSE_NOT_FOUND,
                        "Course not found for latest navigation bundle generation: " + courseId
                ));

        NavigationBundleDocument document = polylineBundleDocumentFactory.build(course.getCoordinates());

        navigationBundleDocumentValidator.validate(document);
        String storageKey = navigationBundleStore.storeLatest(courseId, document);
        course.updateNavigationBundleUrl(storageKey);
        courseRepository.save(course);

        return new NavigationBundleGenerationResult(
                courseId,
                storageKey,
                document.metadata().generatedAt(),
                NavigationBundleStatus.READY
        );
    }
}
