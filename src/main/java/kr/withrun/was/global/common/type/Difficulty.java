package kr.withrun.was.global.common.type;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "체감 난이도 분류")
public enum Difficulty {
    @Schema(description = "쉬운 난이도")
    EASY("EASY", "쉬움"),

    @Schema(description = "보통 난이도")
    MEDIUM("MEDIUM", "보통"),

    @Schema(description = "어려운 난이도")
    HARD("HARD", "어려움");

    private final String data;
    private final String label;

}
