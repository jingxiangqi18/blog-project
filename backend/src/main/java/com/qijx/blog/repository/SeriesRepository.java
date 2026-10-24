package com.qijx.blog.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import com.qijx.blog.dto.PageResponse;
import com.qijx.blog.dto.SeriesArticleSummary;
import com.qijx.blog.entity.Series;
import com.qijx.blog.entity.SeriesChapter;

@Repository
public class SeriesRepository {
    private final JdbcTemplate jdbcTemplate;

    public SeriesRepository(JdbcTemplate jdbcTemplate){
        this.jdbcTemplate = jdbcTemplate;
    }

    public PageResponse<Series> findPage(int page, int size, Long sectionId, String keyword){
        String where = " WHERE (series.title LIKE ? OR series.description LIKE ?) ";
        List<Object> params = new ArrayList<>(List.of("%" + keyword + "%", "%" + keyword + "%"));
        if(sectionId != null){
            where += " AND series.section_id = ? ";
            params.add(sectionId);
        }
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM series" + where, Long.class, params.toArray());
        params.add(size);
        params.add(((long) page - 1) * size);
        List<Series> series = jdbcTemplate.query(baseSelect() + where
            + " ORDER BY series.updated_at DESC, series.id DESC LIMIT ? OFFSET ?", this::mapRow, params.toArray());
        return new PageResponse<>(series, page, size, total == null ? 0 : total);
    }

    public Optional<Series> findById(Long id){
        return jdbcTemplate.query(baseSelect() + " WHERE series.id = ?", this::mapRow, id).stream().findFirst();
    }

    public Optional<Series> findByIdForUpdate(Long id){
        return jdbcTemplate.query(baseSelect() + " WHERE series.id = ? FOR UPDATE", this::mapRow, id).stream().findFirst();
    }

