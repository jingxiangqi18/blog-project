package com.qijx.blog.service;

import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

import com.qijx.blog.entity.Article;
import com.qijx.blog.repository.ArticleRepository;

@Service
public class ArticleService {
    
    private final ArticleRepository articleRepository;

    public ArticleService(ArticleRepository articleRepository){
        this.articleRepository = articleRepository;
    }

    public Article createArticle(Article article){
        LocalDateTime now = LocalDateTime.now();
        article.setCreatedAt(now);
        article.setUpdatedAt(now);

        return articleRepository.save(article);
    }
}
