package com.jj.moca.api.common.exception;

import com.jj.moca.core.common.exception.BusinessException;
import com.jj.moca.core.common.exception.ErrorCode;
import com.jj.moca.core.common.exception.ErrorResponse;
import com.jj.moca.core.common.exception.GlobalErrorCode;
import com.jj.moca.core.domain.user.exception.UserErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String INCORRECT_PARAMETER = "'%s' 파라미터 값이 올바르지 않습니다. (허용된 값: %s)";
    private static final String INVALID_PARAMETER_FORMAT = "'%s' 파라미터의 형식이 올바르지 않습니다.";
    private static final String INVALID_REQUEST = "잘못된 요청입니다.";
    private static final String MISSING_PARAMETER = "'%s' 필수 파라미터가 누락되었습니다.";
    private static final Set<String> REQUIRED_CONSTRAINT_CODES = Set.of("NotBlank", "NotNull", "NotEmpty");

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e, HttpServletRequest request) {
        ErrorCode code = e.getErrorCode();
        if (code.getHttpStatus().is5xxServerError()) {
            log.error("비즈니스 예외 발생: code={}, message={}, path={}",
                    code.getCode(), e.getMessage(), request.getRequestURI(), e);
        } else {
            log.warn("비즈니스 예외 발생: code={}, message={}, path={}",
                    code.getCode(), e.getMessage(), request.getRequestURI());
        }

        ErrorResponse response = e.getHint() != null
                ? ErrorResponse.of(code, e.getMessage(), request.getRequestURI(), e.getHint())
                : ErrorResponse.of(code, e.getMessage(), request.getRequestURI());

        return ResponseEntity.status(code.getHttpStatus()).body(response);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException e,
            HttpServletRequest request
    ) {
        String message = e.getMostSpecificCause().getMessage();
        String normalizedMessage = message != null ? message.toLowerCase(Locale.ROOT) : "";

        if (normalizedMessage.contains("users_email_key")) {
            ErrorCode code = UserErrorCode.EMAIL_DUPLICATED;
            log.warn("이메일 중복 제약 위반: path={}", request.getRequestURI());
            return ResponseEntity.status(code.getHttpStatus())
                    .body(ErrorResponse.of(code, code.getMessage(), request.getRequestURI()));
        }

        ErrorCode code = GlobalErrorCode.INTERNAL_SERVER_ERROR;
        log.error("데이터 무결성 예외 발생: message={}, path={}", e.getMessage(), request.getRequestURI(), e);
        return ResponseEntity.status(code.getHttpStatus())
                .body(ErrorResponse.of(code, code.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException e,
            HttpServletRequest request
    ) {
        ErrorCode code = GlobalErrorCode.INVALID_INPUT;

        // 동일 필드에 여러 검증 오류가 동시에 발생한 경우(예: 빈 문자열에서 @NotBlank + @Pattern),
        // 필수 입력(@NotBlank/@NotNull/@NotEmpty) 오류를 우선하여 필드당 1건만 반환
        Map<String, FieldError> prioritizedErrors = new LinkedHashMap<>();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            prioritizedErrors.merge(
                    error.getField(),
                    error,
                    (existing, incoming) -> isRequiredConstraint(incoming) && !isRequiredConstraint(existing)
                            ? incoming
                            : existing
            );
        }

        List<ErrorResponse.Detail> details = prioritizedErrors.values().stream()
                .map(error -> new ErrorResponse.Detail(
                        error.getField(),
                        error.getDefaultMessage(),
                        sanitizeRejectedValue(error.getField(), error.getRejectedValue())
                ))
                .toList();

        log.warn("유효성 검사 실패: code={}, path={}, details={}",
                code.getCode(), request.getRequestURI(), details);

        ErrorResponse response = ErrorResponse.of(code, code.getMessage(), request.getRequestURI(), details);
        return ResponseEntity.status(code.getHttpStatus()).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException e,
            HttpServletRequest request
    ) {
        ErrorCode code = GlobalErrorCode.INVALID_INPUT;
        log.warn("요청 본문 파싱 실패: code={}, path={}, message={}",
                code.getCode(), request.getRequestURI(), e.getMessage());

        ErrorResponse response = ErrorResponse.of(code, "요청 본문(JSON) 형식이 올바르지 않습니다.", request.getRequestURI());
        return ResponseEntity.status(code.getHttpStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException e,
            HttpServletRequest request
    ) {
        ErrorCode code = GlobalErrorCode.INVALID_INPUT;

        String detailsMessage;
        if (e.getRequiredType() != null && e.getRequiredType().isEnum()) {
            String allowedValues = Arrays.toString(e.getRequiredType().getEnumConstants());
            detailsMessage = String.format(INCORRECT_PARAMETER, e.getName(), allowedValues);
        } else {
            detailsMessage = String.format(INVALID_PARAMETER_FORMAT, e.getName());
        }

        List<ErrorResponse.Detail> details = List.of(
                new ErrorResponse.Detail(e.getName(), detailsMessage, sanitizeRejectedValue(e.getName(), e.getValue()))
        );
        log.warn("파라미터 타입 불일치: code={}, path={}, details={}",
                code.getCode(), request.getRequestURI(), details);

        ErrorResponse response = ErrorResponse.of(code, INVALID_REQUEST, request.getRequestURI(), details);
        return ResponseEntity.status(code.getHttpStatus()).body(response);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException e,
            HttpServletRequest request
    ) {
        ErrorCode code = GlobalErrorCode.INVALID_INPUT;
        String detailsMessage = String.format(MISSING_PARAMETER, e.getParameterName());

        List<ErrorResponse.Detail> details = List.of(
                new ErrorResponse.Detail(e.getParameterName(), detailsMessage, null)
        );

        log.warn("필수 파라미터 누락: code={}, path={}, details={}",
                code.getCode(), request.getRequestURI(), details);

        ErrorResponse response = ErrorResponse.of(code, INVALID_REQUEST, request.getRequestURI(), details);
        return ResponseEntity.status(code.getHttpStatus()).body(response);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestHeader(
            MissingRequestHeaderException e,
            HttpServletRequest request
    ) {
        ErrorCode code = GlobalErrorCode.INVALID_INPUT;
        String headerName = e.getHeaderName();
        String errorMessage = String.format("필수 헤더 '%s'가 누락되었습니다.", headerName);

        log.warn("필수 헤더 누락: code={}, message={}, path={}",
                code.getCode(), errorMessage, request.getRequestURI());

        List<ErrorResponse.Detail> details = List.of(
                new ErrorResponse.Detail(headerName, errorMessage, null)
        );

        ErrorResponse response = ErrorResponse.of(code, INVALID_REQUEST, request.getRequestURI(), details);
        return ResponseEntity.status(code.getHttpStatus()).body(response);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException e,
            HttpServletRequest request
    ) {
        ErrorCode code = GlobalErrorCode.METHOD_NOT_ALLOWED;
        log.warn("지원하지 않는 HTTP 메서드: method={}, path={}", e.getMethod(), request.getRequestURI());

        ErrorResponse response = ErrorResponse.of(code, code.getMessage(), request.getRequestURI());
        return ResponseEntity.status(code.getHttpStatus()).body(response);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(
            NoResourceFoundException e,
            HttpServletRequest request
    ) {
        ErrorCode code = GlobalErrorCode.NOT_FOUND;
        log.warn("리소스를 찾을 수 없음: path={}", request.getRequestURI());

        ErrorResponse response = ErrorResponse.of(code, code.getMessage(), request.getRequestURI());
        return ResponseEntity.status(code.getHttpStatus()).body(response);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException e,
            HttpServletRequest request
    ) {
        ErrorCode code = GlobalErrorCode.UNAUTHORIZED;
        log.warn("인증 예외 발생: code={}, message={}, path={}",
                code.getCode(), e.getMessage(), request.getRequestURI());

        ErrorResponse response = ErrorResponse.of(code, code.getMessage(), request.getRequestURI());
        return ResponseEntity.status(code.getHttpStatus()).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException e,
            HttpServletRequest request
    ) {
        ErrorCode code = GlobalErrorCode.FORBIDDEN;
        log.warn("인가(권한) 예외 발생: code={}, message={}, path={}",
                code.getCode(), e.getMessage(), request.getRequestURI());

        ErrorResponse response = ErrorResponse.of(code, code.getMessage(), request.getRequestURI());
        return ResponseEntity.status(code.getHttpStatus()).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e, HttpServletRequest request) {
        ErrorCode code = GlobalErrorCode.INTERNAL_SERVER_ERROR;
        log.error("서버 내부 오류 발생: code={}, message={}, path={}",
                code.getCode(), e.getMessage(), request.getRequestURI(), e);

        ErrorResponse response = ErrorResponse.of(code, code.getMessage(), request.getRequestURI());
        return ResponseEntity.status(code.getHttpStatus()).body(response);
    }

    private boolean isRequiredConstraint(FieldError error) {
        return error.getCode() != null && REQUIRED_CONSTRAINT_CODES.contains(error.getCode());
    }

    private Object sanitizeRejectedValue(String field, Object rejectedValue) {
        if (rejectedValue == null || field == null) {
            return rejectedValue;
        }
        String lowerField = field.toLowerCase(Locale.ROOT);
        if (lowerField.contains("password") || lowerField.contains("token") || lowerField.contains("secret")) {
            return "***";
        }
        return rejectedValue;
    }
}
