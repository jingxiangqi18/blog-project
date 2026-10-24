package com.qijx.blog.dto;

import java.util.List;

import com.qijx.blog.entity.SeriesChapter;

public record SeriesDirectoryResponse(
    Long id,
    String slug,
    String title,
    String description,
    Long sectionId,
    String sectionSlug,
    String sectionName,
    Long categoryId,
    List<SeriesChapter> chapters,
    List<SeriesArticleSummary> articles
) {}
