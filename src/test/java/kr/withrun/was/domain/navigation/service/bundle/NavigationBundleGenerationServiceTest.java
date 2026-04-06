package kr.withrun.was.domain.navigation.service.bundle;

import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.repository.CourseRepository;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.vo.Coordinates;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleDocument;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleGenerationResult;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleManeuver;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleManeuverSampleAction;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleManeuverType;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleMetadata;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleSegment;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleShapePoint;
import kr.withrun.was.domain.navigation.exception.NavigationBundleGenerationException;
import kr.withrun.was.domain.navigation.service.artifact.NavigationBundleStore;
import kr.withrun.was.domain.navigation.service.bundle.polyline.PolylineBundleDocumentFactory;
import kr.withrun.was.domain.navigation.type.NavigationBundleFailureCode;
import kr.withrun.was.domain.navigation.type.NavigationBundleStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("최신 네비게이션 번들 생성 서비스")
class NavigationBundleGenerationServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private PolylineBundleDocumentFactory polylineBundleDocumentFactory;

    @Mock
    private NavigationBundleDocumentValidator navigationBundleDocumentValidator;

    @Mock
    private NavigationBundleStore navigationBundleStore;

    private NavigationBundleGenerationService navigationBundleGenerationService;

    @BeforeEach
    void setUp() {
        navigationBundleGenerationService = new NavigationBundleGenerationService(
                courseRepository,
                polylineBundleDocumentFactory,
                navigationBundleDocumentValidator,
                navigationBundleStore
        );
    }

    @DisplayName("Course.coordinates 로 최신 번들을 생성하고 저장한다")
    @Test
    void generatesLatestNavigationBundle() {
        Course course = course(10L);
        Coordinates coordinates = course.getCoordinates();
        NavigationBundleDocument document = validDocument();

        when(courseRepository.findNotDeletedCourse(10L)).thenReturn(Optional.of(course));
        when(polylineBundleDocumentFactory.build(coordinates)).thenReturn(document);
        when(navigationBundleStore.storeLatest(10L, document)).thenReturn("navigation/latest/10.json");

        NavigationBundleGenerationResult result = navigationBundleGenerationService.generate(10L);

        assertThat(result.courseId()).isEqualTo(10L);
        assertThat(result.storageKey()).isEqualTo("navigation/latest/10.json");
        assertThat(result.status()).isEqualTo(NavigationBundleStatus.READY);
        assertThat(result.generatedAt()).isEqualTo(Instant.parse("2026-03-24T07:35:00Z"));
        assertThat(course.getNavigationBundleUrl()).isEqualTo("navigation/latest/10.json");
        verify(polylineBundleDocumentFactory).build(coordinates);
        verify(navigationBundleDocumentValidator).validate(document);
    }

    @DisplayName("잘못된 coordinates 는 저장 전에 즉시 실패한다")
    @Test
    void failsFastWhenCoordinatesAreInvalid() {
        Course course = course(10L);
        NavigationBundleGenerationException failure = new NavigationBundleGenerationException(
                NavigationBundleFailureCode.INVALID_COURSE_COORDINATES,
                "At least two course coordinates are required"
        );

        when(courseRepository.findNotDeletedCourse(10L)).thenReturn(Optional.of(course));
        when(polylineBundleDocumentFactory.build(course.getCoordinates())).thenThrow(failure);

        assertThatThrownBy(() -> navigationBundleGenerationService.generate(10L))
                .isSameAs(failure);
        verifyNoInteractions(navigationBundleStore, navigationBundleDocumentValidator);
    }

    @DisplayName("코스가 없으면 COURSE_NOT_FOUND 실패로 종료한다")
    @Test
    void failsWhenCourseDoesNotExist() {
        when(courseRepository.findNotDeletedCourse(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> navigationBundleGenerationService.generate(99L))
                .isInstanceOf(NavigationBundleGenerationException.class)
                .extracting("failureCode")
                .isEqualTo(NavigationBundleFailureCode.COURSE_NOT_FOUND);
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
        setId(course, id);
        return course;
    }

    private static void setId(Course course, Long id) {
        try {
            java.lang.reflect.Field field = Course.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(course, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to set course id", exception);
        }
    }

    private static NavigationBundleDocument validDocument() {
        return new NavigationBundleDocument(
                List.of(
                        new NavigationBundleShapePoint(37.5, 127.0, 10.0),
                        new NavigationBundleShapePoint(37.6, 127.1, 12.0)
                ),
                List.of(new NavigationBundleSegment(0, 1, 1200d, 45, 0d, 1200d)),
                List.of(new NavigationBundleManeuver(
                        NavigationBundleManeuverType.ARRIVAL,
                        null,
                        NavigationBundleManeuverSampleAction.ARRIVAL,
                        "목적지에 도착",
                        1,
                        1200d,
                        null,
                        null,
                        null,
                        null,
                        null
                )),
                1200d,
                new NavigationBundleMetadata(
                        Instant.parse("2026-03-24T07:35:00Z"),
                        "polyline-navigation-bundle",
                        "v1",
                        "ko-KR",
                        "course.coordinates",
                        2,
                        1
                )
        );
    }
}
