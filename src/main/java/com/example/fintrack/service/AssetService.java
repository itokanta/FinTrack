package com.example.fintrack.service;

import com.example.fintrack.domain.Asset;
import com.example.fintrack.domain.AssetCategory;
import com.example.fintrack.repository.AssetCategoryRepository;
import com.example.fintrack.repository.AssetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     * 資産カテゴリの残高を更新（差分を加算/減算）
     * 
     * @param userId ユーザーID
     * @param assetCategoryId 資産カテゴリID（NULLの場合は未分類）
     * @param amountDelta 増減額（プラスで増加、マイナスで減少）
     */
    @Transactional
    public void updateBalance(Long userId, Long assetCategoryId, Long amountDelta) {
        assetRepository.updateBalance(userId, assetCategoryId, amountDelta);
    }
    
    /**
     * 資産カテゴリの残高を設定（絶対値）
     * 
     * @param userId ユーザーID
     * @param assetCategoryId 資産カテゴリID（NULLの場合は未分類）
     * @param amount 設定する金額
     */
    @Transactional
    public void setBalance(Long userId, Long assetCategoryId, Long amount) {
        assetRepository.setBalance(userId, assetCategoryId, amount);
    }
    
    /**
     * 資産カテゴリ間で残高を移動
     * 
     * @param userId ユーザーID
     * @param fromCategoryId 移動元カテゴリID
     * @param toCategoryId 移動先カテゴリID
     * @param amount 移動する金額
     */
    @Transactional
    public void transferBalance(Long userId, Long fromCategoryId, Long toCategoryId, Long amount) {
        // 移動元から減算
        assetRepository.updateBalance(userId, fromCategoryId, -amount);
        // 移動先に加算
        assetRepository.updateBalance(userId, toCategoryId, amount);
    }
    
    /**
     * ユーザーの全資産情報を取得
     */
    public List<Asset> getUserAssets(Long userId) {
        return assetRepository.findByUserId(userId);
    }
    
    /**
     * 全資産情報を取得（管理者用）
     */
    public List<Asset> getAllAssets() {
        return assetRepository.findAll();
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
