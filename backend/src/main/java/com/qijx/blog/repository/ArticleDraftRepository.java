package com.qijx.blog.repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.qijx.blog.entity.ArticleDraft;

@Repository
public class ArticleDraftRepository {
    private final JdbcTemplate jdbcTemplate;

    public ArticleDraftRepository(JdbcTemplate jdbcTemplate){
        this.jdbcTemplate = jdbcTemplate;
    }

    public ArticleDraft save(ArticleDraft draft){
        String sql = """
                INSERT INTO article_drafts(
                    article_id, author_id, title, content, category_id, created_at, updated_at
                )
                VALUES(?, ?, ?, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            setNullableLong(statement, 1, draft.getArticleId());
            statement.setLong(2, draft.getAuthorId());
            statement.setString(3, draft.getTitle());
            statement.setString(4, draft.getContent());
            setNullableLong(statement, 5, draft.getCategoryId());
            statement.setTimestamp(6, Timestamp.valueOf(draft.getCreatedAt()));
            statement.setTimestamp(7, Timestamp.valueOf(draft.getUpdatedAt()));
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if(key != null){
            draft.setId(key.longValue());
        }

        return draft;
    }

    public Optional<ArticleDraft> findById(Long id){
        String sql = """
                SELECT id, article_id, author_id, title, content, category_id, created_at, updated_at
                FROM article_drafts
                WHERE id = ?
                """;

        return firstOrEmpty(jdbcTemplate.query(sql, this::mapRow, id));
    }

    public Optional<ArticleDraft> findByArticleIdAndAuthorId(Long articleId, Long authorId){
        String sql = """
                SELECT id, article_id, author_id, title, content, category_id, created_at, updated_at
                FROM article_drafts
                WHERE article_id = ? AND author_id = ?
                """;

        return firstOrEmpty(jdbcTemplate.query(sql, this::mapRow, articleId, authorId));
    }

    public int update(Long id, ArticleDraft draft){
        String sql = """
                UPDATE article_drafts
                SET title = ?, content = ?, category_id = ?, updated_at = ?
                WHERE id = ?
                """;

        return jdbcTemplate.update(
            sql,
            draft.getTitle(),
            draft.getContent(),
            draft.getCategoryId(),
            draft.getUpdatedAt(),
            id
        );
    }

    public int deleteById(Long id){
        return jdbcTemplate.update("DELETE FROM article_drafts WHERE id = ?", id);
    }

    public int deleteByArticleIdAndAuthorId(Long articleId, Long authorId){
        return jdbcTemplate.update(
            "DELETE FROM article_drafts WHERE article_id = ? AND author_id = ?",
            articleId,
            authorId
        );
    }

    private Optional<ArticleDraft> firstOrEmpty(List<ArticleDraft> drafts){
        return drafts.isEmpty() ? Optional.empty() : Optional.of(drafts.get(0));
    }

    private ArticleDraft mapRow(ResultSet rs, int rowNum) throws SQLException{
        ArticleDraft draft = new ArticleDraft();
        draft.setId(rs.getLong("id"));
        draft.setArticleId(getNullableLong(rs, "article_id"));
        draft.setAuthorId(rs.getLong("author_id"));
        draft.setTitle(rs.getString("title"));
        draft.setContent(rs.getString("content"));
        draft.setCategoryId(getNullableLong(rs, "category_id"));
        draft.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        draft.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return draft;
    }

    private Long getNullableLong(ResultSet rs, String column) throws SQLException{
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private void setNullableLong(PreparedStatement statement, int index, Long value) throws SQLException{
        if(value == null){
            statement.setNull(index, java.sql.Types.BIGINT);
        }else{
            statement.setLong(index, value);
        }
    }
}
