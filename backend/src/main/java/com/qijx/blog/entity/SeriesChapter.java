package com.qijx.blog.entity;

import java.time.LocalDateTime;

public record SeriesChapter(
    Long id,
    Long seriesId,
    Long parentId,
    String title,
    int sortOrder,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
