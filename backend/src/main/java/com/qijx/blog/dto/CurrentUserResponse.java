package com.qijx.blog.dto;

import com.qijx.blog.entity.Role;
public class CurrentUserResponse {
    private final Long id;
    private final String username;
    private final Role role;

    public CurrentUserResponse(Long id, String username, Role role){
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
