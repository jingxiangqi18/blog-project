package com.qijx.blog.repository;

import java.time.LocalDateTime;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CommentLikeRepository {
    private final JdbcTemplate jdbcTemplate;

    public CommentLikeRepository(JdbcTemplate jdbcTemplate){
        this.jdbcTemplate = jdbcTemplate;
    }

    public int add(Long commentId, Long userId, LocalDateTime createdAt){
        String sql = """
                INSERT IGNORE INTO comment_likes(comment_id, user_id, created_at)
                VALUES(?, ?, ?)
                """;
        return jdbcTemplate.update(sql, commentId, userId, createdAt);
    }

    public int remove(Long commentId, Long userId){
        return jdbcTemplate.update(
            "DELETE FROM comment_likes WHERE comment_id = ? AND user_id = ?",
            commentId,
            userId
        );
    }

    public long count(Long commentId){
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM comment_likes WHERE comment_id = ?",
            Long.class,
            commentId
        );
        return count == null ? 0L : count;
    }

    public boolean hasLiked(Long commentId, Long userId){
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM comment_likes WHERE comment_id = ? AND user_id = ?",
            Long.class,
            commentId,
            userId
        );
        return count != null && count > 0;
    }
}
