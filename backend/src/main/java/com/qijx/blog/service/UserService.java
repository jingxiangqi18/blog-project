package com.qijx.blog.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.qijx.blog.repository.UserRepository;
import com.qijx.blog.dto.UserResponse;

import io.jsonwebtoken.Claims;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public UserService(UserRepository userRepository, JwtService jwtService){
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    public List<UserResponse> listUsers(String authorizationHeader){
        Claims claims = jwtService.parseAuthorizationHeader(authorizationHeader);
        String role = claims.get("role", String.class);

        if(!"ADMIN".equals(role)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admin can view users");
        }

        return userRepository.findAll().stream()
                .map(user -> new UserResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getRole(),
                    user.isEnabled()
                ))
                .toList();
    }
}
