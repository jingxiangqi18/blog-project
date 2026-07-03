package com.qijx.blog.controller;

import java.util.List;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import com.qijx.blog.service.UserService;
import com.qijx.blog.dto.UserResponse;

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
}
