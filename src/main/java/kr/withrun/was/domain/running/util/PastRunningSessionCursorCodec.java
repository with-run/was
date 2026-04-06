package kr.withrun.was.domain.running.util;

import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;

public final class PastRunningSessionCursorCodec {

    private static final String DELIMITER = "|";

    private PastRunningSessionCursorCodec() {
    }

    public static String encode(LocalDateTime startedAt, long runningSessionId) {
        String payload = startedAt + DELIMITER + runningSessionId;
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

            LocalDateTime startedAt = LocalDateTime.parse(parts[0]);
            long runningSessionId = Long.parseLong(parts[1]);
            return new CursorPayload(startedAt, runningSessionId);
        } catch (RuntimeException exception) {
            throw invalidCursor();
        }
    }

    private static CustomException invalidCursor() {
        return new CustomException(ResponseCode.INVALID_CURSOR);
    }

    public record CursorPayload(LocalDateTime startedAt, long runningSessionId) {
    }
}
