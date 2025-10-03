package com.daeul.auth.service;

import com.daeul.auth.domain.entity.User;
import com.daeul.auth.domain.repository.UserRepository;
import com.daeul.auth.dto.LoginRequest;
import com.daeul.auth.dto.SignupRequest;
import com.daeul.auth.dto.TokenResponse;
import com.daeul.auth.dto.UserResponse;
import com.daeul.auth.exception.DuplicateEmailException;
import com.daeul.auth.exception.InvalidPasswordException;
import com.daeul.auth.exception.InvalidRefreshTokenException;
import com.daeul.auth.exception.UserNotFoundException;
import com.daeul.auth.security.JwtTokenProvider;
import com.daeul.auth.security.LoginRateLimiter;
import com.daeul.auth.security.RefreshTokenStore;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenStore refreshTokenStore;
    private final LoginRateLimiter loginRateLimiter;

    public void signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("이미 가입된 이메일입니다.");
        }
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();
        userRepository.save(user);
    }

    public TokenResponse login(LoginRequest request, String ip) {
        // 1. IP 기반 Rate-Limiting
        loginRateLimiter.checkRateLimit(ip);

        // 2. 사용자 검증
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("존재하지 않는 이메일입니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidPasswordException("비밀번호가 일치하지 않습니다.");
        }

        // 3. 토큰 발급
        String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        // 4. RefreshToken 저장 (Redis)
        refreshTokenStore.saveToken(user.getEmail(), refreshToken);

        return new TokenResponse(accessToken, refreshToken);
    }



    public UserResponse getUserInfo(String token) {
        String email = jwtTokenProvider.validateAndGetEmail(token);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("해당 사용자가 존재하지 않습니다."));
        return UserResponse.from(user);
    }


    public TokenResponse reissue(String email, String refreshToken) {
        if (!refreshTokenStore.validateToken(email, refreshToken)) {
            throw new InvalidRefreshTokenException("유효하지 않은 Refresh Token입니다.");
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(email);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(email);

        refreshTokenStore.saveToken(email, newRefreshToken);

        return new TokenResponse(newAccessToken, newRefreshToken);
    }

    public void logout(String email) {
        refreshTokenStore.removeToken(email);
    }

}
