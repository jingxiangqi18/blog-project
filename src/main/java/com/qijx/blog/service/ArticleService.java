package com.qijx.blog.service;

import java.util.List;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

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

    public List<Article> listArticles(){
        return articleRepository.findAll();
    }

    public Article getArticle(Long id){
        return articleRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found"));
    }

    public Article updateArticle(Long id, Article article){
        getArticle(id);

        article.setUpdatedAt(LocalDateTime.now());
        articleRepository.update(id, article);

        return getArticle(id);
    }

    public void deleteArticle(Long id){
        getArticle(id);
        articleRepository.deleteById(id);
    }
}