    public void save(Series series){
        String sql = """
            INSERT INTO series(slug, title, description, section_id, category_id, created_by, created_at, updated_at)
            VALUES(?, ?, ?, ?, ?, ?, ?, ?)
            """;
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, series.getSlug());
            statement.setString(2, series.getTitle());
            statement.setString(3, series.getDescription());
            statement.setLong(4, series.getSectionId());
            statement.setObject(5, series.getCategoryId());
            statement.setLong(6, series.getCreatedBy());
            statement.setObject(7, series.getCreatedAt());
            statement.setObject(8, series.getUpdatedAt());
            return statement;
        }, keyHolder);
        series.setId(keyHolder.getKey().longValue());
    }

    public int update(Long id, Series series){
        return jdbcTemplate.update("""
            UPDATE series SET slug = ?, title = ?, description = ?, section_id = ?, category_id = ?, updated_at = ?
            WHERE id = ?
            """, series.getSlug(), series.getTitle(), series.getDescription(), series.getSectionId(), series.getCategoryId(),
            series.getUpdatedAt(), id);
    }

    public int deleteById(Long id){
        return jdbcTemplate.update("DELETE FROM series WHERE id = ?", id);
    }

    public void touch(Long id){
        jdbcTemplate.update("UPDATE series SET updated_at = ? WHERE id = ?", LocalDateTime.now(), id);
    }

    public Map<Long, Integer> findArticleOrders(Long seriesId){
        Map<Long, Integer> orders = new HashMap<>();
        jdbcTemplate.query("SELECT article_id, sort_order FROM series_articles WHERE series_id = ?", rs -> {
            orders.put(rs.getLong("article_id"), rs.getInt("sort_order"));
        }, seriesId);
        return orders;
    }

    public void addArticle(Long seriesId, Long articleId){
        Integer next = jdbcTemplate.queryForObject(
            "SELECT COALESCE(MAX(sort_order), 0) + 1 FROM series_articles WHERE series_id = ?", Integer.class, seriesId);
        jdbcTemplate.update("INSERT INTO series_articles(series_id, article_id, sort_order) VALUES(?, ?, ?)", seriesId, articleId, next);
    }

    public int removeArticle(Long seriesId, Long articleId){
        return jdbcTemplate.update("DELETE FROM series_articles WHERE series_id = ? AND article_id = ?", seriesId, articleId);
    }

    public int updateOrder(Long seriesId, Long articleId, int sortOrder){
        return jdbcTemplate.update("UPDATE series_articles SET sort_order = ? WHERE series_id = ? AND article_id = ?",
            sortOrder, seriesId, articleId);
    }

    public boolean hasConflictingSeries(Long articleId, Long sectionId){
        if(sectionId == null){
            return false;
        }
        Long count = jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM series_articles JOIN series ON series.id = series_articles.series_id
            WHERE series_articles.article_id = ? AND series.section_id <> ?
            """, Long.class, articleId, sectionId);
        return count > 0;
    }

    public List<Series> findByArticleId(Long articleId){
        return jdbcTemplate.query(baseSelect() + """
            JOIN series_articles ON series_articles.series_id = series.id
            WHERE series_articles.article_id = ? ORDER BY series.updated_at DESC, series.id DESC
            """, this::mapRow, articleId);
    }

    public List<SeriesArticleSummary> findDirectoryArticles(Long seriesId){
        // The sidebar does not need article bodies or tag hydration.
        return jdbcTemplate.query("""
            SELECT articles.id, articles.title, articles.summary, series_articles.chapter_id, series_articles.sort_order
            FROM series_articles JOIN articles ON articles.id = series_articles.article_id
            WHERE series_articles.series_id = ? ORDER BY series_articles.sort_order, articles.id
            """, (rs, rowNum) -> new SeriesArticleSummary(rs.getLong("id"), rs.getString("title"),
                rs.getString("summary"), rs.getObject("chapter_id", Long.class), rs.getInt("sort_order")), seriesId);
    }

    public List<SeriesChapter> findChapters(Long seriesId){
        return jdbcTemplate.query("SELECT * FROM series_chapters WHERE series_id = ? ORDER BY sort_order, id",
            this::mapChapter, seriesId);
    }

    public Optional<SeriesChapter> findChapter(Long seriesId, Long chapterId){
        return jdbcTemplate.query("SELECT * FROM series_chapters WHERE series_id = ? AND id = ?",
            this::mapChapter, seriesId, chapterId).stream().findFirst();
    }

    public SeriesChapter saveChapter(Long seriesId, Long parentId, String title){
        int sortOrder = nextChapterOrder(seriesId, parentId);
        LocalDateTime now = LocalDateTime.now();
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement("""
                INSERT INTO series_chapters(series_id, parent_id, title, sort_order, created_at, updated_at)
                VALUES(?, ?, ?, ?, ?, ?)
                """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, seriesId);
            statement.setObject(2, parentId);
            statement.setString(3, title);
            statement.setInt(4, sortOrder);
            statement.setObject(5, now);
            statement.setObject(6, now);
            return statement;
        }, keyHolder);
        return findChapter(seriesId, keyHolder.getKey().longValue()).orElseThrow();
    }

    public void updateChapter(Long seriesId, Long chapterId, Long parentId, String title, int sortOrder){
        jdbcTemplate.update("""
            UPDATE series_chapters SET parent_id = ?, title = ?, sort_order = ?, updated_at = ?
            WHERE series_id = ? AND id = ?
            """, parentId, title, sortOrder, LocalDateTime.now(), seriesId, chapterId);
    }

    public int nextChapterOrder(Long seriesId, Long parentId){
        return jdbcTemplate.queryForObject("""
            SELECT COALESCE(MAX(sort_order), 0) + 1 FROM series_chapters
            WHERE series_id = ? AND (parent_id = ? OR (parent_id IS NULL AND ? IS NULL))
            """, Integer.class, seriesId, parentId, parentId);
    }

    public void updateChapterOrder(Long seriesId, Long chapterId, int sortOrder){
        jdbcTemplate.update("UPDATE series_chapters SET sort_order = ?, updated_at = ? WHERE series_id = ? AND id = ?",
            sortOrder, LocalDateTime.now(), seriesId, chapterId);
    }

    public void deleteChapter(Long seriesId, Long chapterId){
        jdbcTemplate.update("UPDATE series_articles SET chapter_id = NULL WHERE series_id = ? AND chapter_id = ?",
            seriesId, chapterId);
        jdbcTemplate.update("DELETE FROM series_chapters WHERE series_id = ? AND id = ?", seriesId, chapterId);
    }

    public int updateArticleChapter(Long seriesId, Long articleId, Long chapterId){
        return jdbcTemplate.update("UPDATE series_articles SET chapter_id = ? WHERE series_id = ? AND article_id = ?",
            chapterId, seriesId, articleId);
    }

    private SeriesChapter mapChapter(ResultSet rs, int rowNum) throws SQLException{
        return new SeriesChapter(rs.getLong("id"), rs.getLong("series_id"), rs.getObject("parent_id", Long.class),
            rs.getString("title"), rs.getInt("sort_order"), rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime());
    }

    private String baseSelect(){
        return """
            SELECT series.*, sections.slug AS section_slug, sections.name AS section_name, categories.name AS category_name
            FROM series JOIN sections ON sections.id = series.section_id
            LEFT JOIN categories ON categories.id = series.category_id
            """;
    }

    private Series mapRow(ResultSet rs, int rowNum) throws SQLException{
        Series series = new Series();
        series.setId(rs.getLong("id"));
        series.setSlug(rs.getString("slug"));
        series.setTitle(rs.getString("title"));
        series.setDescription(rs.getString("description"));
        series.setSectionId(rs.getLong("section_id"));
        series.setCategoryId(rs.getObject("category_id", Long.class));
        series.setSectionSlug(rs.getString("section_slug"));
        series.setSectionName(rs.getString("section_name"));
        series.setCategoryName(rs.getString("category_name"));
        series.setCreatedBy(rs.getLong("created_by"));
        series.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        series.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return series;
    }
}
