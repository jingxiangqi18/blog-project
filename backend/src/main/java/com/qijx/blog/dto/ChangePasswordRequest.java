package com.qijx.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChangePasswordRequest {
    @NotBlank(message = "Old password must not be blank")
    private String oldPassword;

    @NotBlank(message = "New password must not be blank")
    @Size(min = 5, max = 72, message = "New password length must be between 6 and 72")
    private String newPassword;

    public String getOldPassword(){
        return oldPassword;
    }

    public String getNewPassword(){
        return newPassword;
    }

    public void setOldPassword(String oldPassword){
        this.oldPassword = oldPassword;
    }

    public void setNewPassword(String newPassword){
        this.newPassword = newPassword;
    }
}
