package com.example.fintrack.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fintrack.domain.Asset;
import com.example.fintrack.domain.User;
import com.example.fintrack.repository.AssetRepository;
import com.example.fintrack.repository.UserRepository;

/**
 * ユーザー登録とユーザー情報の取得を扱うサービス。
 *
 * @author kanta ito
 */
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
     * 新規ユーザーを登録する。未分類資産（残高0）も一緒に作る。
     *
     * @param username    ユーザー名
     * @param rawPassword 平文パスワード（BCryptでハッシュ化して保存）
     * @throws IllegalArgumentException ユーザー名が既に使われている場合
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

    /**
     * ユーザー名からユーザーIDを取得する。
     *
     * @param username ユーザー名
     * @return ユーザーID
     * @throws IllegalStateException ユーザーが見つからない場合
     */
    public Long findUserIdByUsername(String username){
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("ユーザーが見つかりません"))
                .getId();
    }
}

