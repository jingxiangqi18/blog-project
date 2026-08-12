package com.qijx.blog.entity;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class KnowledgeCard {
    private Long id;

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    @Size(max = 500)
    private String summary;

    @NotBlank
    private String content;

    private Long createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId(){
        return id;
    }

    public String getTitle(){
        return title;
    }

    public String getSummary(){
        return summary;
    }

    public String getContent(){
        return content;
    }

    public Long getCreatedBy(){
        return createdBy;
    }

    public String getCreatedByName(){
        return createdByName;
    }

    public LocalDateTime getCreatedAt(){
        return createdAt;
    }

    public LocalDateTime getUpdatedAt(){
        return updatedAt;
    }

    public void setId(Long id){
        this.id = id;
    }

    public void setTitle(String title){
        this.title = title;
    }

    public void setSummary(String summary){
        this.summary = summary;
    }

    public void setContent(String content){
        this.content = content;
    }

    public void setCreatedBy(Long createdBy){
        this.createdBy = createdBy;
    }

    public void setCreatedByName(String createdByName){
        this.createdByName = createdByName;
    }

    public void setCreatedAt(LocalDateTime createdAt){
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt){
        this.updatedAt = updatedAt;
    }
}
