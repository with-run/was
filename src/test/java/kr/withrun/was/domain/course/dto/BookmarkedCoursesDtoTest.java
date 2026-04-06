package kr.withrun.was.domain.course.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.type.RouteType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.RecordComponent;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("북마크 코스 조회 DTO")
class BookmarkedCoursesDtoTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @DisplayName("요청 레코드의 구성 요소를 정확히 노출한다")
    @Test
    void exposesExactRequestComponents() {
        assertThat(componentNamesOf(BookmarkedCoursesRequest.class))
                .containsExactly("size", "cursor");
    }

    @DisplayName("size를 생략하면 기본값 10을 사용한다")
    @Test
    void defaultsSizeTo10WhenMissing() {
        BookmarkedCoursesRequest request = new BookmarkedCoursesRequest(null, null);

        assertThat(request.size()).isEqualTo(10);
        assertThat(validate(request)).isEmpty();
    }

    @DisplayName("최대 size 50은 허용한다")
    @Test
    void acceptsMaximumSize() {
        Set<ConstraintViolation<BookmarkedCoursesRequest>> violations = validate(
                new BookmarkedCoursesRequest(50, null)
        );

        assertThat(violations).isEmpty();
    }

    @DisplayName("허용 범위를 벗어난 size 값은 거부한다")
    @ParameterizedTest(name = "size={0}")
    @ValueSource(ints = {0, -1, 51})
    void rejectsSizeOutsideSupportedRange(int size) {
        Set<ConstraintViolation<BookmarkedCoursesRequest>> violations = validate(
                new BookmarkedCoursesRequest(size, null)
        );

        assertOnlyViolationOn(violations, "size");
    }

    @DisplayName("북마크 코스 아이템 응답의 구성 요소를 정확히 노출한다")
    @Test
    void exposesExactBookmarkedCourseItemResponseComponents() {
        assertThat(componentNamesOf(BookmarkedCourseItemResponse.class))
                .containsExactly(
                        "bookmarkId",
                        "bookmarkedAt",
                        "courseId",
                        "title",
                        "status",
                        "routeType",
                        "distanceM",
                        "elevationGainM",
                        "difficulty",
                        "courseTypes",
                        "snapshotImageUrl",
                        "likeCount",
                        "isLiked",
                        "isBookmarked"
                );
    }

    @DisplayName("북마크 코스 응답의 구성 요소를 정확히 노출한다")
    @Test
    void exposesExactBookmarkedCoursesResponseComponents() {
        assertThat(componentNamesOf(BookmarkedCoursesResponse.class))
                .containsExactly("items", "hasMore", "nextCursor");
    }

    @DisplayName("북마크 코스 응답은 카드 정보를 그대로 담는다")
    @Test
    void storesBookmarkCourseCardFields() {
        BookmarkedCourseItemResponse item = new BookmarkedCourseItemResponse(
                11L,
                LocalDateTime.of(2026, 3, 14, 10, 30),
                42L,
                "Han River",
                CourseStatus.OFFICIAL,
                RouteType.LOOP,
                10000,
                35,
                new BookmarkedCourseItemResponse.DifficultyOption("MEDIUM", "보통"),
                List.of(
                        new BookmarkedCourseItemResponse.CourseTypeOption("RIVERSIDE", "강변"),
                        new BookmarkedCourseItemResponse.CourseTypeOption("PARK", "공원")
                ),
                "https://example.com/course.jpg",
                17L,
                true,
                true
        );
        BookmarkedCoursesResponse response = new BookmarkedCoursesResponse(List.of(item), true, "next-cursor");

        assertThat(response.items()).containsExactly(item);
        assertThat(response.hasMore()).isTrue();
        assertThat(response.nextCursor()).isEqualTo("next-cursor");
        assertThat(item.likeCount()).isEqualTo(17L);
        assertThat(item.isLiked()).isTrue();
        assertThat(item.routeType()).isEqualTo(RouteType.LOOP);
    }

    private Set<ConstraintViolation<BookmarkedCoursesRequest>> validate(BookmarkedCoursesRequest request) {
        return validator.validate(request);
    }

    private void assertOnlyViolationOn(Set<? extends ConstraintViolation<?>> violations, String propertyName) {
        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly(propertyName);
    }

    private String[] componentNamesOf(Class<?> type) {
        RecordComponent[] components = type.getRecordComponents();
        assertThat(components).isNotNull();
        return java.util.Arrays.stream(components)
                .map(RecordComponent::getName)
                .toArray(String[]::new);
    }
}
