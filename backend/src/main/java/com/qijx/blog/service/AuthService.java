package com.qijx.blog.service;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.qijx.blog.dto.CurrentUserResponse;
import com.qijx.blog.dto.LoginRequest;
import com.qijx.blog.dto.LoginResponse;
import com.qijx.blog.dto.RegisterRequest;
import com.qijx.blog.repository.UserRepository;
import com.qijx.blog.entity.Role;
import com.qijx.blog.entity.User;

import io.jsonwebtoken.Claims;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(JwtService jwtService, UserRepository userRepository){
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    public LoginResponse login(LoginRequest request){
        User user = userRepository.findByUserName(request.getUsername().trim()).orElseThrow(this::invalidCredentials);

        if(!user.isEnabled()){
            throw invalidCredentials();
        }

        if(!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())){
            throw invalidCredentials();
        }

        String token = jwtService.generateToken(user);

        return new LoginResponse(token, user.getId(), user.getUsername(), user.getRole());
    }

    public CurrentUserResponse getCurrentUser(String authorizationHeader){
        Claims claims = jwtService.parseAuthorizationHeader(authorizationHeader);

        Long userId = claims.get("userId", Long.class);

        User user = userRepository.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user"));

        if(!user.isEnabled()){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user");
        }

        return new CurrentUserResponse(user.getId(), user.getUsername(), user.getRole());
    }

    public LoginResponse register(RegisterRequest request){
        String username = request.getUsername().trim();

        if(userRepository.findByUserName(username).isPresent()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User already exists");
        }

        LocalDateTime now = LocalDateTime.now();

        User user = new User();

        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        user.setEnabled(true);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        User savedUser = userRepository.save(user);

        String token = jwtService.generateToken(savedUser);

        return new LoginResponse(token, savedUser.getId(), savedUser.getUsername(), savedUser.getRole());
    }

    private ResponseStatusException invalidCredentials(){
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
    }
}
