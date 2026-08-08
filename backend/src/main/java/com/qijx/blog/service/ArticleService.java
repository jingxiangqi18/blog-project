package com.qijx.blog.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.qijx.blog.dto.PageResponse;
import com.qijx.blog.dto.InteractionStatusResponse;
import com.qijx.blog.entity.Article;
import com.qijx.blog.entity.ArticleDraft;
import com.qijx.blog.entity.ArticleRevision;
import com.qijx.blog.entity.Role;
import com.qijx.blog.entity.User;
import com.qijx.blog.repository.ArticleRepository;
import com.qijx.blog.repository.ArticleInteractionRepository;
import com.qijx.blog.repository.ArticleDraftRepository;
import com.qijx.blog.repository.ArticleRevisionRepository;
import com.qijx.blog.repository.CommentRepository;

@Service
public class ArticleService {
    
    private final ArticleRepository articleRepository;
    private final ArticleInteractionRepository articleInteractionRepository;
    private final ArticleDraftRepository articleDraftRepository;
    private final ArticleRevisionRepository articleRevisionRepository;
    private final CommentRepository commentRepository;
    private final CategoryService categoryService;
    private final CurrentUserService currentUserService;

    public ArticleService(
        ArticleRepository articleRepository,
        ArticleInteractionRepository articleInteractionRepository,
        ArticleDraftRepository articleDraftRepository,
        ArticleRevisionRepository articleRevisionRepository,
        CommentRepository commentRepository,
        CategoryService categoryService,
        CurrentUserService currentUserService
    ){
        this.articleRepository = articleRepository;
        this.articleInteractionRepository = articleInteractionRepository;
        this.articleDraftRepository = articleDraftRepository;
        this.articleRevisionRepository = articleRevisionRepository;
        this.commentRepository = commentRepository;
        this.categoryService = categoryService;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public Article createArticle(Article article, String authorizationHeader){
        User currentUser = currentUserService.getCurrentEnabledUser(authorizationHeader);

        categoryService.getCategory(article.getCategoryId());

        article.setAuthorId(currentUser.getId());

        LocalDateTime now = LocalDateTime.now();
        article.setCreatedAt(now);
        article.setUpdatedAt(now);

        Article savedArticle = articleRepository.save(article);
        articleRevisionRepository.saveSnapshot(savedArticle, currentUser.getId(), now);

        return savedArticle;
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

    public InteractionStatusResponse getLikeStatus(Long id, String authorizationHeader){
        getArticle(id);
        boolean liked = false;

        if(authorizationHeader != null && !authorizationHeader.isBlank()){
            User currentUser = currentUserService.getCurrentEnabledUser(authorizationHeader);
            liked = articleInteractionRepository.hasLiked(id, currentUser.getId());
        }

        return new InteractionStatusResponse(articleInteractionRepository.countLikes(id), liked);
    }

    public InteractionStatusResponse likeArticle(Long id, String authorizationHeader){
        getArticle(id);
        User currentUser = currentUserService.getCurrentEnabledUser(authorizationHeader);
        articleInteractionRepository.addLike(id, currentUser.getId(), LocalDateTime.now());
        return new InteractionStatusResponse(articleInteractionRepository.countLikes(id), true);
    }

    public InteractionStatusResponse unlikeArticle(Long id, String authorizationHeader){
        getArticle(id);
        User currentUser = currentUserService.getCurrentEnabledUser(authorizationHeader);
        articleInteractionRepository.removeLike(id, currentUser.getId());
        return new InteractionStatusResponse(articleInteractionRepository.countLikes(id), false);
    }

    public InteractionStatusResponse getFavoriteStatus(Long id, String authorizationHeader){
        getArticle(id);
        boolean favorited = false;

        if(authorizationHeader != null && !authorizationHeader.isBlank()){
            User currentUser = currentUserService.getCurrentEnabledUser(authorizationHeader);
            favorited = articleInteractionRepository.hasFavorited(id, currentUser.getId());
        }

        return new InteractionStatusResponse(articleInteractionRepository.countFavorites(id), favorited);
    }

    public InteractionStatusResponse favoriteArticle(Long id, String authorizationHeader){
        getArticle(id);
        User currentUser = currentUserService.getCurrentEnabledUser(authorizationHeader);
        articleInteractionRepository.addFavorite(id, currentUser.getId(), LocalDateTime.now());
        return new InteractionStatusResponse(articleInteractionRepository.countFavorites(id), true);
    }

    public InteractionStatusResponse unfavoriteArticle(Long id, String authorizationHeader){
        getArticle(id);
        User currentUser = currentUserService.getCurrentEnabledUser(authorizationHeader);
        articleInteractionRepository.removeFavorite(id, currentUser.getId());
        return new InteractionStatusResponse(articleInteractionRepository.countFavorites(id), false);
    }

    @Transactional
    public Article updateArticle(Long id, Article article, String authorizationHeader){
        User currentUser = currentUserService.getCurrentEnabledUser(authorizationHeader);
        Article existingArticle = getArticle(id);

        checkArticlePermission(existingArticle, currentUser);

        categoryService.getCategory(article.getCategoryId());

        article.setUpdatedAt(LocalDateTime.now());
        articleRepository.update(id, article);

        Article updatedArticle = getArticle(id);
        articleRevisionRepository.saveSnapshot(updatedArticle, currentUser.getId(), updatedArticle.getUpdatedAt());

        return updatedArticle;
    }

    public ArticleDraft createDraft(ArticleDraft draft, String authorizationHeader){
        User currentUser = currentUserService.getCurrentEnabledUser(authorizationHeader);
        validateDraftCategory(draft.getCategoryId());

        LocalDateTime now = LocalDateTime.now();
        draft.setArticleId(null);
        draft.setAuthorId(currentUser.getId());
        draft.setTitle(normalizeDraftText(draft.getTitle()));
        draft.setContent(normalizeDraftText(draft.getContent()));
        draft.setCreatedAt(now);
        draft.setUpdatedAt(now);

        return articleDraftRepository.save(draft);
    }

    public ArticleDraft getDraft(Long draftId, String authorizationHeader){
        User currentUser = currentUserService.getCurrentEnabledUser(authorizationHeader);
        ArticleDraft draft = findDraft(draftId);
        checkDraftPermission(draft, currentUser);
        return draft;
    }

    public ArticleDraft updateDraft(Long draftId, ArticleDraft request, String authorizationHeader){
        User currentUser = currentUserService.getCurrentEnabledUser(authorizationHeader);
        ArticleDraft draft = findDraft(draftId);
        checkDraftPermission(draft, currentUser);
        validateDraftCategory(request.getCategoryId());

        draft.setTitle(normalizeDraftText(request.getTitle()));
        draft.setContent(normalizeDraftText(request.getContent()));
        draft.setCategoryId(request.getCategoryId());
        draft.setUpdatedAt(LocalDateTime.now());
        articleDraftRepository.update(draftId, draft);

        return findDraft(draftId);
    }

    public void deleteDraft(Long draftId, String authorizationHeader){
        User currentUser = currentUserService.getCurrentEnabledUser(authorizationHeader);
        ArticleDraft draft = findDraft(draftId);
        checkDraftPermission(draft, currentUser);
        articleDraftRepository.deleteById(draftId);
    }

    public Optional<ArticleDraft> getArticleDraft(Long articleId, String authorizationHeader){
        User currentUser = currentUserService.getCurrentEnabledUser(authorizationHeader);
        Article article = getArticle(articleId);
        checkArticlePermission(article, currentUser);
        return articleDraftRepository.findByArticleIdAndAuthorId(articleId, currentUser.getId());
    }

    public ArticleDraft saveArticleDraft(Long articleId, ArticleDraft request, String authorizationHeader){
        User currentUser = currentUserService.getCurrentEnabledUser(authorizationHeader);
        Article article = getArticle(articleId);
        checkArticlePermission(article, currentUser);
        validateDraftCategory(request.getCategoryId());

        Optional<ArticleDraft> existingDraft = articleDraftRepository.findByArticleIdAndAuthorId(
            articleId,
            currentUser.getId()
        );

        if(existingDraft.isPresent()){
            return updateDraft(existingDraft.get().getId(), request, authorizationHeader);
        }

        LocalDateTime now = LocalDateTime.now();
        request.setArticleId(articleId);
        request.setAuthorId(currentUser.getId());
        request.setTitle(normalizeDraftText(request.getTitle()));
        request.setContent(normalizeDraftText(request.getContent()));
        request.setCreatedAt(now);
        request.setUpdatedAt(now);
        return articleDraftRepository.save(request);
    }

    public void deleteArticleDraft(Long articleId, String authorizationHeader){
        User currentUser = currentUserService.getCurrentEnabledUser(authorizationHeader);
        Article article = getArticle(articleId);
        checkArticlePermission(article, currentUser);
        articleDraftRepository.deleteByArticleIdAndAuthorId(articleId, currentUser.getId());
    }

    public List<ArticleRevision> listRevisions(Long articleId, String authorizationHeader){
        User currentUser = currentUserService.getCurrentEnabledUser(authorizationHeader);
        Article article = getArticle(articleId);
        checkArticlePermission(article, currentUser);
        return articleRevisionRepository.findByArticleId(articleId);
    }

    public ArticleRevision getRevision(Long articleId, Long revisionId, String authorizationHeader){
        User currentUser = currentUserService.getCurrentEnabledUser(authorizationHeader);
        Article article = getArticle(articleId);
        checkArticlePermission(article, currentUser);

        return articleRevisionRepository.findByIdAndArticleId(revisionId, articleId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Article revision not found"));
    }

    @Transactional
    public Article restoreRevision(Long articleId, Long revisionId, String authorizationHeader){
        User currentUser = currentUserService.getCurrentEnabledUser(authorizationHeader);
        Article article = getArticle(articleId);
        checkArticlePermission(article, currentUser);
        ArticleRevision revision = articleRevisionRepository.findByIdAndArticleId(revisionId, articleId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Article revision not found"));

        if(revision.getCategoryId() == null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Revision category no longer exists");
        }

        categoryService.getCategory(revision.getCategoryId());
        article.setTitle(revision.getTitle());
        article.setContent(revision.getContent());
        article.setCategoryId(revision.getCategoryId());
        article.setUpdatedAt(LocalDateTime.now());
        articleRepository.update(articleId, article);

        Article restoredArticle = getArticle(articleId);
        articleRevisionRepository.saveSnapshot(restoredArticle, currentUser.getId(), restoredArticle.getUpdatedAt());
        return restoredArticle;
    }

    @Transactional
    public void deleteArticle(Long id, String authorizationHeader){
        User currentUser = currentUserService.getCurrentEnabledUser(authorizationHeader);
        Article existingArticle = getArticle(id);

        checkArticlePermission(existingArticle, currentUser);

        commentRepository.deleteByArticleId(id);
        articleRepository.deleteById(id);
    }

    private void checkArticlePermission(Article article, User currentUser){
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        boolean isAuthor = (article.getAuthorId() != null) && (article.getAuthorId().equals(currentUser.getId()));

        if(!isAdmin && !isAuthor){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No Permission to operate current article");
        }
    }

    private ArticleDraft findDraft(Long draftId){
        return articleDraftRepository.findById(draftId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Article draft not found"));
    }

    private void checkDraftPermission(ArticleDraft draft, User currentUser){
        if(!draft.getAuthorId().equals(currentUser.getId())){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No Permission to operate current draft");
        }
    }

    private void validateDraftCategory(Long categoryId){
        if(categoryId != null){
            categoryService.getCategory(categoryId);
        }
    }

    private String normalizeDraftText(String value){
        return value == null ? "" : value;
    }
}
