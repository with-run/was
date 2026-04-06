package kr.withrun.was.domain.course.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleManeuverSampleAction;

@Schema(description = "코스 navigation 메타 항목 응답")
public record CourseNavigationMetaResponse(
        @ArraySchema(schema = @Schema(implementation = NavigationOption.class), arraySchema = @Schema(description = "navigation maneuver 선택 항목"))
        List<NavigationOption> navigations
) {

    @Schema(name = "CourseNavigationMetaOption", description = "코스 navigation 메타용 maneuver 항목")
    public record NavigationOption(
            @Schema(description = "클라이언트가 사용하는 navigation 데이터 값", example = "SLIGHT_RIGHT")
            String data,
            @Schema(description = "사용자에게 노출하는 라벨", example = "오른쪽으로 살짝 이동")
            String label
    ) {
        public static NavigationOption from(NavigationBundleManeuverSampleAction sampleAction) {
            return new NavigationOption(sampleAction.getData(), sampleAction.getLabel());
        }
    }
}
