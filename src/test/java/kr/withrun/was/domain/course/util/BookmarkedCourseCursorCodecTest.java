package kr.withrun.was.domain.course.util;

import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("북마크 코스 커서 코덱")
class BookmarkedCourseCursorCodecTest {

    @DisplayName("북마크 시각과 북마크 ID를 커서로 인코딩하고 복원한다")
    @Test
    void encodesAndDecodesBookmarkedAtAndBookmarkId() {
        LocalDateTime bookmarkedAt = LocalDateTime.of(2026, 3, 14, 10, 30, 15);

        String cursor = BookmarkedCourseCursorCodec.encode(bookmarkedAt, 42L);
        BookmarkedCourseCursorCodec.CursorPayload payload = BookmarkedCourseCursorCodec.decode(cursor);

        assertThat(payload.bookmarkedAt()).isEqualTo(bookmarkedAt);
        assertThat(payload.bookmarkId()).isEqualTo(42L);
    }

    @DisplayName("형식이 잘못된 커서는 INVALID_CURSOR 예외를 던진다")
    @Test
    void rejectsMalformedCursor() {
        assertThatThrownBy(() -> BookmarkedCourseCursorCodec.decode("%%%"))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.INVALID_CURSOR);
    }
}
