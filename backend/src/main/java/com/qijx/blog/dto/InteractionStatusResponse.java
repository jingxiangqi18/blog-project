package com.qijx.blog.dto;

public class InteractionStatusResponse {
    private final long count;
    private final boolean active;

    public InteractionStatusResponse(long count, boolean active){
        this.count = count;
        this.active = active;
    }

    public long getCount(){
        return count;
    }

    public boolean isActive(){
        return active;
    }
}
