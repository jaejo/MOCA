package com.jj.moca.core.domain.user.service;

import com.jj.moca.core.common.exception.BusinessException;
import com.jj.moca.core.common.security.PasswordEncryptor;
import com.jj.moca.core.common.util.EmailFormatter;
import com.jj.moca.core.domain.user.dto.request.UserCreateRequest;
import com.jj.moca.core.domain.user.dto.response.UserResponse;
import com.jj.moca.core.domain.user.entity.User;
import com.jj.moca.core.domain.user.exception.UserErrorCode;
import com.jj.moca.core.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncryptor passwordEncryptor;

    @Override
    @Transactional
    public UserResponse create(UserCreateRequest request) {
        String email = EmailFormatter.normalize(request.email());
        String name = request.name().trim();

        // 중복 이메일 처리
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(UserErrorCode.EMAIL_DUPLICATED);
        }

        String encodedPassword = passwordEncryptor.encrypt(request.password());

        try {
            User savedUser = userRepository.saveAndFlush(new User(email, name, encodedPassword));

            log.info("[UserServiceImpl] 회원가입 완료: userId={}, email={}",
                    savedUser.getId(), EmailFormatter.mask(email));

            return UserResponse.from(savedUser);
        } catch (DataIntegrityViolationException e) {
            if (isDuplicateEmailViolation(e)) {
                log.warn("[UserServiceImpl] 이메일 중복 제약 조건 위반 발생: email={}", EmailFormatter.mask(email));
                throw new BusinessException(UserErrorCode.EMAIL_DUPLICATED);
            }
            throw e;
        }
    }

    private boolean isDuplicateEmailViolation(DataIntegrityViolationException e) {
        if (e.getCause() instanceof ConstraintViolationException cve) {
            String constraintName = cve.getConstraintName();
            if (constraintName != null && isEmailConstraint(constraintName)) {
                return true;
            }
        }
        String message = e.getMostSpecificCause().getMessage();
        return message != null && isEmailConstraint(message);
    }

    private boolean isEmailConstraint(String value) {
        return value.toLowerCase(Locale.ROOT).contains("users_email_key");
    }
}
