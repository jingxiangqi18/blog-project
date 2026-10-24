package com.qijx.blog.dto;

public record SeriesArticleSummary(
    Long id,
    String title,
    String summary,
    Long chapterId,
    int sortOrder
) {}
