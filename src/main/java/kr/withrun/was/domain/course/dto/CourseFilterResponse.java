package kr.withrun.was.domain.course.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import kr.withrun.was.domain.course.type.CourseDistanceType;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.global.common.type.Difficulty;

import java.util.List;

@Schema(description = "코스 필터 설문 항목 응답")
public record CourseFilterResponse(

        @ArraySchema(schema = @Schema(implementation = CourseDistanceTypeOption.class), arraySchema = @Schema(description = "코스 거리 항목"))
        List<CourseDistanceTypeOption> courseDistanceTypes,

        @ArraySchema(schema = @Schema(implementation = CourseTypeOption.class), arraySchema = @Schema(description = "코스 유형 설문 항목"))
        List<CourseTypeOption> courseTypes,

        @ArraySchema(schema = @Schema(implementation = DifficultyOption.class), arraySchema = @Schema(description = "난이도 설문 항목"))
        List<DifficultyOption> difficulties
) {
    @Schema(name = "CourseFilterDistanceTypeOption", description = "코스 필터용 거리 항목")
    public record CourseDistanceTypeOption(
            @Schema(description = "클라이언트가 사용하는 데이터 값", example = "1")
            String data,
            @Schema(description = "사용자에게 노출하는 라벨", example = "1km")
            String label
    ) {
        public static CourseDistanceTypeOption from(CourseDistanceType courseDistanceType) {
            return new CourseDistanceTypeOption(courseDistanceType.getData(), courseDistanceType.getLabel());
        }
    }

    @Schema(name = "CourseFilterCourseTypeOption", description = "코스 필터용 코스 유형 항목")
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

    @Schema(name = "CourseFilterDifficultyOption", description = "코스 필터용 난이도 항목")
    public record DifficultyOption(
            @Schema(description = "클라이언트가 사용하는 데이터 값", example = "EASY")
            String data,
            @Schema(description = "사용자에게 노출하는 라벨", example = "쉬움")
            String label
    ) {
        public static DifficultyOption from(Difficulty difficulty) {
            return new DifficultyOption(difficulty.getData(), difficulty.getLabel());
        }
    }
}


