package com.qijx.blog.repository;

import java.util.List;
import java.util.Optional;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.SQLException;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.qijx.blog.entity.Comment;

@Repository
public class CommentRepository {
    private final JdbcTemplate jdbcTemplate;

    public CommentRepository(JdbcTemplate jdbcTemplate){
        this.jdbcTemplate = jdbcTemplate;
    }

    public Comment save(Long articleId, Comment comment){
        String sql = """
                INSERT INTO comments(article_id, content, created_at, updated_at)
                VALUES(?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement preparedStatement = connection.prepareStatement(
                sql,
                Statement.RETURN_GENERATED_KEYS
            );

            preparedStatement.setLong(1, articleId);
            preparedStatement.setString(2, comment.getContent());
            preparedStatement.setTimestamp(3, Timestamp.valueOf(comment.getCreatedAt()));
            preparedStatement.setTimestamp(4, Timestamp.valueOf(comment.getUpdatedAt()));

            return preparedStatement;
        }, keyHolder);

        Number key = keyHolder.getKey();

        if(key != null){
            comment.setId(key.longValue());
        }

        return comment;
    }

    public List<Comment> findByArticleId(Long articleId){
        String sql = """
                SELECT id, article_id, content, created_at, updated_at
                FROM comments
                WHERE article_id = ?
                ORDER BY id DESC
                """;

        return jdbcTemplate.query(sql, this::mapRow, articleId);
    }

    private Comment mapRow(ResultSet rs, int rowNum) throws SQLException{
        Comment comment = new Comment();

        comment.setId(rs.getLong("id"));
        comment.setArticleId(rs.getLong("article_id"));
        comment.setContent(rs.getString("content"));
        comment.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        comment.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());

        return comment;
    }

    public Optional<Comment> findByIdAndArticleId(Long articleId, Long id){
        String sql = """
                SELECT id, article_id, content, created_at, updated_at
                FROM comments
                WHERE id = ? AND article_id = ?
                """;

        List<Comment> comments = jdbcTemplate.query(sql, this::mapRow, id, articleId);

        if(comments.isEmpty()){
            return Optional.empty();
        }

        return Optional.of(comments.get(0));
    }

    public int deleteByIdAndArticleId(Long articleId, Long id){
        String sql = """
                DELETE FROM comments
                WHERE id = ? AND article_id = ?
                """;

        return jdbcTemplate.update(sql, id, articleId);
    }

    public int update(Long articleId, Long id, Comment comment){
        String sql = """
                UPDATE comments
                SET content = ?, updated_at = ?
                WHERE id = ? AND article_id = ?
                """;

        return jdbcTemplate.update(
            sql,
            comment.getContent(),
            comment.getUpdatedAt(),
            id,
            articleId
        );
    }
}