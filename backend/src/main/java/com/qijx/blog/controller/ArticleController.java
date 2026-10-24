package com.qijx.blog.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PathVariable;

import com.qijx.blog.dto.PageResponse;
import com.qijx.blog.dto.InteractionStatusResponse;
import com.qijx.blog.entity.Article;
import com.qijx.blog.entity.ArticleDraft;
import com.qijx.blog.entity.ArticleRevision;
import com.qijx.blog.entity.Series;
import com.qijx.blog.service.ArticleService;
import com.qijx.blog.service.SeriesService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/articles")

public class ArticleController {
    
    private final ArticleService articleService;
    private final SeriesService seriesService;

    public ArticleController(ArticleService articleService, SeriesService seriesService){
        this.articleService = articleService;
        this.seriesService = seriesService;
    }

    @PostMapping
    public Article createArticle(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @Valid @RequestBody Article article
    ){
        return articleService.createArticle(article, authorizationHeader);
    }

    @GetMapping
    public PageResponse<Article> listArticles(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) String sectionSlug,
        @RequestParam(required = false) String contentType,
        @RequestParam(required = false) String tagSlug,
        @RequestParam(required = false) Long seriesId
    ){
        return articleService.listArticles(page, size, keyword, categoryId, sectionSlug, contentType, tagSlug, seriesId);
    }

    @GetMapping("/{id}")
    public Article getArticle(@PathVariable Long id){
        return articleService.getArticle(id);
    }

    @GetMapping("/{id}/series")
    public List<Series> listArticleSeries(@PathVariable Long id){
        return seriesService.listSeriesForArticle(id);
    }

    @GetMapping("/{id}/likes")
    public InteractionStatusResponse getLikeStatus(
        @PathVariable Long id,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ){
        return articleService.getLikeStatus(id, authorizationHeader);
    }

    @PostMapping("/{id}/likes")
    public InteractionStatusResponse likeArticle(
        @PathVariable Long id,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ){
        return articleService.likeArticle(id, authorizationHeader);
    }

    @DeleteMapping("/{id}/likes")
    public InteractionStatusResponse unlikeArticle(
        @PathVariable Long id,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ){
        return articleService.unlikeArticle(id, authorizationHeader);
    }

    @GetMapping("/{id}/favorites")
    public InteractionStatusResponse getFavoriteStatus(
        @PathVariable Long id,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ){
        return articleService.getFavoriteStatus(id, authorizationHeader);
    }

    @PostMapping("/{id}/favorites")
    public InteractionStatusResponse favoriteArticle(
        @PathVariable Long id,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ){
        return articleService.favoriteArticle(id, authorizationHeader);
    }

    @DeleteMapping("/{id}/favorites")
    public InteractionStatusResponse unfavoriteArticle(
        @PathVariable Long id,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ){
        return articleService.unfavoriteArticle(id, authorizationHeader);
    }

    @PostMapping("/drafts")
    public ArticleDraft createDraft(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @Valid @RequestBody ArticleDraft draft
    ){
        return articleService.createDraft(draft, authorizationHeader);
    }

    @GetMapping("/drafts/{draftId}")
    public ArticleDraft getDraft(
        @PathVariable Long draftId,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ){
        return articleService.getDraft(draftId, authorizationHeader);
    }

    @PutMapping("/drafts/{draftId}")
    public ArticleDraft updateDraft(
        @PathVariable Long draftId,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @Valid @RequestBody ArticleDraft draft
    ){
        return articleService.updateDraft(draftId, draft, authorizationHeader);
    }

    @DeleteMapping("/drafts/{draftId}")
    public void deleteDraft(
        @PathVariable Long draftId,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ){
        articleService.deleteDraft(draftId, authorizationHeader);
    }

    @GetMapping("/{id}/draft")
    public ResponseEntity<ArticleDraft> getArticleDraft(
        @PathVariable Long id,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ){
        Optional<ArticleDraft> draft = articleService.getArticleDraft(id, authorizationHeader);
        return draft.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping("/{id}/draft")
    public ArticleDraft saveArticleDraft(
        @PathVariable Long id,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @Valid @RequestBody ArticleDraft draft
    ){
        return articleService.saveArticleDraft(id, draft, authorizationHeader);
    }

    @DeleteMapping("/{id}/draft")
    public void deleteArticleDraft(
        @PathVariable Long id,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ){
        articleService.deleteArticleDraft(id, authorizationHeader);
    }

    @GetMapping("/{id}/revisions")
    public List<ArticleRevision> listRevisions(
        @PathVariable Long id,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ){
        return articleService.listRevisions(id, authorizationHeader);
    }

    @GetMapping("/{id}/revisions/{revisionId}")
    public ArticleRevision getRevision(
        @PathVariable Long id,
        @PathVariable Long revisionId,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ){
        return articleService.getRevision(id, revisionId, authorizationHeader);
    }

    @PostMapping("/{id}/revisions/{revisionId}/restore")
    public Article restoreRevision(
        @PathVariable Long id,
        @PathVariable Long revisionId,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ){
        return articleService.restoreRevision(id, revisionId, authorizationHeader);
    }

    @PutMapping("/{id}")
    public Article updateArticle(
        @PathVariable Long id,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @Valid @RequestBody Article article
    ){
        return articleService.updateArticle(id, article, authorizationHeader);
    }

    @DeleteMapping("/{id}")
    public void deleteArticle(
        @PathVariable Long id,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
        ){
        articleService.deleteArticle(id, authorizationHeader);
    }
}
