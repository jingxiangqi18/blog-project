package com.qijx.blog.repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import java.util.List;
import java.util.Optional;

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
                INSERT INTO articles(title, content, category_id, created_at, updated_at)
                VALUES(?, ?, ?, ?, ?)
                """;
        
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement preparedStatement = connection.prepareStatement(
                sql,
                Statement.RETURN_GENERATED_KEYS
            );

            preparedStatement.setString(1, article.getTitle());
            preparedStatement.setString(2, article.getContent());
            preparedStatement.setLong(3, article.getCategoryId());
            preparedStatement.setTimestamp(4, Timestamp.valueOf(article.getCreatedAt()));
            preparedStatement.setTimestamp(5, Timestamp.valueOf(article.getUpdatedAt()));

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
                SELECT articles.id, articles.title, articles.content, articles.category_id, categories.name AS category_name, articles.created_at, articles.updated_at 
                FROM articles
                LEFT JOIN categories ON articles.category_id = categories.id
                ORDER BY articles.id DESC
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Article article = new Article();

            article.setId(rs.getLong("id"));
            article.setTitle(rs.getString("title"));
            article.setContent(rs.getString("content"));
            article.setCategoryId(rs.getLong("category_id"));
            article.setCategoryName(rs.getString("category_name"));
            article.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            article.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());

            return article;
        });
    }

    public Optional<Article> findById(Long id){
        String sql = """
                SELECT articles.id, articles.title, articles.content, articles.category_id, categories.name AS category_name, articles.created_at, articles.updated_at
                FROM articles
                LEFT JOIN categories ON articles.category_id = categories.id
                WHERE articles.id = ?
                """;

        List<Article> articles = jdbcTemplate.query(sql, this::mapRow, id);

        if(articles.isEmpty()){
            return Optional.empty();
        }

        return Optional.of(articles.get(0));
    }

    private Article mapRow(ResultSet rs, int rowNum) throws SQLException{
        Article article = new Article();

        article.setId(rs.getLong("id"));
        article.setTitle(rs.getString("title"));
        article.setContent(rs.getString("content"));
        article.setCategoryId(rs.getLong("category_id"));
        article.setCategoryName(rs.getString("category_name"));
        article.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        article.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());

        return article;
    }

    public int update(Long id, Article article){
        String sql = """
                UPDATE articles
                SET title = ?, content = ?, category_id = ?, updated_at = ?
                WHERE id = ?
                """;

        return jdbcTemplate.update(
            sql,
            article.getTitle(),
            article.getContent(),
            article.getCategoryId(),
            article.getUpdatedAt(),
            id
        );
    }

    public int deleteById(Long id){
        String sql = """
                DELETE FROM articles
                WHERE id = ?
                """;

        return jdbcTemplate.update(sql, id);
    }
}
