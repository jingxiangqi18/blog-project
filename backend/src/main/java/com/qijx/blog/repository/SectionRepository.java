package com.qijx.blog.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import com.qijx.blog.entity.Section;

@Repository
public class SectionRepository {
    private final JdbcTemplate jdbcTemplate;

    public SectionRepository(JdbcTemplate jdbcTemplate){
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Section> findAll(boolean enabledOnly){
        String sql = "SELECT * FROM sections" + (enabledOnly ? " WHERE enabled = TRUE" : "")
            + " ORDER BY sort_order, id";
        return jdbcTemplate.query(sql, this::mapRow);
    }

    public Optional<Section> findById(Long id){
        return jdbcTemplate.query("SELECT * FROM sections WHERE id = ?", this::mapRow, id).stream().findFirst();
    }

    public Optional<Section> findBySlug(String slug){
        return jdbcTemplate.query("SELECT * FROM sections WHERE slug = ?", this::mapRow, slug).stream().findFirst();
    }

    public Section save(Section section){
        String sql = """
            INSERT INTO sections(slug, name, description, sort_order, enabled, created_at, updated_at)
            VALUES(?, ?, ?, ?, ?, ?, ?)
            """;
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, section.getSlug());
            statement.setString(2, section.getName());
            statement.setString(3, section.getDescription());
            statement.setInt(4, section.getSortOrder());
            statement.setBoolean(5, section.getEnabled());
            statement.setObject(6, section.getCreatedAt());
            statement.setObject(7, section.getUpdatedAt());
            return statement;
        }, keyHolder);
        section.setId(keyHolder.getKey().longValue());
        return section;
    }

    public int update(Long id, Section section){
        return jdbcTemplate.update("""
            UPDATE sections SET slug = ?, name = ?, description = ?, sort_order = ?, enabled = ?, updated_at = ?
            WHERE id = ?
            """, section.getSlug(), section.getName(), section.getDescription(), section.getSortOrder(),
            section.getEnabled(), section.getUpdatedAt(), id);
    }

    public int updateEnabled(Long id, boolean enabled, LocalDateTime updatedAt){
        return jdbcTemplate.update("UPDATE sections SET enabled = ?, updated_at = ? WHERE id = ?", enabled, updatedAt, id);
    }

    private Section mapRow(ResultSet rs, int rowNum) throws SQLException{
        Section section = new Section();
        section.setId(rs.getLong("id"));
        section.setSlug(rs.getString("slug"));
        section.setName(rs.getString("name"));
        section.setDescription(rs.getString("description"));
        section.setSortOrder(rs.getInt("sort_order"));
        section.setEnabled(rs.getBoolean("enabled"));
        section.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        section.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return section;
    }
}
