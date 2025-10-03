package com.daeul.auth.service;

import com.daeul.auth.domain.entity.User;
import com.daeul.auth.domain.repository.UserRepository;
import com.daeul.auth.dto.LoginRequest;
import com.daeul.auth.dto.SignupRequest;
import com.daeul.auth.dto.TokenResponse;
import com.daeul.auth.security.JwtTokenProvider;
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

    public void signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("이미 가입된 이메일입니다.");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        userRepository.save(user);
    }

    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("존재하지 않는 이메일"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("비밀번호 불일치");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        return new TokenResponse(accessToken, refreshToken);
    }

    public User getUserInfo(String token) {
        String email = jwtTokenProvider.validateAndGetEmail(token);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("유저 없음"));
    }


    public TokenResponse reissue(String email, String refreshToken) {
        if (!refreshTokenStore.validateToken(email, refreshToken)) {
            throw new RuntimeException("Refresh Token 불일치");
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(email);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(email);

        refreshTokenStore.saveToken(email, newRefreshToken); // 기존 토큰 갱신

        return new TokenResponse(newAccessToken, newRefreshToken);
    }

    public void logout(String email) {
        refreshTokenStore.removeToken(email);
    }
}
