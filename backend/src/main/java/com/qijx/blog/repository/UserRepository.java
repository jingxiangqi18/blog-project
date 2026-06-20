package com.qijx.blog.repository;

import java.util.Optional;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.qijx.blog.entity.Role;
import com.qijx.blog.entity.User;
@Repository
public class UserRepository {
    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate){
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<User> findByUserName(String username){
        String sql = """
                SELECT id, username, password_hash, role, enabled, created_at, updated_at
                FROM users
                WHERE username = ?
                """;

        List<User> users = jdbcTemplate.query(sql, this::mapRow, username);

        if(users.isEmpty()){
            return Optional.empty();
        }

        return Optional.of(users.get(0));
    }

    private User mapRow(ResultSet rs, int rowNum) throws SQLException{
        User user = new User();

        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRole(Role.valueOf(rs.getString("role")));
        user.setEnabled(rs.getBoolean("enabled"));
        user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        user.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());

        return user;
    }
}
