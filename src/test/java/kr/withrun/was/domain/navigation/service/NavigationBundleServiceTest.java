package kr.withrun.was.domain.navigation.service;

import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.repository.CourseRepository;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.vo.Coordinates;
import kr.withrun.was.domain.file.service.CloudFrontSignedUrlService;
import kr.withrun.was.domain.navigation.dto.GenerateNavigationBundleResponse;
import kr.withrun.was.domain.navigation.dto.GetLatestNavigationBundleResponse;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleGenerationResult;
import kr.withrun.was.domain.navigation.exception.NavigationBundleGenerationException;
import kr.withrun.was.domain.navigation.service.bundle.NavigationBundleGenerationService;
import kr.withrun.was.domain.navigation.type.NavigationBundleFailureCode;
import kr.withrun.was.domain.navigation.type.NavigationBundleStatus;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("네비게이션 API 서비스")
class NavigationBundleServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private NavigationBundleGenerationService navigationBundleGenerationService;

    @Mock
    private CloudFrontSignedUrlService cloudFrontSignedUrlService;

    private NavigationBundleService navigationBundleService;

    @BeforeEach
    void setUp() {
        navigationBundleService = new NavigationBundleService(
                courseRepository,
                navigationBundleGenerationService,
                cloudFrontSignedUrlService
        );
    }

    @DisplayName("generate 요청은 동기 생성 완료 응답을 반환한다")
    @Test
    void generatesLatestNavigationBundle() {
        when(navigationBundleGenerationService.generate(42L))
                .thenReturn(new NavigationBundleGenerationResult(
                        42L,
                        "navigation/latest/42.json",
                        java.time.Instant.parse("2026-03-24T10:00:00Z"),
                        NavigationBundleStatus.READY
                ));

        GenerateNavigationBundleResponse response = navigationBundleService.generateBundle(42L);

        assertThat(response.courseId()).isEqualTo(42L);
        assertThat(response.bundleStatus()).isEqualTo("READY");
        verify(navigationBundleGenerationService).generate(42L);
    }

    @DisplayName("generate 중 예외가 발생하면 NAVIGATION_BUNDLE_FAILED 예외로 변환한다")
    @Test
    void throwsNavigationBundleFailedWhenGenerationFails() {
        when(navigationBundleGenerationService.generate(42L))
                .thenThrow(new NavigationBundleGenerationException(
                        NavigationBundleFailureCode.BUNDLE_STORAGE_FAILED,
                        "storage failed"
                ));

        assertThatThrownBy(() -> navigationBundleService.generateBundle(42L))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.NAVIGATION_BUNDLE_FAILED);
    }

    @DisplayName("코스에 저장된 navigation bundle object key 가 있으면 signed URL 로 READY 응답을 반환한다")
    @Test
    void returnsReadyLatestNavigationBundle() {
        Course course = course(42L);
        course.updateNavigationBundleUrl("navigation/latest/42.json");

        when(courseRepository.findNotDeletedCourse(42L)).thenReturn(Optional.of(course));
        when(cloudFrontSignedUrlService.generateSignedUrl("navigation/latest/42.json"))
                .thenReturn("signed::navigation/latest/42.json");

        GetLatestNavigationBundleResponse response = navigationBundleService.getLatestBundle(42L);

        assertThat(response.status()).isEqualTo("READY");
        assertThat(response.downloadUrl()).isEqualTo("signed::navigation/latest/42.json");
    }

    @DisplayName("코스에 기존 CDN navigation bundle URL 이 저장돼 있어도 signed URL 로 READY 응답을 반환한다")
    @Test
    void returnsReadyLatestNavigationBundleForLegacyCdnUrl() {
        Course course = course(42L);
        course.updateNavigationBundleUrl("https://cdn.withrun.kr/navigation/latest/42.json");

        when(courseRepository.findNotDeletedCourse(42L)).thenReturn(Optional.of(course));
        when(cloudFrontSignedUrlService.generateSignedUrl("https://cdn.withrun.kr/navigation/latest/42.json"))
                .thenReturn("signed::navigation/latest/42.json");

        GetLatestNavigationBundleResponse response = navigationBundleService.getLatestBundle(42L);

        assertThat(response.status()).isEqualTo("READY");
        assertThat(response.downloadUrl()).isEqualTo("signed::navigation/latest/42.json");
    }

    @DisplayName("코스에 navigation bundle URL 이 없으면 PENDING 응답을 반환한다")
    @Test
    void returnsPendingWhenNavigationBundleUrlDoesNotExist() {
        when(courseRepository.findNotDeletedCourse(42L)).thenReturn(Optional.of(course(42L)));

        GetLatestNavigationBundleResponse response = navigationBundleService.getLatestBundle(42L);

        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.downloadUrl()).isNull();
    }

    @DisplayName("코스가 없으면 COURSE_NOT_FOUND 예외를 던진다")
    @Test
    void throwsCourseNotFoundWhenCourseDoesNotExist() {
        when(navigationBundleGenerationService.generate(99L))
                .thenThrow(new NavigationBundleGenerationException(
                        NavigationBundleFailureCode.COURSE_NOT_FOUND,
                        "course not found"
                ));

        assertThatThrownBy(() -> navigationBundleService.generateBundle(99L))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.COURSE_NOT_FOUND);
    }

    private static Course course(Long id) {
        Course course = Course.builder()
                .title("Course " + id)
                .status(CourseStatus.OFFICIAL)
                .distanceM(5000)
                .elevationGainM(20)
                .startLatitude(37.5)
                .startLongitude(127.0)
                .endLatitude(37.6)
                .endLongitude(127.1)
                .coordinates(new Coordinates(List.of(37.5, 37.6), List.of(127.0, 127.1), List.of(10.0, 12.0)))
                .build();
        setField(course, "id", id);
        return course;
    }

    private static void setField(Object target, String fieldName, Object value) {
        Class<?> currentClass = target.getClass();
        while (currentClass != null) {
            try {
                java.lang.reflect.Field field = currentClass.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException exception) {
                currentClass = currentClass.getSuperclass();
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException("Failed to set field " + fieldName, exception);
            }
        }

        throw new IllegalArgumentException("Field not found: " + fieldName);
    }
}
