package com.qijx.blog.entity;

public record SeriesArticle(int sortOrder, Article article, Long chapterId) {
    public SeriesArticle(int sortOrder, Article article){
        this(sortOrder, article, null);
    }
}
