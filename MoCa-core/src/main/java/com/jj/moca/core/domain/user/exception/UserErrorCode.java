package com.jj.moca.core.domain.user.exception;

import com.jj.moca.core.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    NOT_FOUND(1001, "NOT_FOUND", HttpStatus.NOT_FOUND, "존재하지 않는 유저입니다."),
    EMAIL_DUPLICATED(1002, "EMAIL_DUPLICATED", HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다.");

    private final int numeric;
    private final String errorKey;
    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String getDomain() {
        return "USER";
    }

    @Override
    public String getCode() {
        return getDomain() + "-" + getErrorKey();
    }
}
