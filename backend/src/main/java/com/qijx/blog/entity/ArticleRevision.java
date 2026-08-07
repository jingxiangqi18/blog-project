package com.qijx.blog.entity;

import java.time.LocalDateTime;

public class ArticleRevision {
    private Long id;
    private Long articleId;
    private int revisionNumber;
    private String title;
    private String content;
    private Long categoryId;
    private Long createdBy;
    private String createdByName;
    private LocalDateTime createdAt;

    public Long getId(){
        return id;
    }

    public Long getArticleId(){
        return articleId;
    }

    public int getRevisionNumber(){
        return revisionNumber;
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

    public Long getCreatedBy(){
        return createdBy;
    }

    public String getCreatedByName(){
        return createdByName;
    }

    public LocalDateTime getCreatedAt(){
        return createdAt;
    }

    public void setId(Long id){
        this.id = id;
    }

    public void setArticleId(Long articleId){
        this.articleId = articleId;
    }

    public void setRevisionNumber(int revisionNumber){
        this.revisionNumber = revisionNumber;
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

    public void setCreatedBy(Long createdBy){
        this.createdBy = createdBy;
    }

    public void setCreatedByName(String createdByName){
        this.createdByName = createdByName;
    }

    public void setCreatedAt(LocalDateTime createdAt){
        this.createdAt = createdAt;
    }
}
