package com.qijx.blog.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.qijx.blog.repository.UserRepository;
import com.qijx.blog.dto.UpdateUserEnabledRequest;
import com.qijx.blog.dto.UserResponse;
import com.qijx.blog.entity.User;

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

    public UserResponse updateUserEnabled(Long id, String authorizationHeader, UpdateUserEnabledRequest request){
        Claims claims = jwtService.parseAuthorizationHeader(authorizationHeader);

        String role = claims.get("role", String.class);
        Long currentUserId = claims.get("userId", Long.class);

        if(!"ADMIN".equals(role)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admin can update users");
        }

        if(id.equals(currentUserId)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admin cannot disable yourself");
        }
        
        userRepository.updateEnabled(id, request.getEnabled());

        User updatedUser = userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        return new UserResponse(updatedUser.getId(), updatedUser.getUsername(), updatedUser.getRole(), updatedUser.isEnabled());
    }
}
