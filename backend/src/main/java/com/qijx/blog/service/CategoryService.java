package com.qijx.blog.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.qijx.blog.entity.Article;
import com.qijx.blog.entity.Category;
import com.qijx.blog.repository.CategoryRepository;

import io.jsonwebtoken.Claims;

import com.qijx.blog.repository.ArticleRepository;
import com.qijx.blog.service.JwtService;
@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final ArticleRepository articleRepository;
    private final JwtService jwtService;

    public CategoryService(CategoryRepository categoryRepository, ArticleRepository articleRepository, JwtService jwtService){
        this.categoryRepository = categoryRepository;
        this.articleRepository = articleRepository;
        this.jwtService = jwtService;
    }

    public Category createCategory(Category category, String authorizationHeader){
        Claims claims = jwtService.parseAuthorizationHeader(authorizationHeader);
        checkAdmin(claims);

        return categoryRepository.save(category);
    }

    public List<Category> listCategories(){
        return categoryRepository.findAll();
    }

    public Category getCategory(Long id){
        return categoryRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    }

    public Category updateCategory(Long id, Category category, String authorizationHeader){
        Claims claims = jwtService.parseAuthorizationHeader(authorizationHeader);
        checkAdmin(claims);

        getCategory(id);

        categoryRepository.update(id, category);

        return getCategory(id);
    }

    @Transactional
    public void deleteCategory(Long id, String authorizationHeader){
        Claims claims = jwtService.parseAuthorizationHeader(authorizationHeader);
        checkAdmin(claims);

        getCategory(id);
        articleRepository.clearCategoryId(id);
        categoryRepository.deleteById(id);
    }

    public List<Article> listArticlesByCategory(Long id){
        getCategory(id);
        return articleRepository.findByCategoryId(id);
    }

    private void checkAdmin(Claims claims){
        String role = claims.get("role", String.class);

        if(!"ADMIN".equals(role)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admin can manage categories");
        }
    }
}
