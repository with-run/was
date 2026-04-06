package kr.withrun.was.domain.course.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.domain.course.type.RouteType;
import kr.withrun.was.global.common.type.Difficulty;

@Schema(description = "코스 등록 메타 항목 응답")
public record CourseRegisterMetaResponse(
        @ArraySchema(schema = @Schema(implementation = CourseTypeOption.class), arraySchema = @Schema(description = "코스 유형 선택 항목"))
        List<CourseTypeOption> courseTypes,

        @ArraySchema(schema = @Schema(implementation = ModeOption.class), arraySchema = @Schema(description = "코스 공개 범위 선택 항목"))
        List<ModeOption> mode,

        @ArraySchema(schema = @Schema(implementation = RouteTypeOption.class), arraySchema = @Schema(description = "경로 형태 선택 항목"))
        List<RouteTypeOption> routeTypes,

        @ArraySchema(schema = @Schema(implementation = DifficultyOption.class), arraySchema = @Schema(description = "난이도 선택 항목"))
        List<DifficultyOption> difficulties
) {
    @Schema(name = "CourseRegisterMetaCourseTypeOption", description = "코스 등록 메타용 코스 유형 항목")
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

    @Schema(name = "CourseRegisterMetaModeOption", description = "코스 등록 메타용 공개 범위 항목")
    public record ModeOption(
            @Schema(description = "클라이언트가 사용하는 데이터 값", example = "COMMUNITY")
            String data,
            @Schema(description = "사용자에게 노출하는 라벨", example = "커뮤니티 코스")
            String label
    ) {
        public static ModeOption from(CourseStatus courseStatus) {
            return new ModeOption(courseStatus.getData(), courseStatus.getLabel());
        }
    }

    @Schema(name = "CourseRegisterMetaRouteTypeOption", description = "코스 등록 메타용 경로 형태 항목")
    public record RouteTypeOption(
            @Schema(description = "클라이언트가 사용하는 데이터 값", example = "LOOP")
            String data,
            @Schema(description = "사용자에게 노출하는 라벨", example = "순환형")
            String label
    ) {
        public static RouteTypeOption from(RouteType routeType) {
            return new RouteTypeOption(routeType.getData(), routeType.getLabel());
        }
    }

    @Schema(name = "CourseRegisterMetaDifficultyOption", description = "코스 등록 메타용 난이도 항목")
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
