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

import com.qijx.blog.entity.Article;
import com.qijx.blog.entity.ArticleRevision;

@Repository
public class ArticleRevisionRepository {
    private final JdbcTemplate jdbcTemplate;

    public ArticleRevisionRepository(JdbcTemplate jdbcTemplate){
        this.jdbcTemplate = jdbcTemplate;
    }

    public ArticleRevision saveSnapshot(Article article, Long createdBy, java.time.LocalDateTime createdAt){
        Integer nextRevision = jdbcTemplate.queryForObject(
            "SELECT COALESCE(MAX(revision_number), 0) + 1 FROM article_revisions WHERE article_id = ?",
            Integer.class,
            article.getId()
        );

        ArticleRevision revision = new ArticleRevision();
        revision.setArticleId(article.getId());
        revision.setRevisionNumber(nextRevision == null ? 1 : nextRevision);
        revision.setTitle(article.getTitle());
        revision.setContent(article.getContent());
        revision.setCategoryId(article.getCategoryId());
        revision.setCreatedBy(createdBy);
        revision.setCreatedAt(createdAt);

        String sql = """
                INSERT INTO article_revisions(
                    article_id, revision_number, title, content, category_id, created_by, created_at
                )
                VALUES(?, ?, ?, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, revision.getArticleId());
            statement.setInt(2, revision.getRevisionNumber());
            statement.setString(3, revision.getTitle());
            statement.setString(4, revision.getContent());
            if(revision.getCategoryId() == null){
                statement.setNull(5, java.sql.Types.BIGINT);
            }else{
                statement.setLong(5, revision.getCategoryId());
            }
            statement.setLong(6, revision.getCreatedBy());
            statement.setTimestamp(7, Timestamp.valueOf(revision.getCreatedAt()));
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if(key != null){
            revision.setId(key.longValue());
        }

        return revision;
    }

    public List<ArticleRevision> findByArticleId(Long articleId){
        String sql = baseSelect() + """
                WHERE revisions.article_id = ?
                ORDER BY revisions.revision_number DESC
                """;

        return jdbcTemplate.query(sql, this::mapRow, articleId);
    }

    public Optional<ArticleRevision> findByIdAndArticleId(Long revisionId, Long articleId){
        String sql = baseSelect() + """
                WHERE revisions.id = ? AND revisions.article_id = ?
                """;

        List<ArticleRevision> revisions = jdbcTemplate.query(sql, this::mapRow, revisionId, articleId);
        return revisions.isEmpty() ? Optional.empty() : Optional.of(revisions.get(0));
    }

    private String baseSelect(){
        return """
                SELECT revisions.id,
                       revisions.article_id,
                       revisions.revision_number,
                       revisions.title,
                       revisions.content,
                       revisions.category_id,
                       revisions.created_by,
                       users.username AS created_by_name,
                       revisions.created_at
                FROM article_revisions revisions
                LEFT JOIN users ON revisions.created_by = users.id
                """;
    }

    private ArticleRevision mapRow(ResultSet rs, int rowNum) throws SQLException{
        ArticleRevision revision = new ArticleRevision();
        revision.setId(rs.getLong("id"));
        revision.setArticleId(rs.getLong("article_id"));
        revision.setRevisionNumber(rs.getInt("revision_number"));
        revision.setTitle(rs.getString("title"));
        revision.setContent(rs.getString("content"));
        long categoryId = rs.getLong("category_id");
        revision.setCategoryId(rs.wasNull() ? null : categoryId);
        revision.setCreatedBy(rs.getLong("created_by"));
        revision.setCreatedByName(rs.getString("created_by_name"));
        revision.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return revision;
    }
}
