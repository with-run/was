package kr.withrun.was.domain.course.type;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("코스 공개 상태 enum")
class CourseStatusTest {

    @DisplayName("각 상태는 외부 노출용 data 와 label 을 가진다")
    @Test
    void exposesDataAndLabelForEachStatus() {
        assertThat(CourseStatus.OFFICIAL)
                .extracting(CourseStatus::getData, CourseStatus::getLabel)
                .containsExactly("OFFICIAL", "공식 코스");

        assertThat(CourseStatus.COMMUNITY)
                .extracting(CourseStatus::getData, CourseStatus::getLabel)
                .containsExactly("COMMUNITY", "커뮤니티 코스");

        assertThat(CourseStatus.PRIVATE)
                .extracting(CourseStatus::getData, CourseStatus::getLabel)
                .containsExactly("PRIVATE", "개인 코스");
    }
}
