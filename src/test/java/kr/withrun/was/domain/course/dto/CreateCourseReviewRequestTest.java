package kr.withrun.was.domain.course.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("코스 리뷰 생성 요청 DTO")
class CreateCourseReviewRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @DisplayName("리뷰 생성 요청은 현재 사용자 userId를 노출하지 않는다")
    @Test
    void doesNotExposeCurrentUserIdComponent() {
        RecordComponent[] components = CreateCourseReviewRequest.class.getRecordComponents();

        assertThat(components)
                .extracting(RecordComponent::getName)
                .containsExactly("rating", "courseTypes", "submittedDifficulty");
    }

    @DisplayName("유효한 리뷰 생성 요청은 검증을 통과한다")
    @Test
    void acceptsValidRequest() {
        CreateCourseReviewRequest request = new CreateCourseReviewRequest(5, List.of("RIVERSIDE", "URBAN"), "MEDIUM");

        assertThat(validator.validate(request)).isEmpty();
    }
}
