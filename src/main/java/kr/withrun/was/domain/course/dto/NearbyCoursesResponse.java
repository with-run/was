package kr.withrun.was.domain.course.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "주변 코스 조회 응답")
public record NearbyCoursesResponse(
        @ArraySchema(
                schema = @Schema(implementation = NearbyCourseItemResponse.class),
                arraySchema = @Schema(description = "현재 페이지 코스 목록")
        )
        List<NearbyCourseItemResponse> items,

        @Schema(description = "현재 페이지 번호", example = "0")
        int page,

        @Schema(description = "요청한 페이지 크기", example = "3")
        int size,

        @Schema(description = "조건에 맞는 전체 항목 수", example = "12")
        long totalElements,

        @Schema(description = "전체 페이지 수", example = "4")
        int totalPages,

        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext
) {
}
