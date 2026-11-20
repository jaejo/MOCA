package com.jj.moca.api.auth.service;

import com.jj.moca.api.auth.details.MoCaUserDetails;
import com.jj.moca.api.auth.jwt.JwtRegistry;
import com.jj.moca.api.auth.jwt.JwtTokenProvider;
import com.jj.moca.api.auth.jwt.response.JwtDto;
import com.jj.moca.api.auth.jwt.response.JwtInformation;
import com.jj.moca.core.common.exception.BusinessException;
import com.jj.moca.core.common.exception.GlobalErrorCode;
import com.jj.moca.core.domain.user.dto.response.UserResponse;
import com.jj.moca.core.domain.user.entity.User;
import com.jj.moca.core.domain.user.exception.UserErrorCode;
import com.jj.moca.core.domain.user.repository.UserRepository;
import com.nimbusds.jose.JOSEException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtRegistry jwtRegistry;

    @Override
    @Transactional
    public JwtDto refresh(String refreshToken) {
        if (refreshToken == null || !jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다.");
        }

        JwtInformation current = jwtRegistry.getJwtInformationByRefreshToken(refreshToken);
        if (current == null) {
            UUID userId = jwtTokenProvider.getUserIdFromTokenWithoutExpiration(refreshToken, "refresh");
            jwtRegistry.invalidateJwtInformationByUserId(userId);
            log.warn("[AuthService] 폐기된 리프레시 토큰 재사용 감지 - 전체 세션 무효화. userId={}", userId);
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED, "만료되었거나 활성화되지 않은 토큰 세션입니다.");
        }

        User user = userRepository.findById(current.userId())
                .orElseThrow(() -> new BusinessException(UserErrorCode.NOT_FOUND));

        UserResponse userResponse = UserResponse.from(user);
        MoCaUserDetails userDetails = new MoCaUserDetails(userResponse, null);

        try {
            String newAccessToken = jwtTokenProvider.generateAccessToken(userDetails);
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

            Instant refreshTokenExpiresAt = jwtTokenProvider.getExpiration(newRefreshToken).toInstant();
            JwtInformation next = new JwtInformation(
                    user.getId(),
                    newAccessToken,
                    newRefreshToken,
                    refreshTokenExpiresAt
            );

            boolean rotated = jwtRegistry.rotateJwtInformation(refreshToken, next);
            if (!rotated) {
                log.warn("[AuthService] 토큰 로테이션 실패(동시 요청 또는 이미 소비된 토큰). userId={}", user.getId());
                throw new BusinessException(GlobalErrorCode.UNAUTHORIZED, "이미 만료되었거나 갱신된 리프레시 토큰입니다.");
            }

            return JwtDto.builder()
                    .user(userResponse)
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .build();
        } catch (JOSEException e) {
            log.error("[AuthService] 토큰 생성 중 암호화 예외 발생", e);
            throw new BusinessException(GlobalErrorCode.INTERNAL_SERVER_ERROR, "토큰 갱신 중 서버 오류가 발생했습니다.");
        }
    }
}

