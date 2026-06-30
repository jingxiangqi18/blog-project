package com.qijx.blog.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PathVariable;

import com.qijx.blog.dto.PageResponse;
import com.qijx.blog.entity.Article;
import com.qijx.blog.service.ArticleService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/articles")

public class ArticleController {
    
    private final ArticleService articleService;

    public ArticleController(ArticleService articleService){
        this.articleService = articleService;
    }

    @PostMapping
    public Article createArticle(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @Valid @RequestBody Article article
    ){
        return articleService.createArticle(article, authorizationHeader);
    }

    @GetMapping
    public PageResponse<Article> listArticles(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Long categoryId
    ){
        return articleService.listArticles(page, size, keyword, categoryId);
    }

    @GetMapping("/{id}")
    public Article getArticle(@PathVariable Long id){
        return articleService.getArticle(id);
    }

    @PutMapping("/{id}")
    public Article updateArticle(
        @PathVariable Long id,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @Valid @RequestBody Article article
    ){
        return articleService.updateArticle(id, article, authorizationHeader);
    }

    @DeleteMapping("/{id}")
    public void deleteArticle(
        @PathVariable Long id,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
        ){
        articleService.deleteArticle(id, authorizationHeader);
    }
}
