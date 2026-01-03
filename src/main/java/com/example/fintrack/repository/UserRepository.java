package com.example.fintrack.repository;

import com.example.fintrack.domain.User;
import com.example.fintrack.repository.mapper.UserRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {
    
    private final JdbcTemplate jdbcTemplate;
    private final UserRowMapper userRowMapper = new UserRowMapper();
    
    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    public Optional<User> findById(Long id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        List<User> users = jdbcTemplate.query(sql, userRowMapper, id);
        if (users.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(users.get(0));
    }
    
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        List<User> users = jdbcTemplate.query(sql, userRowMapper, username);
        if (users.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(users.get(0));
    }
    
    public boolean existsByUsername(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, username);
        return count != null && count > 0;
    }
    
    public User save(User user) {
        if (user.getId() == null) {
            // INSERT
            String sql = "INSERT INTO users (username, password, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?) RETURNING id";
            LocalDateTime now = LocalDateTime.now();
            Long id = jdbcTemplate.queryForObject(sql, Long.class, 
                user.getUsername(), user.getPassword(), now, now);
            user.setId(id);
            user.setCreatedAt(now);
            user.setUpdatedAt(now);
        } else {
            // UPDATE
            String sql = "UPDATE users SET username = ?, password = ?, updated_at = ? WHERE id = ?";
            LocalDateTime now = LocalDateTime.now();
            jdbcTemplate.update(sql, user.getUsername(), user.getPassword(), now, user.getId());
            user.setUpdatedAt(now);
        }
        return user;
    }
}
