package com.qijx.blog.entity;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class Tag {
    private Long id;
    @NotBlank @Size(max = 100)
    private String name;
    @NotBlank @Size(max = 100) @Pattern(regexp = "[a-z][a-z0-9]*(?:-[a-z0-9]+)*")
    private String slug;
    @Size(max = 1000)
    private String description;
    private LocalDateTime createdAt;

    public Long getId(){ return id; }
    public void setId(Long id){ this.id = id; }
    public String getName(){ return name; }
    public void setName(String name){ this.name = name; }
    public String getSlug(){ return slug; }
    public void setSlug(String slug){ this.slug = slug; }
    public String getDescription(){ return description; }
    public void setDescription(String description){ this.description = description; }
    public LocalDateTime getCreatedAt(){ return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt){ this.createdAt = createdAt; }
}
