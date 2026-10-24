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
                INSERT INTO categories(name, section_id)
                VALUES(?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement preparedStatement = connection.prepareStatement(
                sql,
                Statement.RETURN_GENERATED_KEYS
            );

            preparedStatement.setString(1, category.getName());
            preparedStatement.setObject(2, category.getSectionId());

            return preparedStatement;
        }, keyHolder);

        Number key = keyHolder.getKey();

        if(key != null){
            category.setId(key.longValue());
        }

        return category;
    }

    public List<Category> findAll(){
        return findAll(null);
    }

    public List<Category> findAll(Long sectionId){
        String sql = baseSelect();
        if(sectionId == null){
            return jdbcTemplate.query(sql + " ORDER BY categories.id DESC", this::mapRow);
        }
        return jdbcTemplate.query(sql + " WHERE categories.section_id = ? OR categories.section_id IS NULL ORDER BY categories.id DESC",
            this::mapRow, sectionId);
    }

    public Optional<Category> findById(Long id){
        String sql = baseSelect() + " WHERE categories.id = ?";

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
        category.setSectionId(rs.getObject("section_id", Long.class));
        category.setSectionSlug(rs.getString("section_slug"));
        category.setSectionName(rs.getString("section_name"));

        return category;
    }

    public int update(Long id, Category category){
        String sql = """
                UPDATE categories
                SET name = ?, section_id = ?
                WHERE id = ?
                """;

        return jdbcTemplate.update(
            sql,
            category.getName(),
            category.getSectionId(),
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

    public boolean hasConflictingContent(Long categoryId, Long sectionId){
        Long count = jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM articles
            WHERE category_id = ? AND section_id IS NOT NULL AND section_id <> ?
            """, Long.class, categoryId, sectionId);
        Long seriesCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM series WHERE category_id = ? AND section_id <> ?", Long.class, categoryId, sectionId);
        return count > 0 || seriesCount > 0;
    }

    private String baseSelect(){
        return """
            SELECT categories.*, sections.slug AS section_slug, sections.name AS section_name
            FROM categories LEFT JOIN sections ON categories.section_id = sections.id
            """;
    }
}
