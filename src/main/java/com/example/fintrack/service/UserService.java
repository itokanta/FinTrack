package com.example.fintrack.service;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fintrack.domain.Asset;
import com.example.fintrack.domain.User;
import com.example.fintrack.repository.AssetRepository;
import com.example.fintrack.repository.UserRepository;

@Service
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AssetRepository assetRepository;
    
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, 
            AssetRepository assetRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.assetRepository = assetRepository;
    }
    
    /**
     * 新規ユーザーを登録
     */
    @Transactional
    public void createUser(String username, String rawPassword) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("ユーザー名は既に使用されています");
        }
        
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        User createdUser = userRepository.save(user);

        Asset asset = new Asset();
        asset.setAssetCategoryId(null);
        asset.setAmount(0L);
        asset.setUserId(createdUser.getId());
        assetRepository.insert(asset);
    }

    public Optional<User> findByUsername(String username){
        return userRepository.findByUsername(username);
    }
}

