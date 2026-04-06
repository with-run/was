package kr.withrun.was.domain.course.type;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "코스 거리 분류")
public enum CourseDistanceType {
    @Schema(description = "1km")
    ONE("1", "1km"),

    @Schema(description = "3km")
    THREE("3", "3km"),

    @Schema(description = "5km")
    FIVE("5", "5km"),

    @Schema(description = "10km")
    SEVEN("10", "10km");

    private final String data;
    private final String label;
}
