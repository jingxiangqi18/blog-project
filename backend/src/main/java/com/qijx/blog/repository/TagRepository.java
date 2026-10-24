package com.qijx.blog.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import com.qijx.blog.entity.Article;
import com.qijx.blog.entity.Tag;

@Repository
public class TagRepository {
    private final JdbcTemplate jdbcTemplate;

    public TagRepository(JdbcTemplate jdbcTemplate){
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Tag> findAll(String keyword){
        return jdbcTemplate.query("SELECT * FROM tags WHERE name LIKE ? OR slug LIKE ? ORDER BY name, id",
            this::mapRow, "%" + keyword + "%", "%" + keyword + "%");
    }

    public Optional<Tag> findById(Long id){
        return jdbcTemplate.query("SELECT * FROM tags WHERE id = ?", this::mapRow, id).stream().findFirst();
    }

    public Optional<Tag> findBySlug(String slug){
        return jdbcTemplate.query("SELECT * FROM tags WHERE slug = ?", this::mapRow, slug).stream().findFirst();
    }

    public Tag save(Tag tag){
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(
                "INSERT INTO tags(name, slug, description, created_at) VALUES(?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, tag.getName());
            statement.setString(2, tag.getSlug());
            statement.setString(3, tag.getDescription());
            statement.setObject(4, tag.getCreatedAt());
            return statement;
        }, keyHolder);
        tag.setId(keyHolder.getKey().longValue());
        return tag;
    }

    public int update(Long id, Tag tag){
        return jdbcTemplate.update("UPDATE tags SET name = ?, slug = ?, description = ? WHERE id = ?",
            tag.getName(), tag.getSlug(), tag.getDescription(), id);
    }

    public int deleteById(Long id){
        return jdbcTemplate.update("DELETE FROM tags WHERE id = ?", id);
    }

    public long countByIds(List<Long> ids){
        if(ids.isEmpty()){
            return 0;
        }
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tags WHERE id IN (" + placeholders(ids.size()) + ")",
            Long.class, ids.toArray());
    }

    public void updateArticleTags(Long articleId, List<Long> tagIds){
        List<Long> existing = jdbcTemplate.queryForList("SELECT tag_id FROM article_tags WHERE article_id = ? FOR UPDATE", Long.class, articleId);
        var desired = new HashSet<>(tagIds);
        var previous = new HashSet<>(existing);
        for(Long tagId : existing){
            if(!desired.contains(tagId)){
                jdbcTemplate.update("DELETE FROM article_tags WHERE article_id = ? AND tag_id = ?", articleId, tagId);
            }
        }
        for(Long tagId : tagIds){
            if(!previous.contains(tagId)){
                jdbcTemplate.update("INSERT INTO article_tags(article_id, tag_id) VALUES(?, ?)", articleId, tagId);
            }
        }
    }

    public void populateTags(List<Article> articles){
        if(articles.isEmpty()){
            return;
        }
        Map<Long, List<Tag>> tagsByArticle = new HashMap<>();
        String sql = """
            SELECT tags.*, article_tags.article_id FROM article_tags
            JOIN tags ON tags.id = article_tags.tag_id
            WHERE article_tags.article_id IN (
            """ + placeholders(articles.size()) + ") ORDER BY tags.name, tags.id";
        jdbcTemplate.query(sql, rs -> {
            tagsByArticle.computeIfAbsent(rs.getLong("article_id"), key -> new ArrayList<>()).add(mapRow(rs, 0));
        }, articles.stream().map(Article::getId).toArray());
        for(Article article : articles){
            List<Tag> tags = tagsByArticle.getOrDefault(article.getId(), List.of());
            article.setTags(tags);
            article.setTagIds(tags.stream().map(Tag::getId).toList());
        }
    }

    private String placeholders(int size){
        return String.join(",", Collections.nCopies(size, "?"));
    }

    private Tag mapRow(ResultSet rs, int rowNum) throws SQLException{
        Tag tag = new Tag();
        tag.setId(rs.getLong("id"));
        tag.setName(rs.getString("name"));
        tag.setSlug(rs.getString("slug"));
        tag.setDescription(rs.getString("description"));
        tag.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return tag;
    }
}
