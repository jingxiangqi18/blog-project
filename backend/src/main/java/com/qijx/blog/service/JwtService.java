package com.qijx.blog.service;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import com.qijx.blog.entity.User;

import org.springframework.beans.factory.annotation.Value;

@Service
public class JwtService {
    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtService(
        @Value("${app.jwt.secret}") String secret,
        @Value("${app.jwt.expiration-ms}") long expirationMs
    ){
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationMs = expirationMs;
    }

    public String generateToken(User user){
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }
}
