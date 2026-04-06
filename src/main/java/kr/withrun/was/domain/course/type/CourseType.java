package kr.withrun.was.domain.course.type;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "코스 유형 분류")
public enum CourseType {
    @Schema(description = "강변 중심 코스")
    RIVERSIDE("RIVERSIDE", "강변"),

    @Schema(description = "공원 중심 코스")
    PARK("PARK", "공원"),

    @Schema(description = "산책로 또는 트레일 중심 코스")
    MOUNTAIN_TRAIL("MOUNTAIN_TRAIL", "산악"),

    @Schema(description = "트랙 중심 코스")
    TRACK("TRACK", "트랙"),

    @Schema(description = "도심형 코스")
    URBAN("URBAN", "도심"),

    @Schema(description = "기타 유형 코스")
    OTHER("OTHER", "기타");

    private final String data;
    private final String label;

}
