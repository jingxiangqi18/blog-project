package com.qijx.blog.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.web.server.ResponseStatusException;

import com.qijx.blog.dto.PageResponse;
import com.qijx.blog.dto.SeriesChapterRequest;
import com.qijx.blog.dto.SeriesChapterOrderRequest;
import com.qijx.blog.dto.SeriesDirectoryResponse;
import com.qijx.blog.entity.Article;
import com.qijx.blog.entity.Category;
import com.qijx.blog.entity.Role;
import com.qijx.blog.entity.Series;
import com.qijx.blog.entity.SeriesArticle;
import com.qijx.blog.entity.SeriesChapter;
import com.qijx.blog.entity.User;
import com.qijx.blog.repository.ArticleRepository;
import com.qijx.blog.repository.SeriesRepository;

@Service
public class SeriesService {
    private static final int MAX_CHAPTER_DEPTH = 8;
    private static final int MAX_CHAPTERS = 500;
    private final SeriesRepository seriesRepository;
    private final ArticleRepository articleRepository;
    private final CategoryService categoryService;
    private final SectionService sectionService;
    private final CurrentUserService currentUserService;

    public SeriesService(SeriesRepository seriesRepository, ArticleRepository articleRepository,
        CategoryService categoryService, SectionService sectionService, CurrentUserService currentUserService){
        this.seriesRepository = seriesRepository;
        this.articleRepository = articleRepository;
        this.categoryService = categoryService;
        this.sectionService = sectionService;
        this.currentUserService = currentUserService;
    }

    public PageResponse<Series> listSeries(int page, int size, Long sectionId, String sectionSlug, String keyword){
        return seriesRepository.findPage(Math.max(1, page), size < 1 ? 10 : Math.min(size, 50),
            sectionService.resolveSectionId(sectionId, sectionSlug), keyword == null ? "" : keyword.trim());
    }

    public Series getSeries(Long id){
        return seriesRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Series not found"));
    }

    public Series getSeriesWithArticles(Long id){
        Series series = getSeries(id);
        series.setArticles(listArticles(id));
        return series;
    }

    @Transactional(readOnly = true)
    public SeriesDirectoryResponse getDirectory(Long id){
        Series series = getSeries(id);
        return new SeriesDirectoryResponse(series.getId(), series.getSlug(), series.getTitle(), series.getDescription(),
            series.getSectionId(), series.getSectionSlug(), series.getSectionName(), series.getCategoryId(),
            seriesRepository.findChapters(id), seriesRepository.findDirectoryArticles(id));
    }

    public List<Series> listSeriesForArticle(Long articleId){
        if(!articleRepository.existsById(articleId)){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found");
        }
        return seriesRepository.findByArticleId(articleId);
    }

