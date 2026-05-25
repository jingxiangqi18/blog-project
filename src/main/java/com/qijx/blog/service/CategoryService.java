package com.qijx.blog.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.qijx.blog.entity.Category;
import com.qijx.blog.repository.CategoryRepository;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository){
        this.categoryRepository = categoryRepository;
    }

    public Category createCategory(Category category){
        return categoryRepository.save(category);
    }

    public List<Category> listCategories(){
        return categoryRepository.findAll();
    }
}
