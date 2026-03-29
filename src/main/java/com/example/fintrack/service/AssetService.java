package com.example.fintrack.service;

import com.example.fintrack.domain.AssetCategory;
import com.example.fintrack.repository.AssetCategoryRepository;
import com.example.fintrack.repository.AssetRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 資産管理サービス
 * 各資産カテゴリの残高を管理する
 */
@Service
public class AssetService {
    
    private final AssetRepository assetRepository;
    private final AssetCategoryRepository assetCategoryRepository;
    
    public AssetService(AssetRepository assetRepository,
                       AssetCategoryRepository assetCategoryRepository) {
        this.assetRepository = assetRepository;
        this.assetCategoryRepository = assetCategoryRepository;
    }
    
    /**
     * 資産カテゴリごとの残高を取得
     */
    public Map<AssetCategory, Long> getBalanceByCategory(Long userId) {
        List<AssetCategory> categories = assetCategoryRepository.findByUserId(userId);
        Map<AssetCategory, Long> balances = new HashMap<>();
        
        for (AssetCategory category : categories) {
            Long balance = assetRepository.getBalanceByCategory(userId, category.getId());
            balances.put(category, balance);
        }
        
        return balances;
    }
    
    /**
     * 総資産を取得（ユーザー固有）
     */
    public Long getTotalBalance(Long userId) {
        return assetRepository.getTotalBalance(userId);
    }
    
    /**
     * 資産カテゴリ未設定の合計金額を取得（ユーザー固有）
     */
    public Long getUncategorizedBalance(Long userId) {
        return assetRepository.getUncategorizedBalance(userId);
    }

    /**
     * 確定済み総資産を取得（当月のクレジットカード取引を除外）
     */
    public Long getConfirmedTotalBalance(Long userId) {
        return assetRepository.getConfirmedTotalBalance(userId);
    }
    
    /**
     * カテゴリ別の確定済み残高を取得（当月のクレジットカード取引を除外）
     */
    public Map<AssetCategory, Long> getConfirmedBalanceByCategory(Long userId) {
        return assetRepository.getConfirmedBalanceByCategory(userId);
    }
    
    /**
     * 未分類の確定済み残高を取得（当月のクレジットカード取引を除外）
     */
    public Long getConfirmedUncategorizedBalance(Long userId) {
        return assetRepository.getConfirmedUncategorizedBalance(userId);
    }
}
