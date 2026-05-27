package com.qijx.blog.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.qijx.blog.entity.Article;
import com.qijx.blog.entity.Category;
import com.qijx.blog.repository.CategoryRepository;
import com.qijx.blog.repository.ArticleRepository;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final ArticleRepository articleRepository;

    public CategoryService(CategoryRepository categoryRepository, ArticleRepository articleRepository){
        this.categoryRepository = categoryRepository;
        this.articleRepository = articleRepository;
    }

    public Category createCategory(Category category){
        return categoryRepository.save(category);
    }

    public List<Category> listCategories(){
        return categoryRepository.findAll();
    }

    public Category getCategory(Long id){
        return categoryRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    }

    public Category updateCategory(Long id, Category category){
        getCategory(id);

        categoryRepository.update(id, category);

        return getCategory(id);
    }

    public void deleteCategory(Long id){
        getCategory(id);
        categoryRepository.deleteById(id);
    }

    public List<Article> listArticlesByCategory(Long id){
        getCategory(id);
        return articleRepository.findByCategoryId(id);
    }
}
