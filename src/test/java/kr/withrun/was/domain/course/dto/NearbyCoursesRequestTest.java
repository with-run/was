package kr.withrun.was.domain.course.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.type.NearbyCourseSortBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.RecordComponent;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Nearby courses request DTO")
class NearbyCoursesRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void exposesExactRequestComponents() {
        assertThat(componentNamesOf(NearbyCoursesRequest.class))
                .containsExactly(
                        "latitude",
                        "longitude",
                        "targetLatitude",
                        "targetLongitude",
                        "radiusM",
                        "preferredDistanceMs",
                        "status",
                        "sortBy",
                        "page",
                        "size"
                );
    }

    @Test
    void acceptsRequestWhenOptionalPageAndSizeAreOmitted() {
        NearbyCoursesRequest request = request(3000, 37.5700, 126.9820, defaultRanges(), null, null);
        Set<ConstraintViolation<NearbyCoursesRequest>> violations = validate(request);

        assertThat(violations).isEmpty();
        assertThat(request.sortBy()).isEqualTo(NearbyCourseSortBy.DISTANCE);
    }

    @Test
    void acceptsCommunityStatusFilter() {
        NearbyCoursesRequest request = request(37.5665, 126.9780, 37.5700, 126.9820, 3000, defaultRanges(), CourseStatus.COMMUNITY, null, null, null);
        Set<ConstraintViolation<NearbyCoursesRequest>> violations = validate(request);

        assertThat(violations).isEmpty();
        assertThat(request.status()).isEqualTo(CourseStatus.COMMUNITY);
    }

    @Test
    void rejectsMissingRadiusM() {
        Set<ConstraintViolation<NearbyCoursesRequest>> violations = validate(
                request(null, 37.5700, 126.9820, defaultRanges(), null, null)
        );

        assertOnlyViolationOn(violations, "radiusM");
    }

    @ParameterizedTest(name = "radiusM={0}")
    @ValueSource(ints = {0, 6000})
    void rejectsRadiusMOutsideSupportedRange(int radiusM) {
        Set<ConstraintViolation<NearbyCoursesRequest>> violations = validate(
                request(radiusM, 37.5700, 126.9820, defaultRanges(), null, null)
        );

        assertOnlyViolationOn(violations, "radiusM");
    }

    @Test
    void acceptsRadiusMThatIsNotMultipleOf1000WhenItIsWithinRange() {
        Set<ConstraintViolation<NearbyCoursesRequest>> violations = validate(
                request(1500, 37.5700, 126.9820, defaultRanges(), null, null)
        );

        assertThat(violations).isEmpty();
    }

    @Test
    void rejectsMissingTargetLatitude() {
        Set<ConstraintViolation<NearbyCoursesRequest>> violations = validate(
                request(3000, null, 126.9820, defaultRanges(), null, null)
        );

        assertOnlyViolationOn(violations, "targetLatitude");
    }

    @Test
    void rejectsMissingTargetLongitude() {
        Set<ConstraintViolation<NearbyCoursesRequest>> violations = validate(
                request(3000, 37.5700, null, defaultRanges(), null, null)
        );

        assertOnlyViolationOn(violations, "targetLongitude");
    }

    @Test
    void rejectsMissingPreferredDistanceMs() {
        Set<ConstraintViolation<NearbyCoursesRequest>> violations = validate(
                request(3000, 37.5700, 126.9820, null, null, null)
        );

        assertOnlyViolationOn(violations, "preferredDistanceMs");
    }

    @ParameterizedTest(name = "latitude={0}")
    @ValueSource(doubles = {-90.1, 90.1})
    void rejectsLatitudeOutsideSupportedRange(double latitude) {
        Set<ConstraintViolation<NearbyCoursesRequest>> violations = validate(
                request(latitude, 126.9780, 37.5700, 126.9820, 3000, defaultRanges(), null, null, null, null)
        );

        assertOnlyViolationOn(violations, "latitude");
    }

    @ParameterizedTest(name = "longitude={0}")
    @ValueSource(doubles = {-180.1, 180.1})
    void rejectsLongitudeOutsideSupportedRange(double longitude) {
        Set<ConstraintViolation<NearbyCoursesRequest>> violations = validate(
                request(37.5665, longitude, 37.5700, 126.9820, 3000, defaultRanges(), null, null, null, null)
        );

        assertOnlyViolationOn(violations, "longitude");
    }

    @ParameterizedTest(name = "targetLatitude={0}")
    @ValueSource(doubles = {-90.1, 90.1})
    void rejectsTargetLatitudeOutsideSupportedRange(double targetLatitude) {
        Set<ConstraintViolation<NearbyCoursesRequest>> violations = validate(
                request(3000, targetLatitude, 126.9820, defaultRanges(), null, null)
        );

        assertOnlyViolationOn(violations, "targetLatitude");
    }

    @ParameterizedTest(name = "targetLongitude={0}")
    @ValueSource(doubles = {-180.1, 180.1})
    void rejectsTargetLongitudeOutsideSupportedRange(double targetLongitude) {
        Set<ConstraintViolation<NearbyCoursesRequest>> violations = validate(
                request(3000, 37.5700, targetLongitude, defaultRanges(), null, null)
        );

        assertOnlyViolationOn(violations, "targetLongitude");
    }

    @Test
    void acceptsMissingPageAndSize() {
        Set<ConstraintViolation<NearbyCoursesRequest>> violations = validate(
                request(3000, 37.5700, 126.9820, defaultRanges(), null, null)
        );

        assertThat(violations).isEmpty();
    }

    @Test
    void acceptsMaximumSize() {
        Set<ConstraintViolation<NearbyCoursesRequest>> violations = validate(
                request(3000, 37.5700, 126.9820, defaultRanges(), 0, 10)
        );

        assertThat(violations).isEmpty();
    }

    @ParameterizedTest(name = "size={0}")
    @ValueSource(ints = {0, -1, 11})
    void rejectsSizeOutsideSupportedRange(int size) {
        Set<ConstraintViolation<NearbyCoursesRequest>> violations = validate(
                request(3000, 37.5700, 126.9820, defaultRanges(), 0, size)
        );

        assertOnlyViolationOn(violations, "size");
    }

    @ParameterizedTest(name = "page={0}")
    @ValueSource(ints = {-1})
    void rejectsNegativePage(int page) {
        Set<ConstraintViolation<NearbyCoursesRequest>> violations = validate(
                request(3000, 37.5700, 126.9820, defaultRanges(), page, 3)
        );

        assertOnlyViolationOn(violations, "page");
    }

    @Test
    void exposesExactNearbyCourseItemResponseComponents() {
        assertThat(componentNamesOf(NearbyCourseItemResponse.class))
                .containsExactly(
                        "courseId",
                        "title",
                        "status",
                        "routeType",
                        "distanceM",
                        "elevationGainM",
                        "startLatitude",
                        "startLongitude",
                        "endLatitude",
                        "endLongitude",
                        "distanceFromTargetM",
                        "distanceFromUserM",
                        "difficulty",
                        "courseTypes",
                        "isRecommended",
                        "likeCount",
                        "bookmarkCount",
                        "isLiked",
                        "isBookmarked",
                        "snapshotImageUrl"
                );
    }

    @Test
    void exposesExactNearbyCoursesResponseComponents() {
        assertThat(componentNamesOf(NearbyCoursesResponse.class))
                .containsExactly("items", "page", "size", "totalElements", "totalPages", "hasNext");
    }

    private Set<ConstraintViolation<NearbyCoursesRequest>> validate(NearbyCoursesRequest request) {
        return validator.validate(request);
    }

    private NearbyCoursesRequest request(
            Integer radiusM,
            Double targetLatitude,
            Double targetLongitude,
            List<PreferredDistanceRange> preferredDistanceMs,
            Integer page,
            Integer size
    ) {
        return request(37.5665, 126.9780, targetLatitude, targetLongitude, radiusM, preferredDistanceMs, null, null, page, size);
    }

    private NearbyCoursesRequest request(
            Double latitude,
            Double longitude,
            Double targetLatitude,
            Double targetLongitude,
            Integer radiusM,
            List<PreferredDistanceRange> preferredDistanceMs,
            CourseStatus status,
            NearbyCourseSortBy sortBy,
            Integer page,
            Integer size
    ) {
        return new NearbyCoursesRequest(
                latitude,
                longitude,
                targetLatitude,
                targetLongitude,
                radiusM,
                preferredDistanceMs,
                status,
                sortBy,
                page,
                size
        );
    }

    private List<PreferredDistanceRange> defaultRanges() {
        return List.of(
                new PreferredDistanceRange(1, 3000),
                new PreferredDistanceRange(3001, 5000)
        );
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
