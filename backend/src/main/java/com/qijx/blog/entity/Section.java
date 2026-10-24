package com.qijx.blog.entity;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class Section {
    private Long id;
    @NotBlank @Size(max = 100) @Pattern(regexp = "[a-z][a-z0-9]*(?:-[a-z0-9]+)*")
    private String slug;
    @NotBlank @Size(max = 100)
    private String name;
    @Size(max = 1000)
    private String description;
    @NotNull
    private Integer sortOrder = 0;
    @NotNull
    private Boolean enabled = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId(){ return id; }
    public void setId(Long id){ this.id = id; }
    public String getSlug(){ return slug; }
    public void setSlug(String slug){ this.slug = slug; }
    public String getName(){ return name; }
    public void setName(String name){ this.name = name; }
    public String getDescription(){ return description; }
    public void setDescription(String description){ this.description = description; }
    public Integer getSortOrder(){ return sortOrder; }
    public void setSortOrder(Integer sortOrder){ this.sortOrder = sortOrder; }
    public Boolean getEnabled(){ return enabled; }
    public void setEnabled(Boolean enabled){ this.enabled = enabled; }
    public LocalDateTime getCreatedAt(){ return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt){ this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt(){ return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt){ this.updatedAt = updatedAt; }
}
