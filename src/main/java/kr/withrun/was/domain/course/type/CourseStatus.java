package kr.withrun.was.domain.course.type;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "코스 공개 상태. OFFICIAL 은 공식 추천 코스, COMMUNITY 는 커뮤니티 코스, PRIVATE 는 비공개 코스다.")
public enum CourseStatus {
    @Schema(description = "공식 코스")
    OFFICIAL("OFFICIAL", "공식 코스"),

    @Schema(description = "커뮤니티 코스")
    COMMUNITY("COMMUNITY", "커뮤니티 코스"),

    @Schema(description = "개인 코스")
    PRIVATE("PRIVATE", "개인 코스");

    private final String data;
    private final String label;
}
