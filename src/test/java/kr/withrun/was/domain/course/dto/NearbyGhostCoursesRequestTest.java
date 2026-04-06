package kr.withrun.was.domain.course.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import kr.withrun.was.domain.course.type.NearbyGhostCourseSortBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("주변 고스트 런 코스 요청 DTO")
class NearbyGhostCoursesRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void exposesExactRequestComponents() {
        assertThat(componentNamesOf(NearbyGhostCoursesRequest.class))
                .containsExactly(
                        "latitude",
                        "longitude",
                        "targetLatitude",
                        "targetLongitude",
                        "radiusM",
                        "preferredDistanceMs",
                        "sortBy",
                        "page",
                        "size"
                );
    }

    @Test
    void appliesDefaultSortAndPagingWhenOptionalFieldsAreOmitted() {
        NearbyGhostCoursesRequest request = new NearbyGhostCoursesRequest(
                37.5665,
                126.9780,
                37.5700,
                126.9820,
                3000,
                defaultRanges(),
                null,
                null,
                null
        );

        Set<ConstraintViolation<NearbyGhostCoursesRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
        assertThat(request.sortBy()).isEqualTo(NearbyGhostCourseSortBy.DISTANCE);
        assertThat(request.page()).isEqualTo(0);
        assertThat(request.size()).isEqualTo(3);
    }

    @Test
    void acceptsGhostRunCountSort() {
        NearbyGhostCoursesRequest request = new NearbyGhostCoursesRequest(
                37.5665,
                126.9780,
                37.5700,
                126.9820,
                3000,
                defaultRanges(),
                NearbyGhostCourseSortBy.GHOST_RUN_COUNT,
                1,
                5
        );

        Set<ConstraintViolation<NearbyGhostCoursesRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
        assertThat(request.sortBy()).isEqualTo(NearbyGhostCourseSortBy.GHOST_RUN_COUNT);
    }

    @Test
    void exposesExactResponseComponents() {
        assertThat(componentNamesOf(NearbyGhostCoursesResponse.class))
                .containsExactly("items", "page", "size", "totalElements", "totalPages", "hasNext");
    }

    private List<PreferredDistanceRange> defaultRanges() {
        return List.of(
                new PreferredDistanceRange(1, 3000),
                new PreferredDistanceRange(3001, 5000)
        );
    }

    private String[] componentNamesOf(Class<?> type) {
        RecordComponent[] components = type.getRecordComponents();
        assertThat(components).isNotNull();
        return java.util.Arrays.stream(components)
                .map(RecordComponent::getName)
                .toArray(String[]::new);
    }
}
