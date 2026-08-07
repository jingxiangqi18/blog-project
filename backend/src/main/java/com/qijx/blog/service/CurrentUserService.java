package com.qijx.blog.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.qijx.blog.entity.User;
import com.qijx.blog.repository.UserRepository;

import io.jsonwebtoken.Claims;

@Service
public class CurrentUserService {
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public CurrentUserService(JwtService jwtService, UserRepository userRepository){
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    public User getCurrentEnabledUser(String authorizationHeader){
        Claims claims = jwtService.parseAuthorizationHeader(authorizationHeader);

        Long userId = claims.get("userId", Long.class);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user"));

        if(!user.isEnabled()){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user");
        }

        return user;
    }
}
