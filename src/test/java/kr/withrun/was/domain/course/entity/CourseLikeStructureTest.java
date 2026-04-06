package kr.withrun.was.domain.course.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("코스 좋아요 엔티티 구조")
class CourseLikeStructureTest {

    @DisplayName("BaseEntity를 상속하지 않는다")
    @Test
    void doesNotExtendBaseEntity() {
        assertThat(CourseLike.class.getSuperclass()).isEqualTo(Object.class);
    }
}
