package com.jj.moca.api.auth.details;

import com.jj.moca.core.common.util.EmailFormatter;
import com.jj.moca.core.domain.user.entity.User;
import com.jj.moca.core.domain.user.dto.response.UserResponse;
import com.jj.moca.core.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class MoCaUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        log.debug("[MoCaUserDetailsService] loadUserByUsername 호출됨, email: {}", EmailFormatter.mask(username));
        return loadUserDetails(username);
    }

    /**
     * 로그인 인증 시 사용자 정보를 조회하여 UserDetails로 변환합니다.
     */
    private UserDetails loadUserDetails(String username) {
        String normalizedEmail = EmailFormatter.normalize(username);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> {
                    log.warn("[MoCaUserDetailsService] 유저를 찾을 수 없습니다, email: {}", EmailFormatter.mask(normalizedEmail));
                    return new UsernameNotFoundException("해당 이메일의 사용자를 찾을 수 없습니다: " + normalizedEmail);
                });

        UserResponse userResponse = UserResponse.from(user);
        return new MoCaUserDetails(userResponse, user.getPassword());
    }

    /**
     * JWT 토큰 검증 시 필터(JwtAuthenticationFilter)에서 호출하는 메서드입니다.
     * 인증 완료 후 SecurityContext에 비밀번호 해시가 남지 않도록 빈 문자열("")로 생성하여 반환합니다.
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsernameForToken(String username) {
        log.debug("[MoCaUserDetailsService] loadUserByUsernameForToken 호출됨, email: {}", EmailFormatter.mask(username));
        String normalizedEmail = EmailFormatter.normalize(username);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> {
                    log.warn("[MoCaUserDetailsService] 토큰 검증 중 유저를 찾을 수 없습니다, email: {}", EmailFormatter.mask(normalizedEmail));
                    return new UsernameNotFoundException("해당 이메일의 사용자를 찾을 수 없습니다: " + normalizedEmail);
                });

        UserResponse userResponse = UserResponse.from(user);
        return new MoCaUserDetails(userResponse, "");
    }
}
