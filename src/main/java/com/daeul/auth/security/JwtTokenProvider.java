package com.daeul.auth.security;

import static com.daeul.auth.common.ExceptionMessages.EXPIRED_TOKEN;
import static com.daeul.auth.common.ExceptionMessages.INVALID_TOKEN;

import com.daeul.auth.exception.ExpiredTokenException;
import com.daeul.auth.exception.InvalidTokenException;
import io.jsonwebtoken.*;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Date;
import com.daeul.auth.config.JwtProperties;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    public String generateAccessToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getAccessTokenValidity()))
                .setId(UUID.randomUUID().toString())
                .signWith(SignatureAlgorithm.HS256, jwtProperties.getSecret())
                .compact();
    }

    public String generateRefreshToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getRefreshTokenValidity()))
                .setId(UUID.randomUUID().toString())
                .signWith(SignatureAlgorithm.HS256, jwtProperties.getSecret())
                .compact();
    }


    public String validateAndGetEmail(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(jwtProperties.getSecret())
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (ExpiredJwtException e) {
            throw new ExpiredTokenException(EXPIRED_TOKEN.getMessage());
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException(INVALID_TOKEN.getMessage());
        }
    }
}
