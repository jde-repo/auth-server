package com.daeul.auth.controller;

import com.daeul.auth.domain.entity.User;
import com.daeul.auth.dto.LoginRequest;
import com.daeul.auth.dto.SignupRequest;
import com.daeul.auth.dto.TokenResponse;
import com.daeul.auth.exception.DuplicateEmailException;
import com.daeul.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.ok("회원가입 성공");
    }


    @PostMapping("/login")
    public TokenResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/reissue")
    public TokenResponse reissue(@RequestParam String email, @RequestParam String refreshToken) {
        return authService.reissue(email, refreshToken);
    }

    @PostMapping("/logout")
    public String logout(@RequestParam String email) {
        authService.logout(email);
        return "로그아웃 성공";
    }


    @GetMapping("/me")
    public User getUserInfo(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return authService.getUserInfo(token);
    }
}