    // Re-read committed chapter state after waiting for another writer's series lock.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public SeriesChapter createChapter(Long id, SeriesChapterRequest request, String authorizationHeader){
        User user = currentUserService.getCurrentEnabledUser(authorizationHeader);
        Series series = lockSeries(id);
        checkPermission(series, user);
        List<SeriesChapter> chapters = seriesRepository.findChapters(id);
        if(chapters.size() >= MAX_CHAPTERS){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A series supports at most 500 chapter groups");
        }
        validateChapterTree(chapters, null, request.parentId());
        SeriesChapter chapter = seriesRepository.saveChapter(id, request.parentId(), request.title().trim());
        seriesRepository.touch(id);
        return chapter;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public SeriesChapter updateChapter(Long id, Long chapterId, SeriesChapterRequest request, String authorizationHeader){
        User user = currentUserService.getCurrentEnabledUser(authorizationHeader);
        Series series = lockSeries(id);
        checkPermission(series, user);
        SeriesChapter chapter = getChapter(id, chapterId);
        validateChapterTree(seriesRepository.findChapters(id), chapterId, request.parentId());
        int order = Objects.equals(chapter.parentId(), request.parentId())
            ? chapter.sortOrder() : seriesRepository.nextChapterOrder(id, request.parentId());
        seriesRepository.updateChapter(id, chapterId, request.parentId(), request.title().trim(), order);
        seriesRepository.touch(id);
        return getChapter(id, chapterId);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void deleteChapter(Long id, Long chapterId, String authorizationHeader){
        User user = currentUserService.getCurrentEnabledUser(authorizationHeader);
        Series series = lockSeries(id);
        checkPermission(series, user);
        getChapter(id, chapterId);
        boolean hasChildren = seriesRepository.findChapters(id).stream()
            .anyMatch(chapter -> Objects.equals(chapter.parentId(), chapterId));
        if(hasChildren){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Move or remove child chapters before deleting this chapter");
        }
        seriesRepository.deleteChapter(id, chapterId);
        seriesRepository.touch(id);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<SeriesChapter> reorderChapters(Long id, SeriesChapterOrderRequest request, String authorizationHeader){
        User user = currentUserService.getCurrentEnabledUser(authorizationHeader);
        Series series = lockSeries(id);
        checkPermission(series, user);
        if(request.parentId() != null){
            getChapter(id, request.parentId());
        }
        var siblings = new HashSet<Long>();
        for(SeriesChapter chapter : seriesRepository.findChapters(id)){
            if(Objects.equals(chapter.parentId(), request.parentId())){
                siblings.add(chapter.id());
            }
        }
        if(request.chapterIds().size() != new HashSet<>(request.chapterIds()).size()
            || !siblings.equals(new HashSet<>(request.chapterIds()))){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "chapterIds must contain every sibling chapter exactly once");
        }
        for(int index = 0; index < request.chapterIds().size(); index++){
            seriesRepository.updateChapterOrder(id, request.chapterIds().get(index), index + 1);
        }
        seriesRepository.touch(id);
        return seriesRepository.findChapters(id);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public SeriesDirectoryResponse moveArticleToChapter(Long id, Long articleId, Long chapterId, String authorizationHeader){
        User user = currentUserService.getCurrentEnabledUser(authorizationHeader);
        Series series = lockSeries(id);
        checkPermission(series, user);
        if(chapterId != null){
            getChapter(id, chapterId);
        }
        if(!seriesRepository.findArticleOrders(id).containsKey(articleId)){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Article is not part of this series");
        }
        seriesRepository.updateArticleChapter(id, articleId, chapterId);
        seriesRepository.touch(id);
        return getDirectory(id);
    }

    @Transactional
    public Series createSeries(Series series, String authorizationHeader){
        User user = currentUserService.getCurrentEnabledUser(authorizationHeader);
        sectionService.validateAssignment(series.getSectionId(), null);
        validateCategory(series);
        LocalDateTime now = LocalDateTime.now();
        series.setCreatedBy(user.getId());
        series.setCreatedAt(now);
        series.setUpdatedAt(now);
        series.setTitle(series.getTitle().trim());
        try{
            seriesRepository.save(series);
        }catch(DuplicateKeyException exception){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Series slug already exists");
        }
        return getSeriesWithArticles(series.getId());
    }

    @Transactional
    public Series updateSeries(Long id, Series request, String authorizationHeader){
        User user = currentUserService.getCurrentEnabledUser(authorizationHeader);
        Series series = lockSeries(id);
        checkPermission(series, user);
        sectionService.validateAssignment(request.getSectionId(), series.getSectionId());
        validateCategory(request);
        for(Article article : articleRepository.findBySeriesId(id)){
            validateArticleSection(request.getSectionId(), article);
        }
        request.setUpdatedAt(LocalDateTime.now());
        request.setTitle(request.getTitle().trim());
        try{
            seriesRepository.update(id, request);
        }catch(DuplicateKeyException exception){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Series slug already exists");
        }
        return getSeriesWithArticles(id);
    }

    @Transactional
    public void deleteSeries(Long id, String authorizationHeader){
        User user = currentUserService.getCurrentEnabledUser(authorizationHeader);
        Series series = lockSeries(id);
        checkPermission(series, user);
        seriesRepository.deleteById(id);
    }

    public List<SeriesArticle> listArticles(Long id){
        getSeries(id);
        return articleRepository.findSeriesArticles(id);
    }

    @Transactional
    public List<SeriesArticle> addArticle(Long id, Long articleId, String authorizationHeader){
        User user = currentUserService.getCurrentEnabledUser(authorizationHeader);
        Series series = lockSeries(id);
        checkPermission(series, user);
        sectionService.validateAssignment(series.getSectionId(), null);
        Article article = articleRepository.findByIdForUpdate(articleId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Article does not exist"));
        validateArticleSection(series.getSectionId(), article);
        if(!seriesRepository.findArticleOrders(id).containsKey(articleId)){
            seriesRepository.addArticle(id, articleId);
            seriesRepository.touch(id);
        }
        return listArticles(id);
    }

    @Transactional
    public void removeArticle(Long id, Long articleId, String authorizationHeader){
        User user = currentUserService.getCurrentEnabledUser(authorizationHeader);
        Series series = lockSeries(id);
        checkPermission(series, user);
        seriesRepository.removeArticle(id, articleId);
        seriesRepository.touch(id);
    }

    @Transactional
    public List<SeriesArticle> reorderArticles(Long id, List<Long> articleIds, String authorizationHeader){
        User user = currentUserService.getCurrentEnabledUser(authorizationHeader);
        Series series = lockSeries(id);
        checkPermission(series, user);
        Map<Long, Integer> existing = seriesRepository.findArticleOrders(id);
        if(articleIds == null || articleIds.size() != new HashSet<>(articleIds).size()
            || !existing.keySet().equals(new HashSet<>(articleIds))){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "articleIds must contain every series article exactly once");
        }
        // Validate the complete batch before writing any chapter positions.
        for(Article article : articleRepository.findBySeriesId(id)){
            validateArticleSection(series.getSectionId(), article);
        }
        for(int index = 0; index < articleIds.size(); index++){
            seriesRepository.updateOrder(id, articleIds.get(index), index + 1);
        }
        seriesRepository.touch(id);
        return listArticles(id);
    }

    private Series lockSeries(Long id){
        return seriesRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Series not found"));
    }

    private SeriesChapter getChapter(Long seriesId, Long chapterId){
        return seriesRepository.findChapter(seriesId, chapterId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found in this series"));
    }

    private void validateChapterTree(List<SeriesChapter> chapters, Long chapterId, Long parentId){
        Map<Long, Long> parents = new HashMap<>();
        for(SeriesChapter chapter : chapters){
            parents.put(chapter.id(), chapter.parentId());
        }
        if(parentId != null && !parents.containsKey(parentId)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parent chapter must belong to this series");
        }
        // Check the proposed parent map, including descendants affected by a move.
        parents.put(chapterId == null ? -1L : chapterId, parentId);
        for(Long id : parents.keySet()){
            var visited = new HashSet<Long>();
            for(Long current = id; current != null; current = parents.get(current)){
                if(!visited.add(current)){
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chapter hierarchy cannot contain a cycle");
                }
                if(visited.size() > MAX_CHAPTER_DEPTH){
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chapter hierarchy supports at most 8 levels");
                }
            }
        }
    }

    private void validateCategory(Series series){
        if(series.getCategoryId() != null){
            Category category = categoryService.getCategory(series.getCategoryId());
            if(category.getSectionId() != null && !Objects.equals(category.getSectionId(), series.getSectionId())){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Series and category must belong to the same section");
            }
        }
    }

    private void validateArticleSection(Long sectionId, Article article){
        if(article.getSectionId() != null && !Objects.equals(article.getSectionId(), sectionId)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Series and article must belong to the same section");
        }
    }

    private void checkPermission(Series series, User user){
        if(user.getRole() != Role.ADMIN && !Objects.equals(series.getCreatedBy(), user.getId())){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No permission to operate this series");
        }
    }
}
