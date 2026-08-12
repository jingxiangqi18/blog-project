package com.qijx.blog.repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.qijx.blog.entity.KnowledgeCard;

@Repository
public class KnowledgeCardRepository {
    private final JdbcTemplate jdbcTemplate;

    public KnowledgeCardRepository(JdbcTemplate jdbcTemplate){
        this.jdbcTemplate = jdbcTemplate;
    }

    public KnowledgeCard save(KnowledgeCard knowledgeCard){
        String sql = """
                INSERT INTO knowledge_cards(title, summary, content, created_by, created_at, updated_at)
                VALUES(?, ?, ?, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, knowledgeCard.getTitle());
            statement.setString(2, knowledgeCard.getSummary());
            statement.setString(3, knowledgeCard.getContent());
            statement.setLong(4, knowledgeCard.getCreatedBy());
            statement.setTimestamp(5, Timestamp.valueOf(knowledgeCard.getCreatedAt()));
            statement.setTimestamp(6, Timestamp.valueOf(knowledgeCard.getUpdatedAt()));
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if(key != null){
            knowledgeCard.setId(key.longValue());
        }
        return knowledgeCard;
    }

    public List<KnowledgeCard> findAll(String keyword){
        String sql = """
                SELECT knowledge_cards.id,
                       knowledge_cards.title,
                       knowledge_cards.summary,
                       knowledge_cards.content,
                       knowledge_cards.created_by,
                       users.username AS created_by_name,
                       knowledge_cards.created_at,
                       knowledge_cards.updated_at
                FROM knowledge_cards
                LEFT JOIN users ON knowledge_cards.created_by = users.id
                WHERE knowledge_cards.title LIKE ?
                   OR knowledge_cards.summary LIKE ?
                ORDER BY knowledge_cards.updated_at DESC, knowledge_cards.id DESC
                LIMIT 50
                """;
        String likeKeyword = "%" + keyword + "%";
        return jdbcTemplate.query(sql, this::mapRow, likeKeyword, likeKeyword);
    }

    public Optional<KnowledgeCard> findById(Long id){
        String sql = """
                SELECT knowledge_cards.id,
                       knowledge_cards.title,
                       knowledge_cards.summary,
                       knowledge_cards.content,
                       knowledge_cards.created_by,
                       users.username AS created_by_name,
                       knowledge_cards.created_at,
                       knowledge_cards.updated_at
                FROM knowledge_cards
                LEFT JOIN users ON knowledge_cards.created_by = users.id
                WHERE knowledge_cards.id = ?
                """;
        List<KnowledgeCard> cards = jdbcTemplate.query(sql, this::mapRow, id);
        return cards.isEmpty() ? Optional.empty() : Optional.of(cards.get(0));
    }

    public int update(Long id, KnowledgeCard knowledgeCard){
        String sql = """
                UPDATE knowledge_cards
                SET title = ?, summary = ?, content = ?, updated_at = ?
                WHERE id = ?
                """;
        return jdbcTemplate.update(
            sql,
            knowledgeCard.getTitle(),
            knowledgeCard.getSummary(),
            knowledgeCard.getContent(),
            knowledgeCard.getUpdatedAt(),
            id
        );
    }

    public int deleteById(Long id){
        return jdbcTemplate.update("DELETE FROM knowledge_cards WHERE id = ?", id);
    }

    private KnowledgeCard mapRow(ResultSet resultSet, int rowNum) throws SQLException{
        KnowledgeCard knowledgeCard = new KnowledgeCard();
        knowledgeCard.setId(resultSet.getLong("id"));
        knowledgeCard.setTitle(resultSet.getString("title"));
        knowledgeCard.setSummary(resultSet.getString("summary"));
        knowledgeCard.setContent(resultSet.getString("content"));
        knowledgeCard.setCreatedBy(resultSet.getLong("created_by"));
        knowledgeCard.setCreatedByName(resultSet.getString("created_by_name"));
        knowledgeCard.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());
        knowledgeCard.setUpdatedAt(resultSet.getTimestamp("updated_at").toLocalDateTime());
        return knowledgeCard;
    }
}
