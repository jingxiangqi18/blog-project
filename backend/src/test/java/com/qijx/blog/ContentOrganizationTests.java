package com.qijx.blog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.FileSystemResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.qijx.blog.entity.Article;
import com.qijx.blog.entity.User;
import com.qijx.blog.dto.SeriesChapterOrderRequest;
import com.qijx.blog.repository.UserRepository;
import com.qijx.blog.service.ArticleService;
import com.qijx.blog.service.JwtService;
import com.qijx.blog.service.SeriesService;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContentOrganizationTests {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;
    @Autowired ObjectMapper mapper;
    @Autowired JwtService jwtService;
    @Autowired UserRepository userRepository;
    @Autowired ArticleService articleService;
    @Autowired SeriesService seriesService;

    private String ownerToken;
    private String otherToken;
    private String adminToken;
    private long technologyId;
    private long lifeId;
    private long categoryId;

    @BeforeEach
    void setUp(){
        jdbc.execute("DROP ALL OBJECTS");
        new ResourceDatabasePopulator(new FileSystemResource("database/schema.sql")).execute(dataSource);
        insertUser("Owner", "USER", true);
        insertUser("Other", "USER", true);
        insertUser("Admin", "ADMIN", true);
        ownerToken = token("Owner");
        otherToken = token("Other");
        adminToken = token("Admin");
        technologyId = sectionId("technology");
        lifeId = sectionId("life");
        jdbc.update("INSERT INTO categories(name) VALUES(?)", "Global category");
        categoryId = jdbc.queryForObject("SELECT id FROM categories WHERE name = ?", Long.class, "Global category");
    }

    @Test
    void legacyArticleRequestsAndPublicReadsRemainCompatible() throws Exception{
        JsonNode article = createArticle(Map.of());
        long id = article.get("id").asLong();
        assertThat(article.get("sectionId").isNull()).isTrue();
        assertThat(article.get("summary").isNull()).isTrue();
        assertThat(article.get("contentType").asText()).isEqualTo("general");
        assertThat(article.get("tags").size()).isZero();
        mvc.perform(get("/api/articles/{id}", id))
            .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Article"))
            .andExpect(jsonPath("$.authorName").value("Owner"))
            .andExpect(jsonPath("$.sectionIdProvided").doesNotExist());
        mvc.perform(get("/api/articles"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void legacyUpdatesPreserveNewMetadataAndTags() throws Exception{
        long tag = createTag("jvm");
        long id = createArticle(Map.of("sectionId", technologyId, "summary", "Custom summary",
            "contentType", "interview_notes", "tagIds", List.of(tag))).get("id").asLong();
        updateArticle(id, Map.of()).andExpect(status().isOk())
            .andExpect(jsonPath("$.sectionId").value(technologyId))
            .andExpect(jsonPath("$.summary").value("Custom summary"))
            .andExpect(jsonPath("$.contentType").value("interview_notes"))
            .andExpect(jsonPath("$.tagIds[0]").value(tag));
    }

    @Test
    void sectionAndContentTypeFiltersExcludeUnassignedArticles() throws Exception{
        createArticle(Map.of());
        createArticle(Map.of("sectionId", technologyId, "contentType", "interview_notes"));
        createArticle(Map.of("sectionId", lifeId, "contentType", "personal_journal"));
        mvc.perform(get("/api/articles")).andExpect(jsonPath("$.totalElements").value(3));
        mvc.perform(get("/api/articles").param("sectionSlug", "technology").param("contentType", "interview_notes"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].sectionSlug").value("technology"));
        mvc.perform(get("/api/articles").param("sectionSlug", "unknown"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void invalidDisabledAndCrossSectionAssignmentsAreRejected() throws Exception{
        postJson("/api/articles", ownerToken, articleBody(Map.of("sectionId", 9999))).andExpect(status().isBadRequest());
        long techCategory = createCategory("Java", technologyId);
        postJson("/api/articles", ownerToken, articleBody(Map.of("sectionId", lifeId, "categoryId", techCategory)))
            .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/sections/{id}/enabled", technologyId).header("Authorization", adminToken)
            .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
            .andExpect(status().isOk());
        postJson("/api/articles", ownerToken, articleBody(Map.of("sectionId", technologyId)))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/sections/technology")).andExpect(status().isNotFound());
        mvc.perform(get("/api/sections")).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/sections/admin/all").header("Authorization", adminToken))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void disablingASectionDoesNotPreventEditingExistingArticles() throws Exception{
        long id = createArticle(Map.of("sectionId", technologyId)).get("id").asLong();
        jdbc.update("UPDATE sections SET enabled = FALSE WHERE id = ?", technologyId);
        updateArticle(id, Map.of()).andExpect(status().isOk()).andExpect(jsonPath("$.sectionId").value(technologyId));
    }

    @Test
    void categoryFilteringIncludesGlobalCategoriesAndRejectsConflictingMoves() throws Exception{
        long tech = createCategory("Java", technologyId);
        createCategory("Reading", lifeId);
        mvc.perform(get("/api/categories").param("sectionSlug", "technology"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/categories").param("sectionId", Long.toString(lifeId)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        createArticle(Map.of("categoryId", tech, "sectionId", technologyId));
        mvc.perform(put("/api/categories/{id}", tech).header("Authorization", adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(Map.of("name", "Java", "sectionId", lifeId))))
            .andExpect(status().isBadRequest());
        // An old category update must not clear its existing section.
        mvc.perform(put("/api/categories/{id}", tech).header("Authorization", adminToken)
            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Java Backend\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.sectionId").value(technologyId));
    }

    @Test
    void tagsAreDeduplicatedAndCanBeAddedRemovedOrCleared() throws Exception{
        long jvm = createTag("jvm");
        long java = createTag("java");
        long id = createArticle(Map.of("tagIds", List.of(jvm, jvm))).get("id").asLong();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM article_tags WHERE article_id = ?", Long.class, id)).isEqualTo(1);
        updateArticle(id, Map.of("tagIds", List.of(jvm, java))).andExpect(status().isOk())
            .andExpect(jsonPath("$.tags.length()").value(2));
        updateArticle(id, Map.of("tagIds", List.of(java))).andExpect(status().isOk())
            .andExpect(jsonPath("$.tagIds.length()").value(1)).andExpect(jsonPath("$.tagIds[0]").value(java));
        updateArticle(id, Map.of("tagIds", List.of())).andExpect(status().isOk())
            .andExpect(jsonPath("$.tags.length()").value(0));
    }

    @Test
    void invalidTagUpdatesLeaveArticleAndAssociationsUntouched() throws Exception{
        long tag = createTag("jvm");
        long id = createArticle(Map.of("tagIds", List.of(tag))).get("id").asLong();
        updateArticle(id, Map.of("title", "Should not be saved", "tagIds", List.of(9999)))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/articles/{id}", id)).andExpect(jsonPath("$.title").value("Article"))
            .andExpect(jsonPath("$.tagIds[0]").value(tag));
        var nullTags = new java.util.HashMap<String, Object>();
        nullTags.put("tagIds", null);
        updateArticle(id, nullTags).andExpect(status().isBadRequest());
    }

    @Test
    void actualDatabaseFailureRollsBackArticleAndTagChanges() throws Exception{
        long originalTag = createTag("original");
        long newTag = createTag("replacement");
        long id = createArticle(Map.of("tagIds", List.of(originalTag))).get("id").asLong();
        jdbc.execute("ALTER TABLE article_tags ADD CONSTRAINT reject_test_tag CHECK(tag_id <> " + newTag + ")");
        Article request = new Article();
        request.setTitle("Must roll back");
        request.setContent("New content");
        request.setCategoryId(categoryId);
        request.setTagIds(List.of(newTag));
        assertThrows(DataIntegrityViolationException.class, () -> articleService.updateArticle(id, request, ownerToken));
        assertThat(articleService.getArticle(id).getTitle()).isEqualTo("Article");
        assertThat(articleService.getArticle(id).getTagIds()).containsExactly(originalTag);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM article_revisions WHERE article_id = ?", Long.class, id)).isEqualTo(1);
    }

    @Test
    void manyToManyFiltersDoNotDuplicateRowsOrInflatePagination() throws Exception{
        long tag1 = createTag("jvm");
        long tag2 = createTag("concurrency");
        long article1 = createArticle(Map.of("sectionId", technologyId, "contentType", "interview_notes", "tagIds", List.of(tag1, tag2))).get("id").asLong();
        long article2 = createArticle(Map.of("sectionId", technologyId, "contentType", "interview_notes", "tagIds", List.of(tag1, tag2))).get("id").asLong();
        long series1 = createSeries("java-path", technologyId);
        long series2 = createSeries("interview-path", technologyId);
        addArticle(series1, article1);
        addArticle(series1, article2);
        addArticle(series2, article1);
        mvc.perform(get("/api/articles").param("sectionSlug", "technology").param("contentType", "interview_notes")
            .param("tagSlug", "jvm").param("seriesId", Long.toString(series1)).param("size", "1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(2)).andExpect(jsonPath("$.content.length()").value(1));
        long first = response(mvc.perform(get("/api/articles").param("tagSlug", "jvm").param("size", "1")))
            .get("content").get(0).get("id").asLong();
        long second = response(mvc.perform(get("/api/articles").param("tagSlug", "jvm").param("size", "1").param("page", "2")))
            .get("content").get(0).get("id").asLong();
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void seriesArticlesCanBeAddedReorderedRemovedAndDeletedSafely() throws Exception{
        long article1 = createArticle(Map.of("sectionId", technologyId)).get("id").asLong();
        long legacy = createArticle(Map.of()).get("id").asLong();
        long series = createSeries("java-path", technologyId);
        addArticle(series, article1);
        addArticle(series, legacy);
        addArticle(series, article1);
        reorder(series, List.of(legacy, article1)).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].article.id").value(legacy)).andExpect(jsonPath("$[0].sortOrder").value(1));
        mvc.perform(get("/api/series/{id}", series)).andExpect(jsonPath("$.articles[1].article.id").value(article1));
        mvc.perform(get("/api/articles/{id}", legacy)).andExpect(jsonPath("$.sectionId").isEmpty());
        mvc.perform(delete("/api/series/{id}/articles/{articleId}", series, article1).header("Authorization", ownerToken))
            .andExpect(status().isOk());
        mvc.perform(delete("/api/series/{id}", series).header("Authorization", ownerToken)).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM articles", Long.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM series_articles", Long.class)).isZero();
    }

    @Test
    void invalidOrderBatchesAreRejectedWithoutChangingOrder() throws Exception{
        long first = createArticle(Map.of()).get("id").asLong();
        long second = createArticle(Map.of()).get("id").asLong();
        long series = createSeries("path", technologyId);
        addArticle(series, first);
        addArticle(series, second);
        reorder(series, List.of(first, first)).andExpect(status().isBadRequest());
        reorder(series, List.of(first, 9999L)).andExpect(status().isBadRequest());
        reorder(series, List.of(first)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/series/{id}/articles", series)).andExpect(jsonPath("$[0].article.id").value(first));
    }

    @Test
    void orderTransactionRollsBackWhenALaterChapterWriteFails() throws Exception{
        long first = createArticle(Map.of()).get("id").asLong();
        long second = createArticle(Map.of()).get("id").asLong();
        long series = createSeries("path", technologyId);
        addArticle(series, first);
        addArticle(series, second);
        jdbc.execute("ALTER TABLE series_articles ADD CONSTRAINT reject_test_position CHECK(NOT(article_id = " + first + " AND sort_order = 2))");
        assertThrows(DataIntegrityViolationException.class,
            () -> seriesService.reorderArticles(series, List.of(second, first), ownerToken));
        mvc.perform(get("/api/series/{id}/articles", series)).andExpect(jsonPath("$[0].article.id").value(first))
            .andExpect(jsonPath("$[0].sortOrder").value(1)).andExpect(jsonPath("$[1].sortOrder").value(2));
    }

    @Test
    void seriesSectionConsistencyIsCheckedOnBothSides() throws Exception{
        long life = createArticle(Map.of("sectionId", lifeId)).get("id").asLong();
        long legacy = createArticle(Map.of()).get("id").asLong();
        long series = createSeries("path", technologyId);
        mvc.perform(post("/api/series/{id}/articles/{articleId}", series, life).header("Authorization", ownerToken))
            .andExpect(status().isBadRequest());
        addArticle(series, legacy);
        updateArticle(legacy, Map.of("sectionId", lifeId)).andExpect(status().isBadRequest());
        updateArticle(legacy, Map.of("sectionId", technologyId)).andExpect(status().isOk());
        mvc.perform(put("/api/series/{id}", series).header("Authorization", ownerToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(Map.of("slug", "path", "title", "Path", "sectionId", lifeId))))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/series/{id}/articles/9999", series).header("Authorization", ownerToken))
            .andExpect(status().isBadRequest());
    }

    @Test
    void permissionsUseAuthenticatedUserAndRejectDisabledUsers() throws Exception{
        long article = createArticle(Map.of()).get("id").asLong();
        long series = createSeries("path", technologyId);
        mvc.perform(put("/api/articles/{id}", article).header("Authorization", otherToken)
            .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(articleBody(Map.of("authorId", 2)))))
            .andExpect(status().isForbidden());
        mvc.perform(delete("/api/series/{id}", series).header("Authorization", otherToken)).andExpect(status().isForbidden());
        postJson("/api/tags", ownerToken, Map.of("slug", "bad", "name", "Bad")).andExpect(status().isForbidden());
        postJson("/api/sections", ownerToken, Map.of("slug", "reading", "name", "Reading")).andExpect(status().isForbidden());
        postJson("/api/series", null, Map.of("slug", "bad", "title", "Bad", "sectionId", technologyId))
            .andExpect(status().isUnauthorized());
        jdbc.update("UPDATE users SET enabled = FALSE WHERE username = ?", "Owner");
        mvc.perform(delete("/api/series/{id}", series).header("Authorization", ownerToken)).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/series/{id}", series).header("Authorization", adminToken)).andExpect(status().isOk());
    }

    @Test
    void duplicateSlugsAndInvalidIdentifiersReturnClientErrors() throws Exception{
        createTag("jvm");
        postJson("/api/tags", adminToken, Map.of("slug", "jvm", "name", "Another JVM")).andExpect(status().isConflict());
        postJson("/api/sections", adminToken, Map.of("slug", "technology", "name", "Tech")).andExpect(status().isConflict());
        createSeries("path", technologyId);
        postJson("/api/series", ownerToken, Map.of("slug", "path", "title", "Duplicate", "sectionId", technologyId))
            .andExpect(status().isConflict());
        postJson("/api/articles", ownerToken, articleBody(Map.of("contentType", "Invalid Type"))).andExpect(status().isBadRequest());
        postJson("/api/articles", ownerToken, articleBody(Map.of("contentType", ""))).andExpect(status().isBadRequest());
        postJson("/api/articles", ownerToken, articleBody(Map.of("summary", "x".repeat(501)))).andExpect(status().isBadRequest());
        postJson("/api/tags", adminToken, Map.of("slug", "../invalid", "name", "Bad")).andExpect(status().isBadRequest());
    }

    @Test
    void sectionsCanBeAddedAndOrderedWithoutCodeChanges() throws Exception{
        postJson("/api/sections", adminToken, Map.of("slug", "reading", "name", "Reading", "sortOrder", -1))
            .andExpect(status().isOk());
        mvc.perform(get("/api/sections")).andExpect(jsonPath("$[0].slug").value("reading"));
        long tag = createTag("jvm");
        mvc.perform(get("/api/tags").param("keyword", "jvm")).andExpect(jsonPath("$[0].id").value(tag));
        mvc.perform(get("/api/tags/slug/jvm")).andExpect(status().isOk());
        mvc.perform(get("/api/series").param("sectionSlug", "technology").param("keyword", "absent"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void revisionsStillRestoreBodyWithoutDiscardingOrganization() throws Exception{
        long tag = createTag("java");
        long id = createArticle(Map.of("sectionId", technologyId, "summary", "Summary", "tagIds", List.of(tag))).get("id").asLong();
        updateArticle(id, Map.of("title", "Edited")).andExpect(status().isOk());
        long revision = jdbc.queryForObject("SELECT id FROM article_revisions WHERE article_id = ? AND revision_number = 1", Long.class, id);
        mvc.perform(post("/api/articles/{id}/revisions/{revision}/restore", id, revision).header("Authorization", ownerToken))
            .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Article"))
            .andExpect(jsonPath("$.sectionId").value(technologyId)).andExpect(jsonPath("$.summary").value("Summary"))
            .andExpect(jsonPath("$.tagIds[0]").value(tag));
    }

    @Test
    void tagsAndSeriesCanBeEditedAndTagDeletionOnlyRemovesLinks() throws Exception{
        long tag = createTag("jvm");
        long article = createArticle(Map.of("sectionId", technologyId, "tagIds", List.of(tag))).get("id").asLong();
        long series = createSeries("learning", technologyId);
        addArticle(series, article);
        mvc.perform(put("/api/tags/{id}", tag).header("Authorization", adminToken)
            .contentType(MediaType.APPLICATION_JSON).content("{\"slug\":\"java-memory\",\"name\":\"Java Memory\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.slug").value("java-memory"));
        mvc.perform(put("/api/series/{id}", series).header("Authorization", ownerToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(Map.of("slug", "jvm-path", "title", "JVM Path", "sectionId", technologyId))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("JVM Path"));
        mvc.perform(get("/api/series").param("sectionSlug", "technology").param("keyword", "JVM"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(delete("/api/tags/{id}", tag).header("Authorization", adminToken)).andExpect(status().isOk());
        mvc.perform(get("/api/articles/{id}", article)).andExpect(status().isOk()).andExpect(jsonPath("$.tags.length()").value(0));
        mvc.perform(get("/api/series/{id}/articles", series)).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void deletingAnArticleCleansItsNewLinksButKeepsSeriesAndTags() throws Exception{
        long tag = createTag("java");
        long article = createArticle(Map.of("tagIds", List.of(tag))).get("id").asLong();
        long series = createSeries("path", technologyId);
        addArticle(series, article);
        mvc.perform(delete("/api/articles/{id}", article).header("Authorization", ownerToken)).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM article_tags", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM series_articles", Long.class)).isZero();
        mvc.perform(get("/api/tags/{id}", tag)).andExpect(status().isOk());
        mvc.perform(get("/api/series/{id}", series)).andExpect(status().isOk());
    }

    @Test
    void legacyDraftOperationsDoNotChangePublishedOrganization() throws Exception{
        long article = createArticle(Map.of("sectionId", technologyId, "summary", "Published summary")).get("id").asLong();
        mvc.perform(put("/api/articles/{id}/draft", article).header("Authorization", ownerToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(articleBody(Map.of("title", "Draft only")))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Draft only"));
        mvc.perform(get("/api/articles/{id}/draft", article).header("Authorization", ownerToken))
            .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Draft only"));
        mvc.perform(get("/api/articles/{id}", article)).andExpect(jsonPath("$.title").value("Article"))
            .andExpect(jsonPath("$.summary").value("Published summary")).andExpect(jsonPath("$.sectionId").value(technologyId));
    }

    @Test
    void explicitNullClearsOptionalMetadataAndNullContentTypeIsRejected() throws Exception{
        long article = createArticle(Map.of("sectionId", technologyId, "summary", "Summary")).get("id").asLong();
        var clearFields = new java.util.HashMap<String, Object>();
        clearFields.put("sectionId", null);
        clearFields.put("summary", null);
        updateArticle(article, clearFields).andExpect(status().isOk())
            .andExpect(jsonPath("$.sectionId").isEmpty()).andExpect(jsonPath("$.summary").isEmpty());
        clearFields.put("contentType", null);
        updateArticle(article, clearFields).andExpect(status().isBadRequest());
    }

    @Test
    void readingDirectoryAndMembershipResponsesDoNotDownloadBodies() throws Exception{
        long article = createArticle(Map.of("content", "body".repeat(100000))).get("id").asLong();
        long series = createSeries("reading", technologyId);
        addArticle(series, article);
        var result = mvc.perform(get("/api/series/{id}/directory", series))
            .andExpect(status().isOk()).andExpect(jsonPath("$.articles[0].id").value(article))
            .andExpect(jsonPath("$.articles[0].content").doesNotExist())
            .andExpect(jsonPath("$.articles[0].tags").doesNotExist())
            .andExpect(jsonPath("$.chapters.length()").value(0));
        assertThat(result.andReturn().getResponse().getContentAsByteArray().length).isLessThan(4000);
        mvc.perform(get("/api/articles/{id}/series", article))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(series))
            .andExpect(jsonPath("$[0].articles").isEmpty());
        mvc.perform(get("/api/series/{id}", series)).andExpect(status().isOk())
            .andExpect(jsonPath("$.articles[0].article.content").isNotEmpty());
    }

    @Test
    void articleMembershipHandlesMultipleSeriesAndMissingResources() throws Exception{
        long article = createArticle(Map.of()).get("id").asLong();
        mvc.perform(get("/api/articles/{id}/series", article)).andExpect(jsonPath("$.length()").value(0));
        addArticle(createSeries("first", technologyId), article);
        addArticle(createSeries("second", technologyId), article);
        mvc.perform(get("/api/articles/{id}/series", article)).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/articles/99999/series")).andExpect(status().isNotFound());
        mvc.perform(get("/api/series/99999/directory")).andExpect(status().isNotFound());
    }

    @Test
    void nestedChaptersCanBeCreatedRenamedAndMoved() throws Exception{
        long series = createSeries("path", technologyId);
        long first = createChapter(series, "Concepts", null);
        long second = createChapter(series, "Types", null);
        long child = createChapter(series, "Wrappers", second);
        mvc.perform(get("/api/series/{id}/directory", series)).andExpect(status().isOk())
            .andExpect(jsonPath("$.chapters.length()").value(3));
        updateChapter(series, child, "Boxing", first).andExpect(status().isOk())
            .andExpect(jsonPath("$.parentId").value(first)).andExpect(jsonPath("$.title").value("Boxing"));
        updateChapter(series, child, "Boxing", null).andExpect(status().isOk())
            .andExpect(jsonPath("$.parentId").isEmpty()).andExpect(jsonPath("$.sortOrder").value(3));
    }

    @Test
    void chapterTreeRejectsCyclesAndForeignParents() throws Exception{
        long series = createSeries("path", technologyId);
        long root = createChapter(series, "Root", null);
        long child = createChapter(series, "Child", root);
        long foreign = createChapter(createSeries("other", technologyId), "Other", null);
        updateChapter(series, root, "Root", root).andExpect(status().isBadRequest());
        updateChapter(series, root, "Root", child).andExpect(status().isBadRequest());
        updateChapter(series, child, "Child", foreign).andExpect(status().isBadRequest());
        updateChapter(series, child, "Child", 99999L).andExpect(status().isBadRequest());
        postJson("/api/series/" + series + "/chapters", ownerToken, Map.of("title", "   "))
            .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT parent_id FROM series_chapters WHERE id = ?", Long.class, root)).isNull();
    }

    @Test
    void chapterDepthValidationAlsoChecksDescendantsWhenMoving() throws Exception{
        long series = createSeries("path", technologyId);
        Long parent = null;
        for(int depth = 1; depth <= 7; depth++){
            parent = createChapter(series, "Level " + depth, parent);
        }
        long subtree = createChapter(series, "Subtree", null);
        createChapter(series, "Subtree child", subtree);
        updateChapter(series, subtree, "Subtree", parent).andExpect(status().isBadRequest());
        long eighth = createChapter(series, "Level 8", parent);
        postJson("/api/series/" + series + "/chapters", ownerToken, Map.of("title", "Too deep", "parentId", eighth))
            .andExpect(status().isBadRequest());
    }

    @Test
    void chapterOrderingRequiresTheExactSiblingSet() throws Exception{
        long series = createSeries("path", technologyId);
        long first = createChapter(series, "First", null);
        long second = createChapter(series, "Second", null);
        long child = createChapter(series, "Child", first);
        reorderChapters(series, null, List.of(second, first)).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT sort_order FROM series_chapters WHERE id = ?", Integer.class, second)).isEqualTo(1);
        reorderChapters(series, null, List.of(first, first)).andExpect(status().isBadRequest());
        reorderChapters(series, null, List.of(first)).andExpect(status().isBadRequest());
        reorderChapters(series, null, List.of(first, child)).andExpect(status().isBadRequest());
        reorderChapters(series, first, List.of(child)).andExpect(status().isOk());
    }

    @Test
    void chapterAssignmentsAreScopedToTheSeriesAndCanBeCleared() throws Exception{
        long article = createArticle(Map.of()).get("id").asLong();
        long series = createSeries("path", technologyId);
        long otherSeries = createSeries("other", technologyId);
        long chapter = createChapter(series, "First", null);
        long foreign = createChapter(otherSeries, "Foreign", null);
        addArticle(series, article);
        addArticle(otherSeries, article);
        assignChapter(series, article, chapter).andExpect(status().isOk())
            .andExpect(jsonPath("$.articles[0].chapterId").value(chapter));
        assignChapter(series, article, chapter).andExpect(status().isOk());
        assignChapter(series, article, foreign).andExpect(status().isNotFound());
        assignChapter(series, 99999L, chapter).andExpect(status().isNotFound());
        mvc.perform(get("/api/series/{id}/directory", otherSeries)).andExpect(jsonPath("$.articles[0].chapterId").isEmpty());
        mvc.perform(get("/api/series/{id}/articles", series)).andExpect(jsonPath("$[0].chapterId").value(chapter));
        assignChapter(series, article, null).andExpect(status().isOk()).andExpect(jsonPath("$.articles[0].chapterId").isEmpty());
    }

    @Test
    void deletingALeafGroupPreservesItsArticlesAndParentDeletionIsGuarded() throws Exception{
        long article = createArticle(Map.of()).get("id").asLong();
        long series = createSeries("path", technologyId);
        long parent = createChapter(series, "Parent", null);
        long child = createChapter(series, "Child", parent);
        addArticle(series, article);
        assignChapter(series, article, child).andExpect(status().isOk());
        mvc.perform(delete("/api/series/{id}/chapters/{chapter}", series, parent).header("Authorization", ownerToken))
            .andExpect(status().isConflict());
        mvc.perform(delete("/api/series/{id}/chapters/{chapter}", series, child).header("Authorization", ownerToken))
            .andExpect(status().isOk());
        mvc.perform(get("/api/series/{id}/directory", series)).andExpect(jsonPath("$.articles[0].id").value(article))
            .andExpect(jsonPath("$.articles[0].chapterId").isEmpty());
        mvc.perform(get("/api/articles/{id}", article)).andExpect(status().isOk());
    }

    @Test
    void deletingASeriesWithNestedGroupsRetainsArticleData() throws Exception{
        long article = createArticle(Map.of()).get("id").asLong();
        long series = createSeries("path", technologyId);
        long parent = createChapter(series, "Parent", null);
        long child = createChapter(series, "Child", parent);
        addArticle(series, article);
        assignChapter(series, article, child).andExpect(status().isOk());
        mvc.perform(delete("/api/series/{id}", series).header("Authorization", ownerToken)).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM series_chapters", Long.class)).isZero();
        mvc.perform(get("/api/articles/{id}", article)).andExpect(status().isOk());
    }

    @Test
    void chapterWritesRequireAnEnabledOwnerOrAdmin() throws Exception{
        long series = createSeries("path", technologyId);
        long chapter = createChapter(series, "First", null);
        postJson("/api/series/" + series + "/chapters", null, Map.of("title", "Denied"))
            .andExpect(status().isUnauthorized());
        postJson("/api/series/" + series + "/chapters", otherToken, Map.of("title", "Denied"))
            .andExpect(status().isForbidden());
        mvc.perform(delete("/api/series/{id}/chapters/{chapter}", series, chapter).header("Authorization", otherToken))
            .andExpect(status().isForbidden());
        jdbc.update("UPDATE users SET enabled = FALSE WHERE username = ?", "Owner");
        postJson("/api/series/" + series + "/chapters", ownerToken, Map.of("title", "Denied"))
            .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/series/{id}/chapters/{chapter}", series, chapter).header("Authorization", adminToken))
            .andExpect(status().isOk());
    }

    @Test
    void chapterOrderRollbackRestoresEarlierWrites() throws Exception{
        long series = createSeries("path", technologyId);
        long first = createChapter(series, "First", null);
        long second = createChapter(series, "Second", null);
        jdbc.execute("ALTER TABLE series_chapters ADD CONSTRAINT reject_group_move CHECK(NOT(id = " + first + " AND sort_order = 2))");
        assertThrows(DataIntegrityViolationException.class, () -> seriesService.reorderChapters(series,
            new SeriesChapterOrderRequest(null, List.of(second, first)), ownerToken));
        assertThat(jdbc.queryForObject("SELECT sort_order FROM series_chapters WHERE id = ?", Integer.class, first)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT sort_order FROM series_chapters WHERE id = ?", Integer.class, second)).isEqualTo(2);
    }

    private long createChapter(long series, String title, Long parentId) throws Exception{
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("title", title);
        body.put("parentId", parentId);
        return response(postJson("/api/series/" + series + "/chapters", ownerToken, body).andExpect(status().isOk()))
            .get("id").asLong();
    }

    private ResultActions updateChapter(long series, long chapter, String title, Long parentId) throws Exception{
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("title", title);
        body.put("parentId", parentId);
        return mvc.perform(put("/api/series/{id}/chapters/{chapter}", series, chapter).header("Authorization", ownerToken)
            .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)));
    }

    private ResultActions reorderChapters(long series, Long parentId, List<Long> ids) throws Exception{
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("parentId", parentId);
        body.put("chapterIds", ids);
        return mvc.perform(put("/api/series/{id}/chapters/order", series).header("Authorization", ownerToken)
            .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)));
    }

    private ResultActions assignChapter(long series, long article, Long chapterId) throws Exception{
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("chapterId", chapterId);
        return mvc.perform(put("/api/series/{id}/articles/{article}/chapter", series, article).header("Authorization", ownerToken)
            .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)));
    }

    private void insertUser(String username, String role, boolean enabled){
        jdbc.update("""
            INSERT INTO users(username, password_hash, role, enabled, created_at, updated_at)
            VALUES(?, 'test-only', ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """, username, role, enabled);
    }

    private String token(String username){
        Long id = jdbc.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, username);
        User user = userRepository.findById(id).orElseThrow();
        return "Bearer " + jwtService.generateToken(user);
    }

    private long sectionId(String slug){
        return jdbc.queryForObject("SELECT id FROM sections WHERE slug = ?", Long.class, slug);
    }

    private Map<String, Object> articleBody(Map<String, ?> extra){
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("title", "Article");
        body.put("content", "Markdown body [Reference](card:1)");
        body.put("categoryId", categoryId);
        body.putAll(extra);
        return body;
    }

    private JsonNode createArticle(Map<String, ?> extra) throws Exception{
        return response(postJson("/api/articles", ownerToken, articleBody(extra)).andExpect(status().isOk()));
    }

    private ResultActions updateArticle(long id, Map<String, ?> extra) throws Exception{
        return mvc.perform(put("/api/articles/{id}", id).header("Authorization", ownerToken)
            .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(articleBody(extra))));
    }

    private long createTag(String slug) throws Exception{
        return response(postJson("/api/tags", adminToken, Map.of("slug", slug, "name", slug)).andExpect(status().isOk()))
            .get("id").asLong();
    }

    private long createCategory(String name, long section) throws Exception{
        return response(postJson("/api/categories", adminToken, Map.of("name", name, "sectionId", section)).andExpect(status().isOk()))
            .get("id").asLong();
    }

    private long createSeries(String slug, long section) throws Exception{
        return response(postJson("/api/series", ownerToken, Map.of("slug", slug, "title", slug, "sectionId", section))
            .andExpect(status().isOk())).get("id").asLong();
    }

    private void addArticle(long series, long article) throws Exception{
        mvc.perform(post("/api/series/{id}/articles/{articleId}", series, article).header("Authorization", ownerToken))
            .andExpect(status().isOk());
    }

    private ResultActions reorder(long series, List<Long> ids) throws Exception{
        return mvc.perform(put("/api/series/{id}/articles/order", series).header("Authorization", ownerToken)
            .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("articleIds", ids))));
    }

    private ResultActions postJson(String path, String token, Map<String, ?> body) throws Exception{
        var request = post(path).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body));
        if(token != null){
            request.header("Authorization", token);
        }
        return mvc.perform(request);
    }

    private JsonNode response(ResultActions result) throws Exception{
        return mapper.readTree(result.andReturn().getResponse().getContentAsString());
    }
}
