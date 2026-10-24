package com.qijx.blog.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SeriesChapterOrderRequest(
    @Positive Long parentId,
    @NotNull @Size(max = 500) List<@NotNull @Positive Long> chapterIds
) {}
