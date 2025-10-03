package com.daeul.auth.service;

import com.daeul.auth.domain.entity.User;
import com.daeul.auth.domain.repository.UserRepository;
import com.daeul.auth.exception.DuplicateEmailException;
import org.junit.jupiter.api.Test;

import com.daeul.auth.dto.LoginRequest;
import com.daeul.auth.dto.SignupRequest;
import com.daeul.auth.dto.TokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setup() {
        userRepository.deleteAll(); // 테스트마다 깨끗하게
    }

    @Test
    @DisplayName("회원가입이 성공적으로 동작한다")
    void signupTest() {
        // given
        SignupRequest signupRequest = SignupRequest.builder().email("test@test.com").password("password123").build();

        // when
        authService.signup(signupRequest);

        // then
        User saved = userRepository.findByEmail("test@test.com").orElse(null);
        assertThat(saved).isNotNull();
        assertThat(passwordEncoder.matches("password123", saved.getPassword())).isTrue();
    }

    @Test
    @DisplayName("중복 이메일은 회원가입 안됨")
    void signupDupTest() {
        // given
        SignupRequest request1 = SignupRequest.builder()
                .email("test@test.com")
                .password("password123")
                .build();

        SignupRequest request2 = SignupRequest.builder()
                .email("test@test.com") // 동일한 이메일
                .password("password456")
                .build();

        // when
        authService.signup(request1); // 첫 가입은 성공

        // then - 두 번째 가입은 DuplicateEmailException 발생해야 함
        assertThatThrownBy(() -> authService.signup(request2))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessage("이미 가입된 이메일입니다.");
    }

    @Test
    @DisplayName("로그인 시 AccessToken과 RefreshToken이 발급된다")
    void loginTest() {
        // given
        SignupRequest signupRequest = SignupRequest.builder().email("user@test.com").password("password123").build();
        authService.signup(signupRequest);

        LoginRequest loginRequest = LoginRequest.builder().email("user@test.com").password("pass1234").build();

        // when
        TokenResponse tokens = authService.login(loginRequest);

        // then
        assertThat(tokens.getAccessToken()).isNotBlank();
        assertThat(tokens.getRefreshToken()).isNotBlank();
    }

    @Test
    @DisplayName("RefreshToken으로 새로운 AccessToken을 재발급할 수 있다")
    void reissueTest() {
        // given
        SignupRequest signupRequest = SignupRequest.builder().email("reissue@test.com").password("password123").build();
        authService.signup(signupRequest);

        LoginRequest loginRequest = LoginRequest.builder().email("reissue@test.com").password("pass1234").build();

        TokenResponse tokens = authService.login(loginRequest);

        // when
        TokenResponse newTokens = authService.reissue("reissue@test.com", tokens.getRefreshToken());

        // then
        assertThat(newTokens.getAccessToken()).isNotBlank();
        assertThat(newTokens.getRefreshToken()).isNotBlank();
        assertThat(newTokens.getAccessToken()).isNotEqualTo(tokens.getAccessToken());
    }
}
