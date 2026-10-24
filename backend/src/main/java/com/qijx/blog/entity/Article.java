package com.qijx.blog.entity;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

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

    @Positive
    private Long sectionId;
    private String sectionSlug;
    private String sectionName;
    @Size(max = 500)
    private String summary;
    @NotBlank @Size(max = 50) @Pattern(regexp = "[a-z][a-z0-9]*(?:_[a-z0-9]+)*")
    private String contentType = "general";
    @Size(max = 50)
    private List<@NotNull @Positive Long> tagIds;
    private List<Tag> tags = List.of();
    private boolean sectionIdProvided;
    private boolean summaryProvided;
    private boolean contentTypeProvided;
    private boolean tagIdsProvided;

    public Long getSectionId(){ return sectionId; }
    public void setSectionId(Long sectionId){ this.sectionId = sectionId; this.sectionIdProvided = true; }
    public String getSectionSlug(){ return sectionSlug; }
    public void setSectionSlug(String sectionSlug){ this.sectionSlug = sectionSlug; }
    public String getSectionName(){ return sectionName; }
    public void setSectionName(String sectionName){ this.sectionName = sectionName; }
    public String getSummary(){ return summary; }
    public void setSummary(String summary){ this.summary = summary; this.summaryProvided = true; }
    public String getContentType(){ return contentType; }
    public void setContentType(String contentType){ this.contentType = contentType; this.contentTypeProvided = true; }
    public List<Long> getTagIds(){ return tagIds; }
    public void setTagIds(List<Long> tagIds){ this.tagIds = tagIds; this.tagIdsProvided = true; }
    public List<Tag> getTags(){ return tags; }
    public void setTags(List<Tag> tags){ this.tags = tags; }
    @JsonIgnore
    public boolean isSectionIdProvided(){ return sectionIdProvided; }
    @JsonIgnore
    public boolean isSummaryProvided(){ return summaryProvided; }
    @JsonIgnore
    public boolean isContentTypeProvided(){ return contentTypeProvided; }
    @JsonIgnore
    public boolean isTagIdsProvided(){ return tagIdsProvided; }

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
