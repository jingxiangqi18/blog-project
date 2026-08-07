package com.qijx.blog.entity;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Size;

public class ArticleDraft {
    private Long id;
    private Long articleId;
    private Long authorId;

    @Size(max = 200)
    private String title;

    private String content;
    private Long categoryId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId(){
        return id;
    }

    public Long getArticleId(){
        return articleId;
    }

    public Long getAuthorId(){
        return authorId;
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

    public LocalDateTime getCreatedAt(){
        return createdAt;
    }

    public LocalDateTime getUpdatedAt(){
        return updatedAt;
    }

    public void setId(Long id){
        this.id = id;
    }

    public void setArticleId(Long articleId){
        this.articleId = articleId;
    }

    public void setAuthorId(Long authorId){
        this.authorId = authorId;
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

    public void setCreatedAt(LocalDateTime createdAt){
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt){
        this.updatedAt = updatedAt;
    }
}
