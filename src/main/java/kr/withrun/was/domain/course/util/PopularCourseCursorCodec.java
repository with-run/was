package kr.withrun.was.domain.course.util;

import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class PopularCourseCursorCodec {

    private static final String DELIMITER = ":";

    private PopularCourseCursorCodec() {
    }

    public static String encode(long lastPopularityScore, int lastDistanceFromTargetM, long lastCourseId) {
        String payload = lastPopularityScore + DELIMITER + lastDistanceFromTargetM + DELIMITER + lastCourseId;
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    public static CursorPayload decode(String cursor) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = decoded.split(DELIMITER, -1);
            if (parts.length != 3) {
                throw invalidCursor();
            }

            long lastPopularityScore = Long.parseLong(parts[0]);
            int lastDistanceFromTargetM = Integer.parseInt(parts[1]);
            long lastCourseId = Long.parseLong(parts[2]);
            return new CursorPayload(lastPopularityScore, lastDistanceFromTargetM, lastCourseId);
        } catch (IllegalArgumentException exception) {
            throw invalidCursor();
        }
    }

    private static CustomException invalidCursor() {
        return new CustomException(ResponseCode.INVALID_CURSOR);
    }

    public record CursorPayload(long lastPopularityScore, int lastDistanceFromTargetM, long lastCourseId) {
    }
}
