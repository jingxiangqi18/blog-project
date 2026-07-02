package com.qijx.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {
    @NotBlank(message = "Username must not be blank")
    @Size(min = 3, max = 30, message = "Username length must be between 3 and 30")
    private String username;

    @NotBlank(message = "Password must not be blank")
    @Size(min = 5, max = 72, message = "Password length must be between 5 and 72")
    private String password;

    public String getUsername(){
        return username;
    }

    public String getPassword(){
        return password;
    }

    public void setUsername(String username){
        this.username = username;
    }

    public void setPassword(String password){
        this.password = password;
    }
}
