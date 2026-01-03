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
            Long balance = assetRepository.getBalanceByCategory(category.getId());
            balances.put(category, balance);
        }
        
        return balances;
    }
    
    /**
     * 総資産を取得
     */
    public Long getTotalBalance() {
        List<Asset> allAssets = assetRepository.findAll();
        return allAssets.stream()
                .mapToLong(Asset::getAmount)
                .sum();
    }
    
    /**
     * 資産カテゴリ未設定の合計金額を取得
     */
    public Long getUncategorizedBalance() {
        return assetRepository.getUncategorizedBalance();
    }
    
    /**
     * 資産カテゴリの残高を更新（差分を加算/減算）
     * 
     * @param assetCategoryId 資産カテゴリID（NULLの場合は未分類）
     * @param amountDelta 増減額（プラスで増加、マイナスで減少）
     */
    @Transactional
    public void updateBalance(Long assetCategoryId, Long amountDelta) {
        assetRepository.updateBalance(assetCategoryId, amountDelta);
    }
    
    /**
     * 資産カテゴリの残高を設定（絶対値）
     * 
     * @param assetCategoryId 資産カテゴリID（NULLの場合は未分類）
     * @param amount 設定する金額
     */
    @Transactional
    public void setBalance(Long assetCategoryId, Long amount) {
        assetRepository.setBalance(assetCategoryId, amount);
    }
    
    /**
     * 資産カテゴリ間で残高を移動
     * 
     * @param fromCategoryId 移動元カテゴリID
     * @param toCategoryId 移動先カテゴリID
     * @param amount 移動する金額
     */
    @Transactional
    public void transferBalance(Long fromCategoryId, Long toCategoryId, Long amount) {
        // 移動元から減算
        assetRepository.updateBalance(fromCategoryId, -amount);
        // 移動先に加算
        assetRepository.updateBalance(toCategoryId, amount);
    }
    
    /**
     * 全資産情報を取得
     */
    public List<Asset> getAllAssets() {
        return assetRepository.findAll();
    }
}
