package com.example.fintrack.repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.fintrack.domain.User;
import org.springframework.jdbc.core.RowMapper;

@Repository
public class UserRepository {

    private static final RowMapper<User> ROW_MAPPER = (rs, rowNum) -> {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        user.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return user;
    };

    private final NamedParameterJdbcTemplate template;

    public UserRepository(NamedParameterJdbcTemplate template) {
        this.template = template;
    }

    public Optional<User> findById(Long id) {
        String sql = "SELECT id, username, password, created_at, updated_at FROM users WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource("id", id);
        List<User> users = template.query(sql, params, ROW_MAPPER);
        if (users.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(users.get(0));
    }

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT id, username, password, created_at, updated_at FROM users WHERE username = :username";
        MapSqlParameterSource params = new MapSqlParameterSource("username", username);
        List<User> users = template.query(sql, params, ROW_MAPPER);
        if (users.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(users.get(0));
    }

    public boolean existsByUsername(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = :username";
        MapSqlParameterSource params = new MapSqlParameterSource("username", username);
        Integer count = template.queryForObject(sql, params, Integer.class);
        return count != null && count > 0;
    }

    public User save(User user) {
        if (user.getId() == null) {
            String sql = "INSERT INTO users (username, password) " +
                         "VALUES (:username, :password) RETURNING id, created_at, updated_at";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("username", user.getUsername())
                    .addValue("password", user.getPassword());
            Map<String, Object> result = template.queryForMap(sql, params);
            user.setId(((Number) result.get("id")).longValue());
            user.setCreatedAt(((Timestamp) result.get("created_at")).toLocalDateTime());
            user.setUpdatedAt(((Timestamp) result.get("updated_at")).toLocalDateTime());
        } else {
            String sql = "UPDATE users SET username = :username, password = :password WHERE id = :id RETURNING updated_at";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("username", user.getUsername())
                    .addValue("password", user.getPassword())
                    .addValue("id", user.getId());
            Map<String, Object> result = template.queryForMap(sql, params);
            user.setUpdatedAt(((Timestamp) result.get("updated_at")).toLocalDateTime());
        }
        return user;
    }
}
