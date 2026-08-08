package com.qijx.blog.entity;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;

public class Comment {
    private Long id;
    private Long articleId;
    private Long parentId;
    
    @NotBlank
    private String content;

    private Long authorId;
    private String authorName;
    private String replyToAuthorName;
    private long likeCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId(){
        return id;
    }

    public Long getArticleId(){
        return articleId;
    }

    public String getContent(){
        return content;
    }

    public Long getParentId(){
        return parentId;
    }

    public LocalDateTime getCreatedAt(){
        return createdAt;
    }

    public LocalDateTime getUpdatedAt(){
        return updatedAt;
    }

    public Long getAuthorId(){
        return authorId;
    }

    public String getAuthorName(){
        return authorName;
    }

    public String getReplyToAuthorName(){
        return replyToAuthorName;
    }

    public long getLikeCount(){
        return likeCount;
    }

    public void setId(Long id){
        this.id = id;
    }

    public void setArticleId(Long articleId){
        this.articleId = articleId;
    }

    public void setParentId(Long parentId){
        this.parentId = parentId;
    }

    public void setContent(String content){
        this.content = content;
    }

    public void setCreatedAt(LocalDateTime createdAt){
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt){
        this.updatedAt = updatedAt;
    }

    public void setAuthorId(Long authorId){
        this.authorId = authorId;
    }

    public void setAuthorName(String authorName){
        this.authorName = authorName;
    }

    public void setReplyToAuthorName(String replyToAuthorName){
        this.replyToAuthorName = replyToAuthorName;
    }

    public void setLikeCount(long likeCount){
        this.likeCount = likeCount;
    }
}
