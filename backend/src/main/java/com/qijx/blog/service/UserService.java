package com.qijx.blog.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.qijx.blog.repository.UserRepository;
import com.qijx.blog.dto.UpdateUserEnabledRequest;
import com.qijx.blog.dto.UserResponse;
import com.qijx.blog.dto.ChangePasswordRequest;
import com.qijx.blog.entity.User;

import io.jsonwebtoken.Claims;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

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

    public void changeMyPassword(String authorizationHeader, ChangePasswordRequest request){
        Claims claims = jwtService.parseAuthorizationHeader(authorizationHeader);

        Long currentUserId = claims.get("userId", Long.class);

        User user = userRepository.findById(currentUserId).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user"));

        if(!user.isEnabled()){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user");
        }

        if(!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Old password is incorrect");
        }

        String newPassworHash = passwordEncoder.encode(request.getNewPassword());

        userRepository.updatePasswordHash(currentUserId, newPassworHash);
    }
}
