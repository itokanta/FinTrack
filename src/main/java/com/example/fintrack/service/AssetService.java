package com.example.fintrack.service;

import com.example.fintrack.domain.AssetCategory;
import com.example.fintrack.repository.AssetCategoryRepository;
import com.example.fintrack.repository.AssetRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 資産カテゴリごとの残高と総資産の集計を扱うサービス。
 *
 * @author kanta ito
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
     * 資産カテゴリごとの残高を取得する。
     *
     * @param userId ユーザーID
     * @return カテゴリをキー、残高を値とするマップ
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
     * 総資産（全カテゴリ合計）を取得する。
     *
     * @param userId ユーザーID
     * @return 総資産額
     */
    public Long getTotalBalance(Long userId) {
        return assetRepository.getTotalBalance(userId);
    }
    
    /**
     * 未分類（カテゴリ未設定）の残高を取得する。
     *
     * @param userId ユーザーID
     * @return 未分類の残高
     */
    public Long getUncategorizedBalance(Long userId) {
        return assetRepository.getUncategorizedBalance(userId);
    }

    /**
     * 確定済み総資産を取得する（当月クレジットカード取引は除外）。
     *
     * @param userId ユーザーID
     * @return 確定済みの総資産額
     */
    public Long getConfirmedTotalBalance(Long userId) {
        return assetRepository.getConfirmedTotalBalance(userId);
    }
    
    /**
     * カテゴリ別の確定済み残高を取得する（当月クレジットカード取引は除外）。
     *
     * @param userId ユーザーID
     * @return カテゴリをキー、確定済み残高を値とするマップ
     */
    public Map<AssetCategory, Long> getConfirmedBalanceByCategory(Long userId) {
        return assetRepository.getConfirmedBalanceByCategory(userId);
    }
    
    /**
     * 未分類の確定済み残高を取得する（当月クレジットカード取引は除外）。
     *
     * @param userId ユーザーID
     * @return 未分類の確定済み残高
     */
    public Long getConfirmedUncategorizedBalance(Long userId) {
        return assetRepository.getConfirmedUncategorizedBalance(userId);
    }
}
