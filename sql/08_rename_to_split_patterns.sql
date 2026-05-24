-- ============================================
-- テーブル名の変更: 収支統合対応
-- income_split_patterns → split_patterns
-- split_incomes → split_transactions
-- ============================================

-- 1. income_split_patternsをsplit_patternsにリネーム
ALTER TABLE income_split_patterns RENAME TO split_patterns;

-- 2. split_incomesをsplit_transactionsにリネーム
ALTER TABLE split_incomes RENAME TO split_transactions;

-- 3. インデックス名の変更
ALTER INDEX idx_income_split_patterns_id RENAME TO idx_split_patterns_id;
ALTER INDEX idx_income_split_patterns_user_id RENAME TO idx_split_patterns_user_id;

ALTER INDEX idx_split_incomes_asset_category RENAME TO idx_split_transactions_asset_category;
ALTER INDEX idx_split_incomes_split_pattern RENAME TO idx_split_transactions_split_pattern;
ALTER INDEX idx_split_incomes_bank_transaction RENAME TO idx_split_transactions_bank_transaction;
ALTER INDEX idx_split_incomes_card_transaction RENAME TO idx_split_transactions_card_transaction;

-- 4. 制約名の変更
ALTER TABLE split_patterns RENAME CONSTRAINT income_split_patterns_pkey TO split_patterns_pkey;
ALTER TABLE split_patterns RENAME CONSTRAINT income_split_patterns_user_id_fkey TO split_patterns_user_id_fkey;
ALTER TABLE split_patterns RENAME CONSTRAINT income_split_patterns_asset_category_id_fkey TO split_patterns_asset_category_id_fkey;

-- チェック制約のリネーム（存在する場合のみ）
DO $$ 
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE table_name = 'split_patterns' 
        AND constraint_name = 'income_split_patterns_check'
    ) THEN
        ALTER TABLE split_patterns RENAME CONSTRAINT income_split_patterns_check TO split_patterns_check;
    END IF;
END $$;

-- split_transactionsのチェック制約のリネーム（存在する場合のみ）
DO $$ 
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE table_name = 'split_transactions' 
        AND constraint_name = 'chk_split_method'
    ) THEN
        ALTER TABLE split_transactions RENAME CONSTRAINT chk_split_method TO chk_split_transactions_method;
    END IF;
END $$;

-- 完了メッセージ
DO $$ 
BEGIN
    RAISE NOTICE 'テーブル名の変更が完了しました';
    RAISE NOTICE '  income_split_patterns → split_patterns';
    RAISE NOTICE '  split_incomes → split_transactions';
END $$;

