package com.qijx.blog.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.qijx.blog.dto.LoginRequest;
import com.qijx.blog.dto.LoginResponse;
import com.qijx.blog.repository.UserRepository;
import com.qijx.blog.entity.User;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserRepository userRepository){
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

        return new LoginResponse(user.getId(), user.getUsername(), user.getRole());
    }

    private ResponseStatusException invalidCredentials(){
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
    }
}
