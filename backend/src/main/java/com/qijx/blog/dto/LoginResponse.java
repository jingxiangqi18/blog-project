package com.qijx.blog.dto;

import com.qijx.blog.entity.Role;

public class LoginResponse {
    private final String token;
    private final Long id;
    private final String username;
    private final Role role;

    public LoginResponse(String token, Long id, String username, Role role){
        this.token = token;
        this.id = id;
        this.username = username;
        this.role = role;
    }

    public String getToken(){
        return token;
    }

    public Long getId(){
        return id;
    }

    public String getUsername(){
        return username;
    }

    public Role getRole(){
        return role;
    }
}
