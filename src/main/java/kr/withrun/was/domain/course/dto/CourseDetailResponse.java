package kr.withrun.was.domain.course.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.domain.course.type.RouteType;
import kr.withrun.was.domain.course.vo.Coordinates;
import kr.withrun.was.global.common.type.Difficulty;

import java.util.List;

@Schema(description = "코스 상세 정보")
public record CourseDetailResponse(
        @Schema(description = "코스 ID", example = "12")
        Long courseId,

        @Schema(description = "코스 제목", example = "한강 야간 러닝 5K")
        String title,

        @Schema(description = "코스 상태")
        CourseStatus status,

        @Schema(description = "경로 유형", nullable = true)
        RouteType routeType,

        @Schema(description = "코스 난이도", implementation = DifficultyOption.class, nullable = true)
        DifficultyOption difficulty,

        @Schema(description = "코스 총 거리(m)", example = "5200")
        Integer distanceM,

        @Schema(description = "코스 총 누적 상승고도(m)", example = "48")
        Integer elevationGainM,

        @Schema(description = "코스 대표 이미지 URL", example = "https://cdn.withrun.kr/courses/12.png", nullable = true)
        String snapshotImageUrl,

        @Schema(description = "시작 지점 위도", example = "37.5661")
        Double startLatitude,

        @Schema(description = "시작 지점 경도", example = "126.9738")
        Double startLongitude,

        @Schema(description = "종료 지점 위도", example = "37.5702")
        Double endLatitude,

        @Schema(description = "종료 지점 경도", example = "126.9814")
        Double endLongitude,

        @Schema(description = "코스 전체 경로 좌표")
        Coordinates coordinates,

        @ArraySchema(schema = @Schema(implementation = CourseTypeOption.class), arraySchema = @Schema(description = "코스 유형 목록"))
        List<CourseTypeOption> courseTypes,

        @Schema(description = "누적 좋아요 수", example = "128")
        Long likeCount,

        @Schema(description = "현재 사용자의 좋아요 여부", example = "false")
        boolean isLiked,

        @Schema(description = "현재 사용자의 북마크 여부", example = "false")
        boolean isBookmarked,

        @Schema(description = "평균 리뷰 평점", example = "4.3", nullable = true)
        Double averageRating
) {
    @Schema(name = "CourseDetailDifficultyOption", description = "코스 상세 응답의 난이도 항목")
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

    @Schema(name = "CourseDetailCourseTypeOption", description = "코스 상세 응답의 코스 유형 항목")
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
