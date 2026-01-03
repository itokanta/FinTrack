-- ============================================
-- 資産管理の設計変更
-- assetsテーブルを残高管理専用に変更
-- 取引テーブルにカテゴリ情報を追加
-- ============================================

-- 1. bank_transactionsテーブルにカテゴリ情報を追加
ALTER TABLE bank_transactions 
ADD COLUMN asset_category_id INTEGER REFERENCES asset_categories(id) ON DELETE SET NULL,
ADD COLUMN expense_category_id INTEGER REFERENCES expense_categories(id) ON DELETE SET NULL,
ADD COLUMN income_split_pattern_id INTEGER;

-- 2. card_transactionsテーブルにカテゴリ情報を追加
ALTER TABLE card_transactions 
ADD COLUMN asset_category_id INTEGER REFERENCES asset_categories(id) ON DELETE SET NULL,
ADD COLUMN expense_category_id INTEGER REFERENCES expense_categories(id) ON DELETE SET NULL,
ADD COLUMN income_split_pattern_id INTEGER;

-- 3. インデックス追加（パフォーマンス向上）
CREATE INDEX idx_bank_transactions_asset_category ON bank_transactions(asset_category_id);
CREATE INDEX idx_bank_transactions_expense_category ON bank_transactions(expense_category_id);
CREATE INDEX idx_bank_transactions_income_split_pattern ON bank_transactions(income_split_pattern_id);
CREATE INDEX idx_card_transactions_asset_category ON card_transactions(asset_category_id);
CREATE INDEX idx_card_transactions_expense_category ON card_transactions(expense_category_id);
CREATE INDEX idx_card_transactions_income_split_pattern ON card_transactions(income_split_pattern_id);

-- 4. 既存のassetsテーブルをバックアップ
CREATE TABLE assets_backup AS SELECT * FROM assets;

-- 5. assetsテーブルの不要な外部キー制約を削除
ALTER TABLE assets DROP CONSTRAINT IF EXISTS assets_bank_transaction_id_fkey;
ALTER TABLE assets DROP CONSTRAINT IF EXISTS assets_card_transaction_id_fkey;
ALTER TABLE assets DROP CONSTRAINT IF EXISTS assets_expense_category_id_fkey;

-- 6. assetsテーブルから取引関連のカラムを削除
ALTER TABLE assets DROP COLUMN IF EXISTS bank_transaction_id;
ALTER TABLE assets DROP COLUMN IF EXISTS card_transaction_id;
ALTER TABLE assets DROP COLUMN IF EXISTS expense_category_id;

-- 7. assetsテーブルに一意制約を追加（1カテゴリ1レコード）
-- まず既存の重複データを集約
CREATE TEMP TABLE assets_aggregated AS
SELECT 
    MIN(id) as id,
    asset_category_id,
    SUM(amount) as amount,
    MAX(created_at) as created_at,
    MAX(updated_at) as updated_at
FROM assets
GROUP BY asset_category_id;

-- 既存のassetsテーブルをクリア
TRUNCATE TABLE assets;

-- 集約したデータを戻す
INSERT INTO assets (id, asset_category_id, amount, created_at, updated_at)
SELECT id, asset_category_id, amount, created_at, updated_at
FROM assets_aggregated;

-- 一意制約を追加
ALTER TABLE assets ADD CONSTRAINT assets_asset_category_id_unique UNIQUE (asset_category_id);

-- 8. assetsテーブルのインデックスを再作成
DROP INDEX IF EXISTS idx_assets_bank_transaction;
DROP INDEX IF EXISTS idx_assets_card_transaction;
DROP INDEX IF EXISTS idx_assets_expense_category;

-- asset_category_idのインデックスは一意制約で自動作成されるため不要

-- ============================================
-- データ移行の説明
-- ============================================
-- 
-- 【重要】このスクリプト実行後、以下の手動作業が必要です：
--
-- 1. 既存の取引データにカテゴリ情報を設定
--    - assets_backupテーブルを参照して、bank_transactions/card_transactionsに
--      asset_category_idを設定してください
--
-- 2. 収入の分割情報を設定
--    - split_incomesテーブルを参照して、該当する取引に
--      income_split_pattern_idを設定してください
--
-- 3. assetsテーブルの残高を再計算
--    - 各asset_categoryごとの正しい残高を計算して更新してください
--
-- ============================================

-- 参考: 既存データからカテゴリ情報を移行するクエリ例
-- 
-- -- 銀行取引にasset_category_idを設定
-- UPDATE bank_transactions bt
-- SET asset_category_id = ab.asset_category_id
-- FROM assets_backup ab
-- WHERE ab.bank_transaction_id = bt.id
--   AND ab.asset_category_id IS NOT NULL;
--
-- -- カード取引にasset_category_idを設定
-- UPDATE card_transactions ct
-- SET asset_category_id = ab.asset_category_id
-- FROM assets_backup ab
-- WHERE ab.card_transaction_id = ct.id
--   AND ab.asset_category_id IS NOT NULL;
--
-- -- 収入取引にincome_split_pattern_idを設定
-- UPDATE bank_transactions bt
-- SET income_split_pattern_id = si.split_pattern_id
-- FROM split_incomes si
-- WHERE si.bank_transaction_id = bt.id
--   AND bt.transaction_type = 'INCOME';
--
-- UPDATE card_transactions ct
-- SET income_split_pattern_id = si.split_pattern_id
-- FROM split_incomes si
-- WHERE si.card_transaction_id = ct.id
--   AND ct.transaction_type = 'INCOME';

