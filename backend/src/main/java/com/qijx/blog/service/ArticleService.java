package com.qijx.blog.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.qijx.blog.dto.PageResponse;
import com.qijx.blog.entity.Article;
import com.qijx.blog.repository.ArticleRepository;
import com.qijx.blog.repository.CommentRepository;

@Service
public class ArticleService {
    
    private final ArticleRepository articleRepository;
    private final CommentRepository commentRepository;
    private final CategoryService categoryService;

    public ArticleService(
        ArticleRepository articleRepository,
        CommentRepository commentRepository,
        CategoryService categoryService
    ){
        this.articleRepository = articleRepository;
        this.commentRepository = commentRepository;
        this.categoryService = categoryService;
    }

    public Article createArticle(Article article){
        categoryService.getCategory(article.getCategoryId());
        LocalDateTime now = LocalDateTime.now();
        article.setCreatedAt(now);
        article.setUpdatedAt(now);

        return articleRepository.save(article);
    }

    public PageResponse<Article> listArticles(int page, int size, String keyword, Long categoryId)  {
        if(page < 1){
            page = 1;
        }

        if(size < 1){
            size = 10;
        }

        if(size > 50){
            size = 50;
        }

        String normalizedKeyword;

        if(keyword == null){
            normalizedKeyword = "";
        }else{
            normalizedKeyword = keyword.trim();
        }

        return articleRepository.findPage(page, size, normalizedKeyword, categoryId);
    }

    public Article getArticle(Long id){
        return articleRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found"));
    }

    public Article updateArticle(Long id, Article article){
        getArticle(id);
        categoryService.getCategory(article.getCategoryId());

        article.setUpdatedAt(LocalDateTime.now());
        articleRepository.update(id, article);

        return getArticle(id);
    }

    @Transactional
    public void deleteArticle(Long id){
        getArticle(id);
        commentRepository.deleteByArticleId(id);
        articleRepository.deleteById(id);
    }
}
