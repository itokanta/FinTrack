package com.example.fintrack.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fintrack.domain.Asset;
import com.example.fintrack.domain.AssetCategory;
import com.example.fintrack.repository.AssetCategoryRepository;
import com.example.fintrack.repository.AssetRepository;

@Service
public class AssetCategoryService {
  private final AssetCategoryRepository assetCategoryRepository;
  private final AssetRepository assetRepository;

  public AssetCategoryService (AssetCategoryRepository assetCategoryRepository, AssetRepository assetRepository) {
    this.assetCategoryRepository = assetCategoryRepository;
    this.assetRepository = assetRepository;
  }

  @Transactional
  public void insert (AssetCategory category) {
    Long assetCategoryId = assetCategoryRepository.insert(category);
    
    Asset asset = new Asset();
    asset.setAssetCategoryId(assetCategoryId);
    asset.setUserId(category.getUserId());
    asset.setAmount(0L);

    assetRepository.insert(asset);
  }

  public List<AssetCategory> findByUserId(Long userId) {
    return assetCategoryRepository.findByUserId(userId);
  }

  @Transactional
  public void deleteById(Long id, Long userId) {
    Long targetCategoryAmount = assetRepository.getBalanceByCategory(userId, id);

    if (targetCategoryAmount != 0L) {
      throw new IllegalArgumentException();
    }
    assetRepository.deleteByAssetCategoryId(id, userId);
    assetCategoryRepository.deleteById(id, userId);
  }
}
