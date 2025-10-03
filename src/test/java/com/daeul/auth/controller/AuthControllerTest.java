package com.daeul.auth.controller;

import com.daeul.auth.dto.LoginRequest;
import com.daeul.auth.exception.InvalidPasswordException;
import com.daeul.auth.security.JwtTokenProvider;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import com.daeul.auth.dto.SignupRequest;
import com.daeul.auth.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Date;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import io.jsonwebtoken.SignatureAlgorithm;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("정상 토큰으로 /me 호출 시 사용자 정보 반환")
    void getMeWithValidToken() throws Exception {
        // given
        authService.signup(SignupRequest.builder()
                .email("me@test.com")
                .password("password123")
                .build());

        String accessToken = jwtTokenProvider.generateAccessToken("me@test.com");

        // when & then
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("me@test.com"));
    }

    @Test
    @DisplayName("잘못된 토큰으로 /me 호출 시 401 반환")
    void getMeWithInvalidToken() throws Exception {
        // given
        String invalidToken = "this.is.invalid.token";

        // when & then
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + invalidToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("유효하지 않은 토큰입니다."));
    }

    @Test
    @DisplayName("만료된 토큰으로 /me 호출 시 401 반환")
    void getMeWithExpiredToken() throws Exception {
        // given
        authService.signup(SignupRequest.builder()
                .email("expired@test.com")
                .password("password123")
                .build());

        String expiredToken = Jwts.builder()
                .setSubject("expired@test.com")
                .setIssuedAt(new Date(System.currentTimeMillis() - 1000 * 60 * 60)) // 1시간 전 발급
                .setExpiration(new Date(System.currentTimeMillis() - 1000)) // 이미 만료
                .signWith(SignatureAlgorithm.HS256, jwtSecret)
                .compact();

        // when & then
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + expiredToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("토큰이 만료되었습니다."));
    }

}

