package com.qijx.blog.controller;

import java.util.List;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;

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

    @GetMapping
    public List<Article> listArticles(){
        return articleService.listArticles();
    }

    @GetMapping("/{id}")
    public Article getArticle(@PathVariable Long id){
        return ArticleService.getArticle(id);
    }
}
