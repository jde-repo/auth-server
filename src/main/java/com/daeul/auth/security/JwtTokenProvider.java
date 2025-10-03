package com.daeul.auth.security;

import io.jsonwebtoken.*;
import org.springframework.stereotype.Component;
import java.util.Date;

@Component
public class JwtTokenProvider {
    private final String secretKey = "secret1234"; // 실제 환경에서는 환경변수 등으로 관리
    private final long accessTokenValidity = 1000 * 60 * 30;       // 30분
    private final long refreshTokenValidity = 1000L * 60 * 60 * 24 * 7; // 7일

    public String generateAccessToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenValidity))
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }

    public String generateRefreshToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshTokenValidity))
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }

    public String validateAndGetEmail(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(secretKey)
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            throw new RuntimeException("토큰 검증 실패");
        }
    }
}
