package kr.withrun.was.domain.course.util;

import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;

public final class BookmarkedCourseCursorCodec {

    private static final String DELIMITER = "|";

    private BookmarkedCourseCursorCodec() {
    }

    public static String encode(LocalDateTime bookmarkedAt, long bookmarkId) {
        String payload = bookmarkedAt + DELIMITER + bookmarkId;
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    public static CursorPayload decode(String cursor) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = decoded.split("\\|", -1);
            if (parts.length != 2) {
                throw invalidCursor();
            }

            LocalDateTime bookmarkedAt = LocalDateTime.parse(parts[0]);
            long bookmarkId = Long.parseLong(parts[1]);
            return new CursorPayload(bookmarkedAt, bookmarkId);
        } catch (RuntimeException exception) {
            throw invalidCursor();
        }
    }

    private static CustomException invalidCursor() {
        return new CustomException(ResponseCode.INVALID_CURSOR);
    }

    public record CursorPayload(LocalDateTime bookmarkedAt, long bookmarkId) {
    }
}
