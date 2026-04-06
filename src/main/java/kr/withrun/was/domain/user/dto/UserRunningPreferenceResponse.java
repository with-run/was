package kr.withrun.was.domain.user.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.domain.user.entity.UserPreferenceCourseType;
import kr.withrun.was.domain.user.entity.UserPreferencePurpose;
import kr.withrun.was.domain.user.entity.UserPreferenceTimeSlot;
import kr.withrun.was.domain.user.entity.UserRunningPreference;
import kr.withrun.was.domain.user.type.Purpose;
import kr.withrun.was.global.common.type.Difficulty;
import kr.withrun.was.global.common.type.TimeSlot;

import java.util.List;

@Schema(description = "내 러닝 기본 설정 조회 응답")
public record UserRunningPreferenceResponse(
        @ArraySchema(
                arraySchema = @Schema(description = "러닝 목적 목록"),
                schema = @Schema(implementation = Purpose.class)
        )
        List<Purpose> purposes,

        @ArraySchema(
                arraySchema = @Schema(description = "선호 시간대 목록"),
                schema = @Schema(implementation = TimeSlot.class)
        )
        List<TimeSlot> timeSlots,

        @Schema(description = "선호 거리(km)", example = "5.0", nullable = true)
        Double preferredDistanceKm,

        @Schema(description = "선호 난이도", implementation = Difficulty.class, nullable = true)
        Difficulty preferredDifficulty,

        @ArraySchema(
                arraySchema = @Schema(description = "선호 코스 타입 목록"),
                schema = @Schema(implementation = CourseType.class)
        )
        List<CourseType> courseTypes
) {
    public static UserRunningPreferenceResponse empty() {
        // 아직 저장된 기본 설정이 없는 초기 사용자를 위한 기본 응답입니다.
        return new UserRunningPreferenceResponse(List.of(), List.of(), null, null, List.of());
    }

    public static UserRunningPreferenceResponse from(UserRunningPreference preference) {
        if (preference == null) {
            return empty();
        }

        return new UserRunningPreferenceResponse(
                preference.getPurposes().stream()
                        .map(UserPreferencePurpose::getPurpose)
                        .toList(),
                preference.getTimeSlots().stream()
                        .map(UserPreferenceTimeSlot::getTimeSlot)
                        .toList(),
                preference.getPreferredDistanceKm(),
                preference.getPreferredDifficulty(),
                preference.getCourseTypes().stream()
                        .map(UserPreferenceCourseType::getCourseType)
                        .toList()
        );
    }
}
