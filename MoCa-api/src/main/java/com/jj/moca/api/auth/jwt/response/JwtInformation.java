package com.jj.moca.api.auth.jwt.response;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record JwtInformation(
        UUID userId,
        String accessToken,
        String refreshToken,
        Instant refreshTokenExpiresAt
) {
    /*
     * 전달받은 userId 및 refreshTokenExpiresAt이 null인지 검증한다.
     * null인 경우 NullPointerException 예외를 발생시킨다.
     */
    public JwtInformation {
        Objects.requireNonNull(userId, "유저 ID는 null이 될 수 없습니다.");
        Objects.requireNonNull(refreshTokenExpiresAt, "리프레시 토큰 만료 시각은 null이 될 수 없습니다.");
    }

    public boolean isExpired(Instant now) {
        return !refreshTokenExpiresAt.isAfter(now);
    }
}
