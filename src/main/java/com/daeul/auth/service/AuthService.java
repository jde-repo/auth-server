package com.daeul.auth.service;

import static com.daeul.auth.common.ExceptionMessages.INVALID_TOKEN;

import com.daeul.auth.common.ExceptionMessages;
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
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
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
            throw new DuplicateEmailException(ExceptionMessages.EMAIL_ALREADY_EXISTS.getMessage());
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
                .orElseThrow(() -> new UserNotFoundException(ExceptionMessages.USER_NOT_FOUND.getMessage()));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidPasswordException(ExceptionMessages.INVALID_PASSWORD.getMessage());
        }

        // 3. 토큰 발급
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        // 4. RefreshToken 저장 (Redis)
        refreshTokenStore.saveToken(user.getId(), refreshToken);

        return new TokenResponse(accessToken, refreshToken);
    }



    public UserResponse getUserInfo(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findById(Long.valueOf(userDetails.getUsername()))
                .orElseThrow(() -> new UserNotFoundException(ExceptionMessages.USER_NOT_FOUND.getMessage()));
        return UserResponse.from(user);
    }

    public TokenResponse reissue(String refreshToken) {
        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);

        if (!refreshTokenStore.validateToken(userId, refreshToken)) {
            throw new InvalidRefreshTokenException(INVALID_TOKEN.getMessage());
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(userId);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId);

        refreshTokenStore.saveToken(userId, newRefreshToken);

        return new TokenResponse(newAccessToken, newRefreshToken);
    }

    public void logout(String email) {
        Optional<User> userOp = userRepository.findByEmail(email);
        userOp.ifPresent(user -> refreshTokenStore.removeToken(user.getId()));
    }

}
