-- ============================================
-- assetsテーブルにuser_idカラムを追加
-- ユーザー固有の資産管理を可能にする
-- ============================================

-- 1. assetsテーブルにuser_idカラムを追加
ALTER TABLE assets 
ADD COLUMN user_id INTEGER;

-- 2. 既存データのuser_idを設定（asset_category_idから取得）
UPDATE assets 
SET user_id = (
    SELECT user_id 
    FROM asset_categories 
    WHERE asset_categories.id = assets.asset_category_id
)
WHERE asset_category_id IS NOT NULL;

-- 3. 未分類の資産については、関連する取引からuser_idを推測
-- （この時点で未分類資産がある場合は手動で設定が必要）
-- 注意: 未分類資産が複数ユーザーに分散している場合は、手動での調整が必要です

-- 4. user_idをNOT NULL制約に変更（既存データが設定された後）
-- ALTER TABLE assets 
-- ALTER COLUMN user_id SET NOT NULL;

-- 5. 外部キー制約を追加
ALTER TABLE assets 
ADD CONSTRAINT assets_user_id_fkey 
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- 6. インデックスを追加（パフォーマンス向上）
CREATE INDEX idx_assets_user_id ON assets(user_id);

-- 7. 複合ユニーク制約を追加（1ユーザー・1カテゴリにつき1レコード）
-- 既存のユニーク制約を削除
ALTER TABLE assets DROP CONSTRAINT IF EXISTS assets_asset_category_id_key;

-- 新しい複合ユニーク制約を追加
ALTER TABLE assets 
ADD CONSTRAINT assets_user_category_unique 
UNIQUE (user_id, asset_category_id);

-- 注意事項:
-- - 既存データがある場合、user_idがNULLのレコードが残る可能性があります
-- - その場合は手動でuser_idを設定してから、NOT NULL制約を有効にしてください
-- - コマンド: ALTER TABLE assets ALTER COLUMN user_id SET NOT NULL;

