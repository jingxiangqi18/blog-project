package com.qijx.blog.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import com.qijx.blog.dto.PageResponse;
import com.qijx.blog.entity.Article;
import com.qijx.blog.entity.SeriesArticle;

@Repository
public class ArticleRepository {
    private final JdbcTemplate jdbcTemplate;
    private final TagRepository tagRepository;

    public ArticleRepository(JdbcTemplate jdbcTemplate, TagRepository tagRepository){
        this.jdbcTemplate = jdbcTemplate;
        this.tagRepository = tagRepository;
    }

    public Article save(Article article){
        String sql = """
            INSERT INTO articles(title, content, category_id, author_id, created_at, updated_at,
                                 section_id, summary, content_type)
            VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, article.getTitle());
            statement.setString(2, article.getContent());
            statement.setObject(3, article.getCategoryId());
            statement.setLong(4, article.getAuthorId());
            statement.setObject(5, article.getCreatedAt());
            statement.setObject(6, article.getUpdatedAt());
            statement.setObject(7, article.getSectionId());
            statement.setString(8, article.getSummary());
            statement.setString(9, article.getContentType());
            return statement;
        }, keyHolder);
        article.setId(keyHolder.getKey().longValue());
        return article;
    }

    public PageResponse<Article> findPage(int page, int size, String keyword, Long categoryId){
        return findPage(page, size, keyword, categoryId, null, null, null, null);
    }

    public PageResponse<Article> findPage(int page, int size, String keyword, Long categoryId,
        String sectionSlug, String contentType, String tagSlug, Long seriesId){
        StringBuilder whereSql = new StringBuilder(" WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();
        if(!keyword.isBlank()){
            whereSql.append(" AND (articles.title LIKE ? OR articles.content LIKE ? OR categories.name LIKE ?) ");
            String like = "%" + keyword + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if(categoryId != null){
            whereSql.append(" AND articles.category_id = ? ");
            params.add(categoryId);
        }
        if(sectionSlug != null){
            whereSql.append(" AND sections.slug = ? ");
            params.add(sectionSlug);
        }
        if(contentType != null){
            whereSql.append(" AND articles.content_type = ? ");
            params.add(contentType);
        }
        // EXISTS keeps one result row per article even when multiple tags/series match.
        if(tagSlug != null){
            whereSql.append("""
                AND EXISTS (SELECT 1 FROM article_tags
                    JOIN tags ON tags.id = article_tags.tag_id
                    WHERE article_tags.article_id = articles.id AND tags.slug = ?)
                """);
            params.add(tagSlug);
        }
        if(seriesId != null){
            whereSql.append("""
                AND EXISTS (SELECT 1 FROM series_articles
                    WHERE series_articles.article_id = articles.id AND series_articles.series_id = ?)
                """);
            params.add(seriesId);
        }
        Long total = jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM articles
            LEFT JOIN categories ON articles.category_id = categories.id
            LEFT JOIN sections ON articles.section_id = sections.id
            """ + whereSql, Long.class, params.toArray());
        params.add(size);
        params.add(((long) page - 1) * size);
        List<Article> articles = queryArticles(baseSelect() + whereSql
            + " ORDER BY articles.updated_at DESC, articles.id DESC LIMIT ? OFFSET ?", params.toArray());
        return new PageResponse<>(articles, page, size, total == null ? 0 : total);
    }

    public Optional<Article> findById(Long id){
        return queryArticles(baseSelect() + " WHERE articles.id = ?", id).stream().findFirst();
    }

    public boolean existsById(Long id){
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM articles WHERE id = ?)", Boolean.class, id));
    }

    public Optional<Article> findByIdForUpdate(Long id){
        // Lock the article row before replacing its tag set or taking a new revision.
        return queryArticles(baseSelect() + " WHERE articles.id = ? FOR UPDATE", id).stream().findFirst();
    }

    public int update(Long id, Article article){
        return jdbcTemplate.update("""
            UPDATE articles SET title = ?, content = ?, category_id = ?, updated_at = ?,
                section_id = ?, summary = ?, content_type = ? WHERE id = ?
            """, article.getTitle(), article.getContent(), article.getCategoryId(), article.getUpdatedAt(),
            article.getSectionId(), article.getSummary(), article.getContentType(), id);
    }

    public int deleteById(Long id){
        return jdbcTemplate.update("DELETE FROM articles WHERE id = ?", id);
    }

    public int clearCategoryId(Long categoryId){
        return jdbcTemplate.update("UPDATE articles SET category_id = NULL WHERE category_id = ?", categoryId);
    }

    public List<Article> findByCategoryId(Long categoryId){
        return queryArticles(baseSelect() + " WHERE articles.category_id = ? ORDER BY articles.id DESC", categoryId);
    }

    public List<Article> findByAuthorId(Long authorId){
        return queryArticles(baseSelect()
            + " WHERE articles.author_id = ? ORDER BY articles.updated_at DESC, articles.id DESC", authorId);
    }

    public List<Article> findFavoritedByUserId(Long userId){
        return queryArticles(baseSelect() + """
            JOIN article_favorites ON article_favorites.article_id = articles.id
            WHERE article_favorites.user_id = ? ORDER BY article_favorites.created_at DESC
            """, userId);
    }

    public List<Article> findBySeriesId(Long seriesId){
        return findSeriesArticles(seriesId).stream().map(SeriesArticle::article).toList();
    }

    public List<SeriesArticle> findSeriesArticles(Long seriesId){
        String sql = """
            SELECT articles.*, users.username AS author_name, categories.name AS category_name,
                sections.slug AS section_slug, sections.name AS section_name, series_articles.sort_order, series_articles.chapter_id
            FROM series_articles JOIN articles ON articles.id = series_articles.article_id
            LEFT JOIN categories ON articles.category_id = categories.id
            LEFT JOIN users ON articles.author_id = users.id
            LEFT JOIN sections ON articles.section_id = sections.id
            WHERE series_articles.series_id = ? ORDER BY series_articles.sort_order, articles.id
            """;
        List<SeriesArticle> chapters = jdbcTemplate.query(sql,
            (rs, rowNum) -> new SeriesArticle(rs.getInt("sort_order"), mapRow(rs, rowNum),
                rs.getObject("chapter_id", Long.class)), seriesId);
        tagRepository.populateTags(chapters.stream().map(SeriesArticle::article).toList());
        return chapters;
    }

    private String baseSelect(){
        return """
            SELECT articles.*, users.username AS author_name, categories.name AS category_name,
                sections.slug AS section_slug, sections.name AS section_name
            FROM articles
            LEFT JOIN categories ON articles.category_id = categories.id
            LEFT JOIN users ON articles.author_id = users.id
            LEFT JOIN sections ON articles.section_id = sections.id
            """;
    }

    private List<Article> queryArticles(String sql, Object... params){
        List<Article> articles = jdbcTemplate.query(sql, this::mapRow, params);
        tagRepository.populateTags(articles);
        return articles;
    }

    private Article mapRow(ResultSet rs, int rowNum) throws SQLException{
        Article article = new Article();
        article.setId(rs.getLong("id"));
        article.setTitle(rs.getString("title"));
        article.setContent(rs.getString("content"));
        article.setCategoryId(rs.getObject("category_id", Long.class));
        article.setAuthorId(rs.getObject("author_id", Long.class));
        article.setAuthorName(rs.getString("author_name"));
        article.setCategoryName(rs.getString("category_name"));
        article.setSectionId(rs.getObject("section_id", Long.class));
        article.setSectionSlug(rs.getString("section_slug"));
        article.setSectionName(rs.getString("section_name"));
        article.setSummary(rs.getString("summary"));
        article.setContentType(rs.getString("content_type"));
        article.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        article.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return article;
    }
}
