package com.jj.moca.api.auth.jwt;

import com.jj.moca.api.auth.jwt.response.JwtInformation;

import java.util.UUID;

public interface JwtRegistry {

    void registerJwtInformation(JwtInformation jwtInformation);

    void invalidateJwtInformationByUserId(UUID userId);

    boolean hasActiveJwtInformationByAccessToken(String accessToken);

    boolean hasActiveJwtInformationByRefreshToken(String refreshToken);

    JwtInformation getJwtInformationByRefreshToken(String refreshToken);

    boolean rotateJwtInformation(String refreshToken, JwtInformation newJwtInformation);

    void rollbackRotateJwtInformation(
            String oldRefreshToken,
            JwtInformation oldJwtInformation,
            String newRefreshToken
    );

    void clearExpiredJwtInformation();
}
