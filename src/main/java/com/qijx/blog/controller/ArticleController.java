package com.qijx.blog.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.qijx.blog.entity.Article;
import com.qijx.blog.service.ArticleService;

@RestController
@RequestMapping("/api/articles")

public class ArticleController {
    
    private final ArticleService articleService;

    public ArticleController(ArticleService articleService){
        this.articleService = articleService;
    }

    @PostMapping
    public Article createArticle(@RequestBody Article article){
        return articleService.createArticle(article);
    }
    
}
