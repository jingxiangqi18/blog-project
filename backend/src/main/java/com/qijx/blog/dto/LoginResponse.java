package com.qijx.blog.dto;

import com.qijx.blog.entity.Role;

public class LoginResponse {
    private final Long id;
    private final String username;
    private final Role role;

    public LoginResponse(Long id, String username, Role role){
        this.id = id;
        this.username = username;
        this.role = role;
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
