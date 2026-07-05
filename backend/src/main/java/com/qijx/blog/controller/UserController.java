package com.qijx.blog.controller;

import java.util.List;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import com.qijx.blog.service.UserService;

import jakarta.validation.Valid;

import com.qijx.blog.dto.UserResponse;
import com.qijx.blog.dto.UpdateUserEnabledRequest;
import com.qijx.blog.dto.ChangePasswordRequest;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService){
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> listUsers(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ){
        return userService.listUsers(authorizationHeader);
    }

    @PatchMapping("/{id}/enabled")
    public UserResponse updateUserEnabled(
        @PathVariable Long id,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @Valid @RequestBody UpdateUserEnabledRequest request
    ){
        return userService.updateUserEnabled(id, authorizationHeader, request);
    }

    @PatchMapping("/me/password")
    public void changeMyPassword(
        @RequestHeader(value = "Authorization", required = false) String authorizationheader,
        @Valid @RequestBody ChangePasswordRequest request
    ){
        userService.changeMyPassword(authorizationheader, request);
    }
}
