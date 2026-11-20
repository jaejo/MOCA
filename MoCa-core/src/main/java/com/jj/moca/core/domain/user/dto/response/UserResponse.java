package com.jj.moca.core.domain.user.dto.response;

import com.jj.moca.core.domain.user.entity.User;
import com.jj.moca.core.domain.user.entity.UserRole;
import lombok.Builder;

import java.util.UUID;

@Builder
public record UserResponse(
        UUID id,
        String email,
        String name,
        UserRole role
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole()
        );
    }
}
