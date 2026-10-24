package com.qijx.blog.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class Category {
    private Long id;

    @NotBlank
    @Size(max = 100)
    private String name;

    @Positive
    private Long sectionId;
    private String sectionSlug;
    private String sectionName;
    private boolean sectionIdProvided;

    public Long getSectionId(){ return sectionId; }
    public void setSectionId(Long sectionId){ this.sectionId = sectionId; this.sectionIdProvided = true; }
    public String getSectionSlug(){ return sectionSlug; }
    public void setSectionSlug(String sectionSlug){ this.sectionSlug = sectionSlug; }
    public String getSectionName(){ return sectionName; }
    public void setSectionName(String sectionName){ this.sectionName = sectionName; }
    @JsonIgnore
    public boolean isSectionIdProvided(){ return sectionIdProvided; }

    public Long getId(){
        return id;
    }

    public String getName(){
        return name;
    }

    public void setId(Long id){
        this.id = id;
    }

    public void setName(String name){
        this.name = name;
    }
}
