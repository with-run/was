package kr.withrun.was.domain.course.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.domain.course.type.RouteType;
import kr.withrun.was.global.common.type.Difficulty;

import java.util.List;

@Schema(description = "주변 코스 요약 정보")
public record NearbyCourseItemResponse(
        @Schema(description = "코스 ID", example = "12")
        Long courseId,

        @Schema(description = "코스 제목", example = "한강 야간 러닝 5K")
        String title,

        @Schema(description = "코스 상태")
        CourseStatus status,

        @Schema(description = "경로 유형", nullable = true)
        RouteType routeType,

        @Schema(description = "코스 총 거리(m)", example = "5200")
        Integer distanceM,

        @Schema(description = "코스 총 누적 상승고도(m)", example = "48")
        Integer elevationGainM,

        @Schema(description = "코스 시작 지점 위도", example = "37.5661")
        Double startLatitude,

        @Schema(description = "코스 시작 지점 경도", example = "126.9738")
        Double startLongitude,

        @Schema(description = "코스 종료 지점 위도", example = "37.5702")
        Double endLatitude,

        @Schema(description = "코스 종료 지점 경도", example = "126.9814")
        Double endLongitude,

        @Schema(description = "검색 기준 위치와 코스 사이의 최소 거리(m)", example = "320")
        Integer distanceFromTargetM,

        @Schema(description = "사용자 현재 위치와 코스 사이의 최소 거리(m)", example = "240")
        Integer distanceFromUserM,

        @Schema(description = "코스 난이도", implementation = DifficultyOption.class, nullable = true)
        DifficultyOption difficulty,

        @ArraySchema(
                schema = @Schema(implementation = CourseTypeOption.class),
                arraySchema = @Schema(description = "코스 유형 목록")
        )
        List<CourseTypeOption> courseTypes,

        @Schema(description = "하이브리드 추천 코스 여부", example = "true")
        boolean isRecommended,

        @Schema(description = "코스 좋아요 수", example = "128")
        Long likeCount,

        @Schema(description = "코스 북마크 수", example = "42")
        Long bookmarkCount,

        @Schema(description = "현재 사용자의 좋아요 여부", example = "false")
        boolean isLiked,

        @Schema(description = "현재 사용자의 북마크 여부", example = "false")
        boolean isBookmarked,

        @Schema(description = "코스 대표 이미지 URL", example = "https://cdn.withrun.kr/courses/12.png", nullable = true)
        String snapshotImageUrl
) {
    public NearbyCourseItemResponse(
            Long courseId,
            String title,
            CourseStatus status,
            RouteType routeType,
            Integer distanceM,
            Integer elevationGainM,
            Double startLatitude,
            Double startLongitude,
            Double endLatitude,
            Double endLongitude,
            Integer distanceFromTargetM,
            Integer distanceFromUserM,
            DifficultyOption difficulty,
            List<CourseTypeOption> courseTypes,
            String snapshotImageUrl
    ) {
        this(courseId, title, status, routeType, distanceM, elevationGainM, startLatitude, startLongitude, endLatitude, endLongitude,
                distanceFromTargetM, distanceFromUserM, difficulty, courseTypes, false, 0L, 0L, false, false, snapshotImageUrl);
    }

    @Schema(name = "NearbyCourseDifficultyOption", description = "주변 코스 응답의 난이도 항목")
    public record DifficultyOption(
            @Schema(description = "클라이언트가 사용하는 데이터 값", example = "MEDIUM")
            String data,
            @Schema(description = "사용자에게 노출하는 라벨", example = "보통")
            String label
    ) {
        public static DifficultyOption from(Difficulty difficulty) {
            if (difficulty == null) {
                return null;
            }
            return new DifficultyOption(difficulty.getData(), difficulty.getLabel());
        }
    }

    @Schema(name = "NearbyCourseTypeOption", description = "주변 코스 응답의 코스 유형 항목")
    public record CourseTypeOption(
            @Schema(description = "클라이언트가 사용하는 데이터 값", example = "RIVERSIDE")
            String data,
            @Schema(description = "사용자에게 노출하는 라벨", example = "강변")
            String label
    ) {
        public static CourseTypeOption from(CourseType courseType) {
            return new CourseTypeOption(courseType.getData(), courseType.getLabel());
        }
    }
}
