package com.qijx.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SeriesChapterRequest(
    @NotBlank @Size(max = 200) String title,
    @Positive Long parentId
) {}
