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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.qijx.blog.dto.PageResponse;
import com.qijx.blog.dto.SeriesArticlesRequest;
import com.qijx.blog.dto.SeriesChapterRequest;
import com.qijx.blog.dto.SeriesChapterOrderRequest;
import com.qijx.blog.dto.SeriesArticleChapterRequest;
import com.qijx.blog.dto.SeriesDirectoryResponse;
import com.qijx.blog.entity.Series;
import com.qijx.blog.entity.SeriesArticle;
import com.qijx.blog.entity.SeriesChapter;
import com.qijx.blog.service.SeriesService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/series")
public class SeriesController {
    private final SeriesService seriesService;

    public SeriesController(SeriesService seriesService){
        this.seriesService = seriesService;
    }

    @GetMapping
    public PageResponse<Series> listSeries(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) Long sectionId,
        @RequestParam(required = false) String sectionSlug,
        @RequestParam(required = false) String keyword
    ){
        return seriesService.listSeries(page, size, sectionId, sectionSlug, keyword);
    }

    @GetMapping("/{id}")
    public Series getSeries(@PathVariable Long id){
        return seriesService.getSeriesWithArticles(id);
    }

    @GetMapping("/{id}/directory")
    public SeriesDirectoryResponse getDirectory(@PathVariable Long id){
        return seriesService.getDirectory(id);
    }

    @PostMapping("/{id}/chapters")
    public SeriesChapter createChapter(
        @PathVariable Long id,
        @Valid @RequestBody SeriesChapterRequest request,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ){
        return seriesService.createChapter(id, request, authorizationHeader);
    }

    @PutMapping("/{id}/chapters/{chapterId}")
    public SeriesChapter updateChapter(
        @PathVariable Long id,
        @PathVariable Long chapterId,
        @Valid @RequestBody SeriesChapterRequest request,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ){
        return seriesService.updateChapter(id, chapterId, request, authorizationHeader);
    }

    @DeleteMapping("/{id}/chapters/{chapterId}")
    public void deleteChapter(
        @PathVariable Long id,
        @PathVariable Long chapterId,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ){
        seriesService.deleteChapter(id, chapterId, authorizationHeader);
    }

    @PutMapping("/{id}/chapters/order")
    public List<SeriesChapter> reorderChapters(
        @PathVariable Long id,
        @Valid @RequestBody SeriesChapterOrderRequest request,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ){
        return seriesService.reorderChapters(id, request, authorizationHeader);
    }

    @PutMapping("/{id}/articles/{articleId}/chapter")
    public SeriesDirectoryResponse moveArticleToChapter(
        @PathVariable Long id,
        @PathVariable Long articleId,
        @Valid @RequestBody SeriesArticleChapterRequest request,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ){
        return seriesService.moveArticleToChapter(id, articleId, request.chapterId(), authorizationHeader);
    }

    @PostMapping
    public Series createSeries(
        @Valid @RequestBody Series series,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ){
        return seriesService.createSeries(series, authorizationHeader);
    }

    @PutMapping("/{id}")
    public Series updateSeries(
        @PathVariable Long id,
        @Valid @RequestBody Series series,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ){
        return seriesService.updateSeries(id, series, authorizationHeader);
    }

    @DeleteMapping("/{id}")
    public void deleteSeries(
        @PathVariable Long id,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ){
        seriesService.deleteSeries(id, authorizationHeader);
    }

    @GetMapping("/{id}/articles")
    public List<SeriesArticle> listArticles(@PathVariable Long id){
        return seriesService.listArticles(id);
    }

    @PostMapping("/{id}/articles/{articleId}")
    public List<SeriesArticle> addArticle(
        @PathVariable Long id,
        @PathVariable Long articleId,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ){
        return seriesService.addArticle(id, articleId, authorizationHeader);
    }

    @DeleteMapping("/{id}/articles/{articleId}")
    public void removeArticle(
        @PathVariable Long id,
        @PathVariable Long articleId,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ){
        seriesService.removeArticle(id, articleId, authorizationHeader);
    }

    @PutMapping("/{id}/articles/order")
    public List<SeriesArticle> reorderArticles(
        @PathVariable Long id,
        @Valid @RequestBody SeriesArticlesRequest request,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ){
        return seriesService.reorderArticles(id, request.articleIds(), authorizationHeader);
    }
}
