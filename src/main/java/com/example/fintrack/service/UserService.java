package com.example.fintrack.service;

import com.example.fintrack.domain.User;
import com.example.fintrack.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    /**
     * ユーザー名でユーザーを検索
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    /**
     * ユーザーIDでユーザーを検索
     */
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
    
    /**
     * 新規ユーザーを登録
     */
    @Transactional
    public User createUser(String username, String rawPassword) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("ユーザー名は既に使用されています");
        }
        
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        
        return userRepository.save(user);
    }
}

