package com.daeul.auth.service;

import com.daeul.auth.common.ExceptionMessages;
import com.daeul.auth.domain.entity.User;
import com.daeul.auth.domain.repository.UserRepository;
import com.daeul.auth.exception.DuplicateEmailException;
import com.daeul.auth.exception.InvalidPasswordException;
import com.daeul.auth.exception.InvalidTokenException;
import com.daeul.auth.exception.TooManyLoginAttemptsException;
import com.daeul.auth.exception.UserNotFoundException;
import com.daeul.auth.security.RefreshTokenStore;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import com.daeul.auth.dto.LoginRequest;
import com.daeul.auth.dto.SignupRequest;
import com.daeul.auth.dto.TokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static com.daeul.auth.common.ExceptionMessages.EMAIL_ALREADY_EXISTS;
import static com.daeul.auth.common.ExceptionMessages.INVALID_TOKEN;
import static org.assertj.core.api.Assertions.*;

@ActiveProfiles("local")
@SpringBootTest
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RefreshTokenStore refreshTokenStore;

    @BeforeEach
    void setup() {
        userRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().flushAll();
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
    @DisplayName("정상 로그인")
    void loginSuccessTest() {
        // given
        SignupRequest signupRequest = SignupRequest.builder()
                .email("success@test.com")
                .password("password123")
                .build();
        authService.signup(signupRequest);

        LoginRequest loginRequest = LoginRequest.builder()
                .email("success@test.com")
                .password("password123")
                .build();

        // when
        TokenResponse tokens = authService.login(loginRequest, "127.0.0.1");

        // then
        assertThat(tokens.getAccessToken()).isNotBlank();
        assertThat(tokens.getRefreshToken()).isNotBlank();
    }

    @Test
    @DisplayName("TooManyRequests - 동일 IP에서 5회 이상 로그인 실패 시 차단")
    void loginTooManyRequestsTest() {
        // given
        SignupRequest signupRequest = SignupRequest.builder()
                .email("limit@test.com")
                .password("password123")
                .build();
        authService.signup(signupRequest);

        LoginRequest loginRequest = LoginRequest.builder()
                .email("limit@test.com")
                .password("wrongpass")
                .build();

        // when & then
        for (int i = 0; i < 5; i++) {
            try {
                authService.login(loginRequest, "192.168.0.1");
            } catch (InvalidPasswordException ignored) {}
        }

        assertThatThrownBy(() -> authService.login(loginRequest, "192.168.0.1"))
                .isInstanceOf(TooManyLoginAttemptsException.class)
                .hasMessageContaining("로그인 시도가 너무 많습니다");
    }

    @Test
    @DisplayName("존재하지 않는 이메일 로그인 실패")
    void loginUserNotFoundTest() {
        // given
        LoginRequest loginRequest = LoginRequest.builder()
                .email("notfound@test.com")
                .password("password123")
                .build();

        // when & then
        assertThatThrownBy(() -> authService.login(loginRequest, "127.0.0.1"))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("존재하지 않는 이메일");
    }

    @Test
    @DisplayName("비밀번호 불일치 로그인 실패")
    void loginInvalidPasswordTest() {
        // given
        SignupRequest signupRequest = SignupRequest.builder()
                .email("wrongpw@test.com")
                .password("password123")
                .build();
        authService.signup(signupRequest);

        LoginRequest loginRequest = LoginRequest.builder()
                .email("wrongpw@test.com")
                .password("wrongpassword")
                .build();

        // when & then
        assertThatThrownBy(() -> authService.login(loginRequest, "127.0.0.1"))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining(ExceptionMessages.INVALID_PASSWORD.getMessage());
    }

    @Test
    @DisplayName("RefreshToken으로 AccessToken 재발급 성공")
    void reissueSuccessTest() throws InterruptedException {
        // given
        SignupRequest signupRequest = SignupRequest.builder()
                .email("reissue@test.com")
                .password("password123")
                .build();
        authService.signup(signupRequest);

        LoginRequest loginRequest = LoginRequest.builder()
                .email("reissue@test.com")
                .password("password123")
                .build();
        TokenResponse tokens = authService.login(loginRequest, "127.0.0.1");

        // when
        TokenResponse newTokens = authService.reissue(tokens.getRefreshToken());

        // then
        assertThat(newTokens.getAccessToken()).isNotBlank();
        assertThat(newTokens.getRefreshToken()).isNotBlank();
        assertThat(newTokens.getAccessToken()).isNotEqualTo(tokens.getAccessToken()); // 새로운 토큰이어야 함
    }

    @Test
    @DisplayName("RefreshToken 불일치 시 재발급 실패")
    void reissueInvalidTokenTest() {
        // given
        SignupRequest signupRequest = SignupRequest.builder()
                .email("invalidRefresh@test.com")
                .password("password123")
                .build();
        authService.signup(signupRequest);

        // 로그인해서 RefreshToken 발급
        LoginRequest loginRequest = LoginRequest.builder()
                .email("invalidRefresh@test.com")
                .password("password123")
                .build();
        authService.login(loginRequest, "127.0.0.1");

        // when & then
        assertThatThrownBy(() -> authService.reissue("fake-refresh-token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining(INVALID_TOKEN.getMessage());
    }

    @Test
    @DisplayName("로그아웃 시 RefreshToken 제거됨")
    void logoutTest() {
        // given
        SignupRequest signupRequest = SignupRequest.builder()
                .email("logout@test.com")
                .password("password123")
                .build();
        authService.signup(signupRequest);

        LoginRequest loginRequest = LoginRequest.builder()
                .email("logout@test.com")
                .password("password123")
                .build();
        TokenResponse tokens = authService.login(loginRequest, "127.0.0.1");

        // 로그아웃 실행
        authService.logout(tokens.getAccessToken());

        Optional<User> userOp = userRepository.findByEmail("logout@test.com");

        // then
        String storedToken = refreshTokenStore.getToken(userOp.get().getId());
        assertThat(storedToken).isNull(); // 삭제되어야 함
    }

    @Test
    @DisplayName("로그아웃 이후 RefreshToken 재사용 불가")
    void logoutTokenReuseFailTest() {
        // given
        SignupRequest signupRequest = SignupRequest.builder()
                .email("reuse@test.com")
                .password("password123")
                .build();
        authService.signup(signupRequest);

        LoginRequest loginRequest = LoginRequest.builder()
                .email("reuse@test.com")
                .password("password123")
                .build();
        TokenResponse tokens = authService.login(loginRequest, "127.0.0.1");

        // 로그아웃 실행
        authService.logout(tokens.getAccessToken());

        // when & then
        assertThatThrownBy(() ->
                authService.reissue(tokens.getRefreshToken()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(INVALID_TOKEN.getMessage());
    }


    @Test
    @DisplayName("중복 이메일은 회원가입 안됨")
    void signupDuplicateEmailTest() {
        // given
        SignupRequest request1 = SignupRequest.builder()
                .email("duplicate@test.com")
                .password("password123")
                .build();

        SignupRequest request2 = SignupRequest.builder()
                .email("duplicate@test.com") // 동일 이메일
                .password("anotherPassword")
                .build();

        // when
        authService.signup(request1); // 첫 가입은 정상 동작

        // then - 두 번째 가입 시 DuplicateEmailException 발생해야 함
        assertThatThrownBy(() -> authService.signup(request2))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining(EMAIL_ALREADY_EXISTS.getMessage());
    }


}
