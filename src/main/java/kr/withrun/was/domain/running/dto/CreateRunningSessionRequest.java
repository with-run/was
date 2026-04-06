package kr.withrun.was.domain.running.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import kr.withrun.was.domain.running.type.RunningMode;

@Schema(description = "러닝 세션 시작 요청")
public record CreateRunningSessionRequest(
        @Schema(description = "러닝 모드. FREE 는 자유 러닝, COURSE 는 공식 코스 러닝, GHOST 는 완료된 세션을 목표로 하는 고스트 러닝이다.", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        RunningMode mode,

        @Schema(description = "공식 코스 러닝일 때 사용할 코스 ID. FREE 모드에서는 비워둘 수 있다.", example = "12", nullable = true)
        @Positive
        Long courseId,

        @Schema(description = "GHOST 모드일 때 목표가 되는 완료된 러닝 세션 ID. 비우면 같은 코스의 내 최고 리더보드 기록을 우선 사용한다.", example = "91", nullable = true)
        @Positive
        Long ghostTargetRunningSessionId,

        @Schema(description = "러닝 시작 지점 위도", example = "37.5665", minimum = "-90.0", maximum = "90.0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @DecimalMin(value = "-90.0")
        @DecimalMax(value = "90.0")
        Double startLatitude,

        @Schema(description = "러닝 시작 지점 경도", example = "126.9780", minimum = "-180.0", maximum = "180.0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @DecimalMin(value = "-180.0")
        @DecimalMax(value = "180.0")
        Double startLongitude
) {
}
