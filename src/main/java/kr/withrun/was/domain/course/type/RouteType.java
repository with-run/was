package kr.withrun.was.domain.course.type;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "코스 경로 형태")
public enum RouteType {
    @Schema(description = "출발 지점으로 다시 돌아오는 순환형 코스")
    LOOP("LOOP", "순환형"),

    @Schema(description = "출발 지점에서 반환 지점까지 왕복하는 코스")
    OUT_AND_BACK("OUT_AND_BACK", "왕복형");

    private final String data;
    private final String label;
}
