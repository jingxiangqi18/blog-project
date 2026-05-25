package com.qijx.blog.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public Category createCategory(@Valid @RequestBody Category category){
        return categoryService.createCategory(category);
    }

    @GetMapping
    public List<Category> listCategories(){
        return categoryService.listCategories();
    }
}
