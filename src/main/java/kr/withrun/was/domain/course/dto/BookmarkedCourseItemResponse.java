package kr.withrun.was.domain.course.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.domain.course.type.RouteType;
import kr.withrun.was.global.common.type.Difficulty;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "북마크한 코스 단건 정보. 좋아요 메타데이터를 포함한다.")
public record BookmarkedCourseItemResponse(
        @Schema(description = "북마크 ID", example = "42")
        Long bookmarkId,

        @Schema(description = "북마크 생성 시각", example = "2026-03-17T10:15:30")
        LocalDateTime bookmarkedAt,

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

        @Schema(description = "코스 난이도", implementation = DifficultyOption.class, nullable = true)
        DifficultyOption difficulty,

        @ArraySchema(schema = @Schema(implementation = CourseTypeOption.class), arraySchema = @Schema(description = "코스 유형 목록"))
        List<CourseTypeOption> courseTypes,

        @Schema(description = "코스 대표 이미지 URL", example = "https://cdn.withrun.kr/courses/12.png", nullable = true)
        String snapshotImageUrl,

        @Schema(description = "누적 좋아요 수", example = "23")
        Long likeCount,

        @Schema(description = "현재 사용자의 좋아요 여부", example = "true")
        boolean isLiked,

        @Schema(description = "북마크 목록 응답에서는 항상 true 다.", example = "true")
        boolean isBookmarked
) {
    @Schema(name = "BookmarkedCourseDifficultyOption", description = "북마크 코스 응답의 난이도 항목")
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

    @Schema(name = "BookmarkedCourseTypeOption", description = "북마크 코스 응답의 코스 유형 항목")
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
