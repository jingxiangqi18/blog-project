package com.qijx.blog.dto;

import jakarta.validation.constraints.NotNull;

public class UpdateUserEnabledRequest {
    @NotNull(message = "Enabled must not be null")
    private boolean enabled;

    public boolean getEnabled(){
        return enabled;
    }

    public void setEnabled(boolean enabled){
        this.enabled = enabled;
    }
}
