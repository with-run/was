package kr.withrun.was.domain.course.util;

import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("코스 고스트 리더보드 커서 코덱")
class CourseGhostLeaderboardCursorCodecTest {

    @DisplayName("점수와 리더보드 ID를 커서로 인코딩하고 복원한다")
    @Test
    void encodesAndDecodesPointAndLeaderboardId() {
        String cursor = CourseGhostLeaderboardCursorCodec.encode(1250, 101L);

        CourseGhostLeaderboardCursorCodec.CursorPayload payload = CourseGhostLeaderboardCursorCodec.decode(cursor);

        assertThat(payload.point()).isEqualTo(1250);
        assertThat(payload.leaderboardId()).isEqualTo(101L);
    }

    @DisplayName("형식이 잘못된 커서는 INVALID_CURSOR 예외를 던진다")
    @Test
    void rejectsMalformedCursor() {
        assertThatThrownBy(() -> CourseGhostLeaderboardCursorCodec.decode("%%%"))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.INVALID_CURSOR);
    }
}
