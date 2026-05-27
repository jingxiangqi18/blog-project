package com.qijx.blog.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;

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

    public Comment save(Comment comment){
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

            preparedStatement.setLong(1, comment.getArticleId());
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
}

