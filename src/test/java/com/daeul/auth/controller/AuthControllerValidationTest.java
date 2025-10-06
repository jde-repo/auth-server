package com.daeul.auth.controller;

import com.daeul.auth.security.JwtTokenProvider;
import com.daeul.auth.service.AuthService;
import com.daeul.auth.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(GlobalExceptionHandler.class) // 검증 예외를 JSON으로 내려주는 핸들러
class AuthControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    // 🔹 Controller가 의존하는 Bean들을 Mock 처리
    @MockBean
    private AuthService authService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("이메일 형식이 잘못된 경우 400 반환")
    void signupInvalidEmailTest() throws Exception {
        String invalidEmailJson =
                "{" +
                        "\"email\": \"invalid-email\"," +
                        "\"password\": \"password123\"" +
                        "}";

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidEmailJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].defaultMessage")
                        .value("올바른 이메일 형식이 아닙니다."));
    }

    @Test
    @DisplayName("이메일이 비어있는 경우 400 반환")
    void signupBlankEmailTest() throws Exception {
        String blankEmailJson =
                "{" +
                        "\"email\": \"\"," +
                        "\"password\": \"password123\"" +
                        "}";

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blankEmailJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].defaultMessage")
                        .value("이메일은 필수 입력값입니다."));
    }

    @Test
    @DisplayName("비밀번호가 비어있는 경우 400 반환")
    void signupBlankPasswordTest() throws Exception {
        String blankPasswordJson =
                "{" +
                        "\"email\": \"test@example.com\"," +
                        "\"password\": \"\"" +
                        "}";

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blankPasswordJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].defaultMessage")
                        .value("비밀번호는 필수 입력값입니다."));
    }
}
