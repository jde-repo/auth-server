package com.daeul.auth.controller;

import com.daeul.auth.dto.LoginRequest;
import com.daeul.auth.dto.ReissueRequest;
import com.daeul.auth.dto.SignupRequest;
import com.daeul.auth.dto.TokenResponse;
import com.daeul.auth.dto.UserResponse;
import com.daeul.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "인증 API", description = "회원가입, 로그인, 토큰 재발급, 로그아웃, 사용자 정보 조회")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "회원가입", description = "이메일과 비밀번호로 회원가입을 수행합니다.")
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody @Valid SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.ok("회원가입 성공");
    }

    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하여 AccessToken과 RefreshToken을 발급합니다.")
    @PostMapping("/login")
    public TokenResponse login(@RequestBody @Valid LoginRequest request, HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        return authService.login(request, ip);
    }

    @Operation(summary = "토큰 재발급", description = "RefreshToken을 이용해 AccessToken을 재발급합니다.")
    @PostMapping("/reissue")
    public TokenResponse reissue(@RequestBody ReissueRequest request) {
        return authService.reissue(request.getRefreshToken());
    }

    @Operation(summary = "로그아웃", description = "RefreshToken을 무효화합니다.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        authService.logout(token);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "JWT AccessToken을 이용하여 사용자 정보를 조회합니다.")
    public ResponseEntity<UserResponse> getMe(Authentication authentication) {
        UserResponse response = authService.getUserInfo(authentication);
        return ResponseEntity.ok(response);
    }
}
