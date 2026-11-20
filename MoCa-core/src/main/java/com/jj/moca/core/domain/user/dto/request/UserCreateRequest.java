package com.jj.moca.core.domain.user.dto.request;

import com.jj.moca.core.common.util.PasswordValidationConstants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(

        @NotBlank(message = "이메일은 필수로 입력해야 합니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 100, message = "이메일은 100자 이하로 입력해야 합니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수로 입력해야 합니다.")
        @Pattern(
                regexp = PasswordValidationConstants.PASSWORD_REGEX,
                message = PasswordValidationConstants.PASSWORD_INVALID_MESSAGE
        )
        String password,

        @NotBlank(message = "이름은 필수로 입력해야 합니다.")
        @Size(min = 2, max = 50, message = "이름은 2자 이상 50자 이하로 입력해야 합니다.")
        String name
) {
    public UserCreateRequest {
        if (email != null) {
            email = email.trim();
        }
        if (name != null) {
            name = name.trim();
        }
    }
}
