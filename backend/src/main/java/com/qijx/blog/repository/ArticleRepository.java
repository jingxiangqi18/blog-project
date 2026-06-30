package com.qijx.blog.repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import com.qijx.blog.dto.PageResponse;
import com.qijx.blog.entity.Article;

@Repository
public class ArticleRepository {

    private final JdbcTemplate jdbcTemplate;
    
    public ArticleRepository(JdbcTemplate jdbcTemplate){
        this.jdbcTemplate = jdbcTemplate;
    }

    public Article save(Article article){
        String sql = """
                INSERT INTO articles(title, content, category_id, author_id, created_at, updated_at)
                VALUES(?, ?, ?, ?, ?, ?)
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
            preparedStatement.setLong(4, article.getAuthorId());
            preparedStatement.setTimestamp(5, Timestamp.valueOf(article.getCreatedAt()));
            preparedStatement.setTimestamp(6, Timestamp.valueOf(article.getUpdatedAt()));

            return preparedStatement;
        }, keyHolder);

        Number key = keyHolder.getKey();

        if(key != null){
            article.setId(key.longValue());
        }

        return article;
    }

    public PageResponse<Article> findPage(int page, int size, String keyword, Long categoryId){
        StringBuilder whereSql = new StringBuilder(" WHERE 1 = 1 ");

        List<Object> params = new ArrayList<>();

        if(!keyword.isBlank()){
            whereSql.append("""
                AND (
                articles.title LIKE ?
                OR articles.content LIKE ?
                OR categories.name LIKE ?
                )
            """);

            String likeKeyword = "%" + keyword + "%";
            params.add(likeKeyword);
            params.add(likeKeyword);
            params.add(likeKeyword);
        }

        if(categoryId != null){
            whereSql.append(" AND articles.category_id = ? ");
            params.add(categoryId);
        }

        String countSql = """
                SELECT COUNT(*)
                FROM articles
                LEFT JOIN categories ON articles.category_id = categories.id
                """ + whereSql;

        Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());

        int offset = (page - 1) * size;

        String dataSql = """
                SELECT articles.id,
                       articles.title,
                       articles.content,
                       articles.category_id,
                       articles.author_id,
                       users.username AS author_name,
                       categories.name AS category_name,
                       articles.created_at,
                       articles.updated_at
                FROM articles
                LEFT JOIN categories ON articles.category_id = categories.id
                LEFT JOIN users ON articles.author_id = users.id
                """ + whereSql + """
                        ORDER BY articles.updated_at DESC, articles.id DESC
                        LIMIT ? OFFSET ?
                        """;

        params.add(size);
        params.add(offset);

        List<Article> content = jdbcTemplate.query(dataSql, this::mapRow, params.toArray());

        if(total == null){
            total = 0L;
        }

        return new PageResponse<>(content, page, size, total);
    }

    public Optional<Article> findById(Long id){
        String sql = """
                SELECT articles.id,
                       articles.title,
                       articles.content,
                       articles.category_id,
                       articles.author_id,
                       users.username AS author_name,
                       categories.name AS category_name,
                       articles.created_at,
                       articles.updated_at
                FROM articles
                LEFT JOIN categories ON articles.category_id = categories.id
                LEFT JOIN users ON articles.author_id = users.id
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
        Long categoryId = rs.getLong("category_id");
        article.setCategoryId(rs.wasNull() ? null : categoryId);
        Long authorId = rs.getLong("author_id");
        article.setAuthorId(rs.wasNull() ? null : authorId);
        article.setAuthorName(rs.getString("author_name"));
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

    public int clearCategoryId(Long categoryId){
        String sql = """
                UPDATE articles
                SET category_id = NULL
                WHERE category_id = ?
                """;

        return jdbcTemplate.update(sql, categoryId);
    }

    public List<Article> findByCategoryId(Long categoryId){
        String sql = """
                SELECT articles.id,
                       articles.title,
                       articles.content,
                       articles.category_id,
                       articles.author_id,
                       users.username AS author_name,
                       categories.name AS category_name,
                       articles.created_at,
                       articles.updated_at
                FROM articles
                LEFT JOIN categories ON articles.category_id = categories.id
                LEFT JOIN users ON articles.author_id = users.id
                WHERE articles.category_id = ?
                ORDER BY articles.id DESC
                """;

        return jdbcTemplate.query(sql, this::mapRow, categoryId);
    }
}
