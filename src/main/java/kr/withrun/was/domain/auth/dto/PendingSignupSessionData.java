package kr.withrun.was.domain.auth.dto;

import java.io.Serializable;

public record PendingSignupSessionData(
        String provider,
        String providerUserId
) implements Serializable {
}

