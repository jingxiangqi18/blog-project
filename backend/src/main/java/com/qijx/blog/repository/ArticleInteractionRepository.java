package com.qijx.blog.repository;

import java.time.LocalDateTime;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ArticleInteractionRepository {
    private final JdbcTemplate jdbcTemplate;

    public ArticleInteractionRepository(JdbcTemplate jdbcTemplate){
        this.jdbcTemplate = jdbcTemplate;
    }

    public int addLike(Long articleId, Long userId, LocalDateTime createdAt){
        String sql = """
                INSERT IGNORE INTO article_likes(article_id, user_id, created_at)
                VALUES(?, ?, ?)
                """;
        return jdbcTemplate.update(sql, articleId, userId, createdAt);
    }

    public int removeLike(Long articleId, Long userId){
        return jdbcTemplate.update(
            "DELETE FROM article_likes WHERE article_id = ? AND user_id = ?",
            articleId,
            userId
        );
    }

    public long countLikes(Long articleId){
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM article_likes WHERE article_id = ?",
            Long.class,
            articleId
        );
        return count == null ? 0L : count;
    }

    public boolean hasLiked(Long articleId, Long userId){
        return exists("article_likes", articleId, userId);
    }

    public int addFavorite(Long articleId, Long userId, LocalDateTime createdAt){
        String sql = """
                INSERT IGNORE INTO article_favorites(article_id, user_id, created_at)
                VALUES(?, ?, ?)
                """;
        return jdbcTemplate.update(sql, articleId, userId, createdAt);
    }

    public int removeFavorite(Long articleId, Long userId){
        return jdbcTemplate.update(
            "DELETE FROM article_favorites WHERE article_id = ? AND user_id = ?",
            articleId,
            userId
        );
    }

    public long countFavorites(Long articleId){
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM article_favorites WHERE article_id = ?",
            Long.class,
            articleId
        );
        return count == null ? 0L : count;
    }

    public boolean hasFavorited(Long articleId, Long userId){
        return exists("article_favorites", articleId, userId);
    }

    private boolean exists(String tableName, Long articleId, Long userId){
        String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE article_id = ? AND user_id = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, articleId, userId);
        return count != null && count > 0;
    }
}
