package com.qijx.blog.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.qijx.blog.entity.Article;
import com.qijx.blog.entity.Category;
import com.qijx.blog.service.CategoryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/categories")

public class CategoryController {
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService){
        this.categoryService = categoryService;
    }

    @PostMapping
    public Category createCategory(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @Valid @RequestBody Category category
    ){
        return categoryService.createCategory(category, authorizationHeader);
    }

    @GetMapping
    public List<Category> listCategories(){
        return categoryService.listCategories();
    }

    @GetMapping("/{id}")
    public Category getCategory(@PathVariable Long id){
        return categoryService.getCategory(id);
    }

    @PutMapping("/{id}")
    public Category updateCategory(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long id,
        @Valid @RequestBody Category category
    ){
        return categoryService.updateCategory(id, category, authorizationHeader);
    }

    @DeleteMapping("/{id}")
    public void deleteCategory(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long id
    ){
        categoryService.deleteCategory(id, authorizationHeader);
    }

    @GetMapping("/{id}/articles")
    public List<Article> listArticlesByCategory(@PathVariable Long id){
        return categoryService.listArticlesByCategory(id);
    }
}
