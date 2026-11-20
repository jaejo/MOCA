package com.jj.moca.core.domain.user.service;

import com.jj.moca.core.domain.user.dto.request.UserCreateRequest;
import com.jj.moca.core.domain.user.dto.response.UserResponse;

public interface UserService {

    UserResponse create(UserCreateRequest request);
}
