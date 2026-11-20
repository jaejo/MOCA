package com.jj.moca.api.auth.service;

import com.jj.moca.api.auth.jwt.response.JwtDto;

public interface AuthService {

    JwtDto refresh(String refreshToken);
}
