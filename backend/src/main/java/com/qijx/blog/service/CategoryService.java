package com.qijx.blog.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DuplicateKeyException;

import com.qijx.blog.entity.Article;
import com.qijx.blog.entity.Category;
import com.qijx.blog.entity.Role;
import com.qijx.blog.entity.User;
import com.qijx.blog.repository.CategoryRepository;

import com.qijx.blog.repository.ArticleRepository;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final ArticleRepository articleRepository;
    private final CurrentUserService currentUserService;
    private final SectionService sectionService;

    public CategoryService(CategoryRepository categoryRepository, ArticleRepository articleRepository,
        CurrentUserService currentUserService, SectionService sectionService){
        this.categoryRepository = categoryRepository;
        this.articleRepository = articleRepository;
        this.currentUserService = currentUserService;
        this.sectionService = sectionService;
    }

    public Category createCategory(Category category, String authorizationHeader){
        User currentUser = currentUserService.getCurrentEnabledUser(authorizationHeader);
        checkAdmin(currentUser);

        sectionService.validateAssignment(category.getSectionId(), null);
        try{
            Category saved = categoryRepository.save(category);
            return getCategory(saved.getId());
        }catch(DuplicateKeyException exception){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category name already exists");
        }
    }

    public List<Category> listCategories(){
        return categoryRepository.findAll();
    }

    public List<Category> listCategories(Long sectionId, String sectionSlug){
        return categoryRepository.findAll(sectionService.resolveSectionId(sectionId, sectionSlug));
    }

    public Category getCategory(Long id){
        return categoryRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    }

    @Transactional
    public Category updateCategory(Long id, Category category, String authorizationHeader){
        User currentUser = currentUserService.getCurrentEnabledUser(authorizationHeader);
        checkAdmin(currentUser);

        Category existing = getCategory(id);
        if(!category.isSectionIdProvided()){
            category.setSectionId(existing.getSectionId());
        }
        sectionService.validateAssignment(category.getSectionId(), existing.getSectionId());
        if(category.getSectionId() != null && categoryRepository.hasConflictingContent(id, category.getSectionId())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category is used by content in another section");
        }
        try{
            categoryRepository.update(id, category);
        }catch(DuplicateKeyException exception){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category name already exists");
        }

        return getCategory(id);
    }

    @Transactional
    public void deleteCategory(Long id, String authorizationHeader){
        User currentUser = currentUserService.getCurrentEnabledUser(authorizationHeader);
        checkAdmin(currentUser);

        getCategory(id);
        articleRepository.clearCategoryId(id);
        categoryRepository.deleteById(id);
    }

    public List<Article> listArticlesByCategory(Long id){
        getCategory(id);
        return articleRepository.findByCategoryId(id);
    }

    private void checkAdmin(User currentUser){
        if(currentUser.getRole() != Role.ADMIN){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admin can manage categories");
        }
    }
}
