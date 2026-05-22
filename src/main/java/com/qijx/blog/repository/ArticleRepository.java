package com.qijx.blog.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.qijx.blog.entity.Article;

@Repository
public class ArticleRepository {

    private final JdbcTemplate jdbcTemplate;
    
    public ArticleRepository(JdbcTemplate jdbcTemplate){
        this.jdbcTemplate = jdbcTemplate;
    }

    public Article save(Article article){
        String sql = """
                INSERT INTO articles(title, content, created_at, updated_at)
                VALUES(?, ?, ?, ?)
                """;

        return article;
    }
}
