package com.qijx.blog.entity;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class Series {
    private Long id;
    @NotBlank @Size(max = 100) @Pattern(regexp = "[a-z][a-z0-9]*(?:-[a-z0-9]+)*")
    private String slug;
    @NotBlank @Size(max = 200)
    private String title;
    @Size(max = 1000)
    private String description;
    @NotNull @Positive
    private Long sectionId;
    @Positive
    private Long categoryId;
    private String sectionSlug;
    private String sectionName;
    private String categoryName;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<SeriesArticle> articles;

    public Long getId(){ return id; }
    public void setId(Long id){ this.id = id; }
    public String getSlug(){ return slug; }
    public void setSlug(String slug){ this.slug = slug; }
    public String getTitle(){ return title; }
    public void setTitle(String title){ this.title = title; }
    public String getDescription(){ return description; }
    public void setDescription(String description){ this.description = description; }
    public Long getSectionId(){ return sectionId; }
    public void setSectionId(Long sectionId){ this.sectionId = sectionId; }
    public Long getCategoryId(){ return categoryId; }
    public void setCategoryId(Long categoryId){ this.categoryId = categoryId; }
    public String getSectionSlug(){ return sectionSlug; }
    public void setSectionSlug(String sectionSlug){ this.sectionSlug = sectionSlug; }
    public String getSectionName(){ return sectionName; }
    public void setSectionName(String sectionName){ this.sectionName = sectionName; }
    public String getCategoryName(){ return categoryName; }
    public void setCategoryName(String categoryName){ this.categoryName = categoryName; }
    public Long getCreatedBy(){ return createdBy; }
    public void setCreatedBy(Long createdBy){ this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt(){ return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt){ this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt(){ return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt){ this.updatedAt = updatedAt; }
    public List<SeriesArticle> getArticles(){ return articles; }
    public void setArticles(List<SeriesArticle> articles){ this.articles = articles; }
}
