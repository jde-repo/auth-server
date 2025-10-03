package com.daeul.auth.security;

import static com.daeul.auth.common.ExceptionMessages.EXPIRED_TOKEN;
import static com.daeul.auth.common.ExceptionMessages.INVALID_TOKEN;

import com.daeul.auth.common.ExceptionMessages;
import com.daeul.auth.domain.entity.User;
import com.daeul.auth.domain.repository.UserRepository;
import com.daeul.auth.exception.ExpiredTokenException;
import com.daeul.auth.exception.InvalidRefreshTokenException;
import com.daeul.auth.exception.InvalidTokenException;
import io.jsonwebtoken.*;
import java.util.Collections;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import java.util.Date;
import com.daeul.auth.config.JwtProperties;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private final UserRepository userRepository;

    public String generateAccessToken(Long userId) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))   // sub = userId
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getAccessTokenValidity()))
                .setId(UUID.randomUUID().toString())
                .signWith(SignatureAlgorithm.HS256, jwtProperties.getSecret())
                .compact();
    }

    public String generateRefreshToken(Long userId) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))   // sub = userId
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getRefreshTokenValidity()))
                .setId(UUID.randomUUID().toString())
                .signWith(SignatureAlgorithm.HS256, jwtProperties.getSecret())
                .compact();
    }

    public Authentication getAuthentication(String token) {
        Long userId = getUserIdFromToken(token);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException(ExceptionMessages.USER_NOT_FOUND.getMessage()));

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(String.valueOf(user.getId()))
                .password(user.getPassword())
                .authorities(Collections.emptyList())
                .build();

        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
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

    public Long getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(jwtProperties.getSecret())
                    .parseClaimsJws(token)
                    .getBody();
            return Long.valueOf(claims.getSubject());
        } catch (ExpiredJwtException e) {
            throw new ExpiredTokenException(EXPIRED_TOKEN.getMessage());
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException(INVALID_TOKEN.getMessage());
        }
    }
}
