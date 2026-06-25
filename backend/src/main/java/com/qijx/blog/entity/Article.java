package com.qijx.blog.entity;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class Article {
    private Long id;

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    private String content;

    @NotNull
    private Long categoryId;

    private Long authorId;

    private String authorName;
    
    private String categoryName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId(){
        return id;
    }

    public String getTitle(){
        return title;
    }

    public String getContent(){
        return content;
    }

    public Long getCategoryId(){
        return categoryId;
    }

    public String getCategoryName(){
        return categoryName;
    }

    public Long getAuthorId(){
        return authorId;
    }

    public String getAuthorName(){
        return authorName;
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

    public void setContent(String content){
        this.content = content;
    }

    public void setCategoryId(Long categoryId){
        this.categoryId = categoryId;
    }

    public void setCategoryName(String categoryName){
        this.categoryName = categoryName;
    }
    public void setAuthorId(Long authorId){
        this.authorId = authorId;
    }

    public void setAuthorName(String authorName){
        this.authorName = authorName;
    }

    public void setCreatedAt(LocalDateTime createdAt){
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt){
        this.updatedAt = updatedAt;
    }
}
