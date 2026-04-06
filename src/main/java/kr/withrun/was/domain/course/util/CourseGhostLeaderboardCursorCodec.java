package kr.withrun.was.domain.course.util;

import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class CourseGhostLeaderboardCursorCodec {

    private static final String DELIMITER = "|";

    private CourseGhostLeaderboardCursorCodec() {
    }

    public static String encode(int point, long leaderboardId) {
        String payload = point + DELIMITER + leaderboardId;
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

            int point = Integer.parseInt(parts[0]);
            long leaderboardId = Long.parseLong(parts[1]);
            return new CursorPayload(point, leaderboardId);
        } catch (RuntimeException exception) {
            throw invalidCursor();
        }
    }

    private static CustomException invalidCursor() {
        return new CustomException(ResponseCode.INVALID_CURSOR);
    }

    public record CursorPayload(int point, long leaderboardId) {
    }
}
