package kr.withrun.was.domain.running.type;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "러닝 세션 종료 상태. 진행 중인 세션은 null 이다.")
public enum RunningSessionCompleteState {
    @Schema(description = "러닝이 실패로 종료된 상태")
    FAIL,

    @Schema(description = "러닝이 성공적으로 종료된 상태")
    SUCCESS,

    @Schema(description = "러닝을 중도 포기한 상태")
    GIVEUP
}
