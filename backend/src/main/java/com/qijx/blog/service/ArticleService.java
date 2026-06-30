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

import io.jsonwebtoken.Claims;

@Service
public class ArticleService {
    
    private final ArticleRepository articleRepository;
    private final CommentRepository commentRepository;
    private final CategoryService categoryService;
    private final JwtService jwtService;

    public ArticleService(
        ArticleRepository articleRepository,
        CommentRepository commentRepository,
        CategoryService categoryService,
        JwtService jwtService
    ){
        this.articleRepository = articleRepository;
        this.commentRepository = commentRepository;
        this.categoryService = categoryService;
        this.jwtService = jwtService;
    }

    public Article createArticle(Article article, String authorizationHeader){
        Claims claims = jwtService.parseAuthorizationHeader(authorizationHeader);

        Long userId = claims.get("userId", Long.class);

        categoryService.getCategory(article.getCategoryId());

        article.setAuthorId(userId);

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

    public Article updateArticle(Long id, Article article, String authorizationHeader){
        Claims claims = jwtService.parseAuthorizationHeader(authorizationHeader);
        Article existingArticle = getArticle(id);

        checkArticlePermission(existingArticle, claims);

        categoryService.getCategory(article.getCategoryId());

        article.setUpdatedAt(LocalDateTime.now());
        articleRepository.update(id, article);

        return getArticle(id);
    }

    @Transactional
    public void deleteArticle(Long id, String authorizationHeader){
        Claims claims = jwtService.parseAuthorizationHeader(authorizationHeader);
        Article existingArticle = getArticle(id);

        checkArticlePermission(existingArticle, claims);

        commentRepository.deleteByArticleId(id);
        articleRepository.deleteById(id);
    }

    private void checkArticlePermission(Article article, Claims claims){
        Long currentUserId = claims.get("userId", Long.class);
        String role = claims.get("role", String.class);

        boolean isAdmin = "ADMIN".equals(role);
        boolean isAuthor = (article.getAuthorId() != null) && (article.getAuthorId().equals(currentUserId));

        if(!isAdmin && !isAuthor){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No Permission to operate current article");
        }
    }
}
