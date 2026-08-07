package com.qijx.blog.dto;

import jakarta.validation.constraints.NotNull;

public class UpdateUserEnabledRequest {
    @NotNull(message = "Enabled must not be null")
    private Boolean enabled;

    public Boolean getEnabled(){
        return enabled;
    }

    public void setEnabled(Boolean enabled){
        this.enabled = enabled;
    }
}
