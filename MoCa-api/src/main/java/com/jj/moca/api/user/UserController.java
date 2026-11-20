package com.jj.moca.api.user;

import com.jj.moca.core.domain.user.dto.request.UserCreateRequest;
import com.jj.moca.core.domain.user.dto.response.UserResponse;
import com.jj.moca.core.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    /**
     * 신규 회원 생성(회원가입)
     *
     * @param request 회원가입 요청 DTO
     * @return 생성된 회원 정보 DTO
     */
    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody UserCreateRequest request
    ) {

        UserResponse userResponse = userService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(userResponse);
    }
}
