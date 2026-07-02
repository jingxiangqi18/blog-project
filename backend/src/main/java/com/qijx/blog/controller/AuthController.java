package com.qijx.blog.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.qijx.blog.dto.CurrentUserResponse;
import com.qijx.blog.dto.LoginRequest;
import com.qijx.blog.dto.LoginResponse;
import com.qijx.blog.service.AuthService;
import com.qijx.blog.dto.RegisterRequest;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService){
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request){
        return authService.login(request);
    }

    @GetMapping("/me")
    public CurrentUserResponse me(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ){
        return authService.getCurrentUser(authorizationHeader);
    }

    @PostMapping("/register")
    public LoginResponse register(@Valid @RequestBody RegisterRequest request){
        return authService.register(request);
    }  
}
