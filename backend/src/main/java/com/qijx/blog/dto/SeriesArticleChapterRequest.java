package com.qijx.blog.dto;

import jakarta.validation.constraints.Positive;

public record SeriesArticleChapterRequest(@Positive Long chapterId) {}
