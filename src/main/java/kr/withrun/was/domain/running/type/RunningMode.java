package kr.withrun.was.domain.running.type;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "러닝 실행 모드. FREE 는 자유 러닝, COURSE 는 공식 코스 러닝, GHOST 는 완료된 세션을 목표로 하는 고스트 러닝이다.")
public enum RunningMode {
    @Schema(description = "코스 없이 자유롭게 기록하는 러닝")
    FREE,

    @Schema(description = "공식 코스를 따라 달리는 러닝")
    COURSE,

    @Schema(description = "완료된 러닝 세션 기록과 경쟁하는 고스트 러닝")
    GHOST
}
