package kr.withrun.was.domain.navigation.service;

import kr.withrun.was.domain.course.repository.CourseRepository;
import kr.withrun.was.domain.file.service.CloudFrontSignedUrlService;
import kr.withrun.was.domain.navigation.dto.GenerateNavigationBundleResponse;
import kr.withrun.was.domain.navigation.dto.GetLatestNavigationBundleResponse;
import kr.withrun.was.domain.navigation.exception.NavigationBundleGenerationException;
import kr.withrun.was.domain.navigation.service.bundle.NavigationBundleGenerationService;
import kr.withrun.was.domain.navigation.type.NavigationBundleFailureCode;
import kr.withrun.was.domain.navigation.type.NavigationBundleStatus;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
/**
 * 컨트롤러 응답 형식에 맞춰 네비게이션 생성/조회 결과를 조합하는 API 서비스입니다.
 */
public class NavigationBundleService {

    private final CourseRepository courseRepository;
    private final NavigationBundleGenerationService navigationBundleGenerationService;
    private final CloudFrontSignedUrlService cloudFrontSignedUrlService;

    /**
     * 코스 조회와 번들 생성 서비스를 묶어 API 흐름을 구성하기 위한 생성자입니다.
     */
    public NavigationBundleService(
            CourseRepository courseRepository,
            NavigationBundleGenerationService navigationBundleGenerationService,
            CloudFrontSignedUrlService cloudFrontSignedUrlService
    ) {
        this.courseRepository = courseRepository;
        this.navigationBundleGenerationService = navigationBundleGenerationService;
        this.cloudFrontSignedUrlService = cloudFrontSignedUrlService;
    }

    /**
     * 최신 네비게이션 번들을 생성하고 API 응답 DTO 로 변환합니다.
     */
    public GenerateNavigationBundleResponse generateBundle(Long courseId) {
        try {
            navigationBundleGenerationService.generate(courseId);
            return new GenerateNavigationBundleResponse(courseId, NavigationBundleStatus.READY.name());
        } catch (NavigationBundleGenerationException exception) {
            if (exception.getFailureCode() == NavigationBundleFailureCode.COURSE_NOT_FOUND) {
                throw new CustomException(ResponseCode.COURSE_NOT_FOUND);
            }
            throw new CustomException(ResponseCode.NAVIGATION_BUNDLE_FAILED);
        }
    }

    /**
     * 코스에 저장된 최신 번들 URL 기준으로 READY/PENDING 응답을 만듭니다.
     */
    public GetLatestNavigationBundleResponse getLatestBundle(Long courseId) {
        return courseRepository.findNotDeletedCourse(courseId)
                .map(course -> {
                    if (StringUtils.hasText(course.getNavigationBundleUrl())) {
                        return new GetLatestNavigationBundleResponse(
                                courseId,
                                NavigationBundleStatus.READY.name(),
                                cloudFrontSignedUrlService.generateSignedUrl(course.getNavigationBundleUrl())
                        );
                    }
                    return createPendingResponse(courseId);
                })
                .orElseThrow(() -> new CustomException(ResponseCode.COURSE_NOT_FOUND));
    }

    /**
     * 번들이 아직 준비되지 않았을 때 사용하는 기본 PENDING 응답을 생성합니다.
     */
    private GetLatestNavigationBundleResponse createPendingResponse(Long courseId) {
        return new GetLatestNavigationBundleResponse(
                courseId,
                "PENDING",
                null
        );
    }

}
