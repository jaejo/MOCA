package com.jj.moca.api.auth.controller;

import com.jj.moca.api.auth.jwt.JwtTokenProvider;
import com.jj.moca.api.auth.jwt.response.JwtDto;
import com.jj.moca.api.auth.service.AuthService;
import com.jj.moca.core.common.exception.BusinessException;
import com.jj.moca.core.common.exception.GlobalErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * CSRF 토큰을 조회한다.
     *
     * @param csrfToken Spring Security가 주입된 CSRF 토큰 객체
     * @return 응답 본문이 없는 ResponseEntity (HTTP 204 No Content)
     */
    @GetMapping("/csrf-token")
    public ResponseEntity<Void> getCsrfToken(CsrfToken csrfToken) {
        csrfToken.getToken();
        return ResponseEntity.noContent().build();
    }

    /**
     * Refresh Token을 사용하여 Access Token을 재발급한다.
     *
     * @param refreshToken 쿠키에서 추출한 Refresh Token
     * @param response     새로운 Refresh Token 쿠키를 주입할 HTTP 응답 객체
     * @return 갱신된 JWT 토큰 DTO
     */
    @PostMapping("/refresh")
    public ResponseEntity<JwtDto> refresh(
            @CookieValue(
                    name = JwtTokenProvider.REFRESH_TOKEN_COOKIE_NAME,
                    required = false
            )
            String refreshToken,
            HttpServletResponse response
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED, "리프레시 토큰이 누락되었습니다.");
        }

        try {
            JwtDto jwtDto = authService.refresh(refreshToken);
            jwtTokenProvider.addRefreshCookie(response, jwtDto.getRefreshToken());
            return ResponseEntity.ok(jwtDto);
        } catch (BusinessException e) {
            jwtTokenProvider.expireRefreshCookie(response);
            throw e;
        }
    }
}
