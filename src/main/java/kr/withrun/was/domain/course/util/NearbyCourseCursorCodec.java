package kr.withrun.was.domain.course.util;

import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class NearbyCourseCursorCodec {

    private static final String DELIMITER = ":";
    private static final long SCORE_SCALE_FACTOR = 1_000_000L;

    private NearbyCourseCursorCodec() {
    }

    public static String encode(double lastFinalScore, int lastDistanceFromUserM, long lastCourseId) {
        long scaledScore = scaleScore(lastFinalScore);
        String payload = scaledScore + DELIMITER + lastDistanceFromUserM + DELIMITER + lastCourseId;
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

            long lastScaledScore = Long.parseLong(parts[0]);
            int lastDistanceFromUserM = Integer.parseInt(parts[1]);
            long lastCourseId = Long.parseLong(parts[2]);
            return new CursorPayload(lastScaledScore, lastDistanceFromUserM, lastCourseId);
        } catch (IllegalArgumentException exception) {
            throw invalidCursor();
        }
    }

    public static long scaleScore(double score) {
        if (Double.isNaN(score) || Double.isInfinite(score)) {
            return 0L;
        }
        return Math.round(score * SCORE_SCALE_FACTOR);
    }

    private static CustomException invalidCursor() {
        return new CustomException(ResponseCode.INVALID_CURSOR);
    }

    public record CursorPayload(long lastScaledScore, int lastDistanceFromUserM, long lastCourseId) {
    }
}
