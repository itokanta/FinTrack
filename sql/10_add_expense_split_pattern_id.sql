-- ============================================
-- 分割パターンID管理用のカラムを統合
-- bank_transactions, card_transactionsの
-- income_split_pattern_id + expense_split_pattern_id
-- → split_pattern_id に統合
-- ============================================

-- 1. bank_transactionsに統合カラムを追加
ALTER TABLE bank_transactions ADD COLUMN split_pattern_id INTEGER;

-- 2. データを統合（収入の分割パターンIDをコピー）
UPDATE bank_transactions 
SET split_pattern_id = income_split_pattern_id 
WHERE income_split_pattern_id IS NOT NULL;

-- 3. インデックスを追加
CREATE INDEX idx_bank_transactions_split_pattern ON bank_transactions(split_pattern_id);

-- 4. card_transactionsに統合カラムを追加
ALTER TABLE card_transactions ADD COLUMN split_pattern_id INTEGER;

-- 5. データを統合（収入の分割パターンIDをコピー）
UPDATE card_transactions 
SET split_pattern_id = income_split_pattern_id 
WHERE income_split_pattern_id IS NOT NULL;

-- 6. インデックスを追加
CREATE INDEX idx_card_transactions_split_pattern ON card_transactions(split_pattern_id);

-- income_split_pattern_idカラムを削除
ALTER TABLE bank_transactions DROP COLUMN IF EXISTS income_split_pattern_id;
ALTER TABLE card_transactions DROP COLUMN IF EXISTS income_split_pattern_id;

-- 注意: income_split_pattern_idとexpense_split_pattern_idは
-- 後方互換性のため残しますが、新規開発ではsplit_pattern_idを使用してください

-- 完了メッセージ
DO $$ 
BEGIN
    RAISE NOTICE '分割パターンID管理用カラムの統合が完了しました';
    RAISE NOTICE '  bank_transactions.split_pattern_id';
    RAISE NOTICE '  card_transactions.split_pattern_id';
    RAISE NOTICE '';
    RAISE NOTICE '値の意味:';
    RAISE NOTICE '  NULL: 未分配';
    RAISE NOTICE '  >= 1: パターン分割 (split_patternsテーブルのpattern_id)';
    RAISE NOTICE '  0: 単一カテゴリ分割';
    RAISE NOTICE '  -1: 手動分割';
    RAISE NOTICE '';
    RAISE NOTICE '注意: 古いカラムを削除するには11番のマイグレーションを実行してください';
END $$;

