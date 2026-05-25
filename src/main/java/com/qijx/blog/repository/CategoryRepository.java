package com.qijx.blog.repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.qijx.blog.entity.Category;

@Repository
public class CategoryRepository {
    private final JdbcTemplate jdbcTemplate;

    public CategoryRepository(JdbcTemplate jdbcTemplate){
        this.jdbcTemplate = jdbcTemplate;
    }

    public Category save(Category category){
        String sql = """
                INSERT INTO categories(name)
                VALUES(?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement preparedStatement = connection.prepareStatement(
                sql,
                Statement.RETURN_GENERATED_KEYS
            );

            preparedStatement.setString(1, category.getName());

            return preparedStatement;
        }, keyHolder);

        Number key = keyHolder.getKey();

        if(key != null){
            category.setId(key.longValue());
        }

        return category;
    }

    public List<Category> findAll(){
        String sql = """
                SELECT * 
                FROM categories
                ORDER BY id DESC
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Category category = new Category();

            category.setId(rs.getLong("id"));
            category.setName(rs.getString("name"));

            return category;
        });
    }

    public Optional<Category> findById(Long id){
        String sql = """
                SELECT id, name
                FROM categories
                WHERE id = ?
                """;

        List<Category> categories = jdbcTemplate.query(sql, this::mapRow, id);

        if(categories.isEmpty()){
            return Optional.empty();
        }

        return Optional.of(categories.get(0));
    }

    private Category mapRow(ResultSet rs, int rowNum) throws SQLException{
        Category category = new Category();

        category.setId(rs.getLong("id"));
        category.setName(rs.getString("name"));

        return category;
    }

    public int update(Long id, Category category){
        String sql = """
                UPDATE categories
                SET name = ?
                WHERE id = ?
                """;

        return jdbcTemplate.update(
            sql,
            category.getName(),
            id
        );
    }

    public int deleteById(Long id){
        String sql = """
                DELETE FROM categories
                WHERE id = ?
                """;

        return jdbcTemplate.update(sql, id);
    }
}
