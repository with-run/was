package kr.withrun.was.domain.course.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.type.NearbyCourseSortBy;

import java.util.List;

@Schema(description = "주변 코스 조회 요청")
public record NearbyCoursesRequest(
        @Schema(description = "사용자 위도", example = "37.5665", minimum = "-90.0", maximum = "90.0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @DecimalMin(value = "-90.0")
        @DecimalMax(value = "90.0")
        Double latitude,

        @Schema(description = "사용자 경도", example = "126.9780", minimum = "-180.0", maximum = "180.0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @DecimalMin(value = "-180.0")
        @DecimalMax(value = "180.0")
        Double longitude,

        @Schema(description = "검색 기준 위도", example = "37.5700", minimum = "-90.0", maximum = "90.0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @DecimalMin(value = "-90.0")
        @DecimalMax(value = "90.0")
        Double targetLatitude,

        @Schema(description = "검색 기준 경도", example = "126.9820", minimum = "-180.0", maximum = "180.0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @DecimalMin(value = "-180.0")
        @DecimalMax(value = "180.0")
        Double targetLongitude,

        @Schema(description = "검색 반경(m)", example = "3000", minimum = "1000", maximum = "5000", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @Min(1000)
        @Max(5000)
        Integer radiusM,

        @Schema(description = "선호 코스 거리 범위 목록", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty
        List<@Valid PreferredDistanceRange> preferredDistanceMs,

        @Schema(description = "조회에 사용할 코스 상태", example = "COMMUNITY", nullable = true)
        CourseStatus status,

        @Schema(
                description = "주변 코스 정렬 기준입니다. DISTANCE는 가까운 순, POPULAR는 좋아요 수와 북마크 수 합계가 많은 순으로 정렬합니다.",
                example = "DISTANCE",
                nullable = true
        )
        NearbyCourseSortBy sortBy,

        @Schema(description = "0부터 시작하는 페이지 번호입니다. 생략하면 0을 사용합니다.", example = "0", minimum = "0", nullable = true)
        @PositiveOrZero
        Integer page,

        @Schema(description = "페이지 크기입니다. 생략하면 3을 사용합니다.", example = "3", minimum = "1", maximum = "10", nullable = true)
        @Positive
        @Max(10)
        Integer size
) {
    public NearbyCoursesRequest {
        sortBy = sortBy == null ? NearbyCourseSortBy.DISTANCE : sortBy;
        page = page == null ? 0 : page;
        size = size == null ? 3 : size;
    }
}
