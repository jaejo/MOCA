package com.jj.moca.core.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum GlobalErrorCode implements ErrorCode {

    INVALID_INPUT(9001, "INVALID_INPUT", HttpStatus.BAD_REQUEST, "입력값 검증에 실패했습니다."),
    METHOD_NOT_ALLOWED(9002, "METHOD_NOT_ALLOWED", HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
    NOT_FOUND(9003, "NOT_FOUND", HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    UNAUTHORIZED(9004, "UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "인증 정보가 필요하거나 유효하지 않습니다."),
    FORBIDDEN(9005, "FORBIDDEN", HttpStatus.FORBIDDEN, "해당 리소스에 접근할 권한이 없습니다."),
    CDN_URL_NOT_FOUND(9006, "CDN_URL_NOT_FOUND", HttpStatus.NOT_FOUND, "CDN 경로가 없습니다."),

    INTERNAL_SERVER_ERROR(9999, "SERVER_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final int numeric;
    private final String errorKey;
    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String getDomain() {
        return "GLOBAL";
    }

    @Override
    public String getCode() {
        return getDomain() + "-" + getErrorKey();
    }
}