package com.qijx.blog.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;

import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
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
        
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement preparedStatement = connection.prepareStatement(
                sql,
                Statement.RETURN_GENERATED_KEYS
            );

            preparedStatement.setString(1, article.getTitle());
            preparedStatement.setString(2, article.getContent());
            preparedStatement.setTimestamp(3, Timestamp.valueOf(article.getCreatedAt()));
            preparedStatement.setTimestamp(4, Timestamp.valueOf(article.getUpdatedAt()));

            return preparedStatement;
        }, keyHolder);

        Number key = keyHolder.getKey();

        if(key != null){
            article.setId(key.longValue());
        }

        return article;
    }

    public List<Article> findAll(){
        String sql = """
                SELECT id, title, content, created_at, updated_at 
                FROM articles
                ORDER BY id DESC
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Article article = new Article();

            article.setId(rs.getLong("id"));
            article.setTitle(rs.getString("title"));
            article.setContent(rs.getString("content"));
            article.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            article.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());

            return article;
        });
    }

    public Optional<Article> findById(Long id){
        String sql = """
                SELECT id, title, content, created_at, updated_at
                FROM articles
                WHERE id = ?
                """;

        List<Article> articles = jdbcTemplate.query(sql, this::mapRow, id);
    }
}
