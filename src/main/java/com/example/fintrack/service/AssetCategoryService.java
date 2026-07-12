package com.example.fintrack.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fintrack.domain.Asset;
import com.example.fintrack.domain.AssetCategory;
import com.example.fintrack.repository.AssetCategoryRepository;
import com.example.fintrack.repository.AssetRepository;

/**
 * 資産カテゴリの登録・削除を扱うサービス。
 *
 * @author kanta ito
 */
@Service
public class AssetCategoryService {
  private final AssetCategoryRepository assetCategoryRepository;
  private final AssetRepository assetRepository;

  public AssetCategoryService (AssetCategoryRepository assetCategoryRepository, AssetRepository assetRepository) {
    this.assetCategoryRepository = assetCategoryRepository;
    this.assetRepository = assetRepository;
  }

  /**
   * 資産カテゴリを新規登録する。残高0の資産レコードも一緒に作る。
   *
   * @param category 登録するカテゴリ
   */
  @Transactional
  public void insert (AssetCategory category) {
    Long assetCategoryId = assetCategoryRepository.insert(category);
    
    Asset asset = new Asset();
    asset.setAssetCategoryId(assetCategoryId);
    asset.setUserId(category.getUserId());
    asset.setAmount(0L);

    assetRepository.insert(asset);
  }

  /**
   * ユーザーの資産カテゴリ一覧を取得する。
   *
   * @param userId ユーザーID
   * @return カテゴリの一覧
   */
  public List<AssetCategory> findByUserId(Long userId) {
    return assetCategoryRepository.findByUserId(userId);
  }

  /**
   * 資産カテゴリを削除する。残高が0でない場合は削除できない。
   *
   * @param id     カテゴリID
   * @param userId ユーザーID
   * @throws IllegalArgumentException 残高が0でない場合
   */
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
