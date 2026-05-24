-- ============================================
-- 収支統合対応: カラムの追加（改訂版v3）
-- transaction_type: 収入/支出の判別
-- split_type: 割合/金額指定の判別
-- split_amount: 金額指定時の金額
--
-- 【設計方針】
-- - 収入も支出もasset_categoriesを使用
-- - expense_categoriesは使用しない（expense_category_idも不要）
-- - asset_category_idは常にNOT NULL
-- - プライマリキーをSERIAL主キーに変更
-- ============================================

-- 1. 既存のプライマリキーを削除
ALTER TABLE split_patterns DROP CONSTRAINT split_patterns_pkey;

-- 2. 新しいプライマリキー用のカラムを追加（既存データには自動採番される）
ALTER TABLE split_patterns ADD COLUMN new_id SERIAL;

-- 4. 既存のインデックスを削除
DROP INDEX IF EXISTS idx_split_patterns_id;

-- 5. idカラムをpattern_idにリネーム
ALTER TABLE split_patterns RENAME COLUMN id TO pattern_id;

-- 6. new_idをidにリネーム
ALTER TABLE split_patterns RENAME COLUMN new_id TO id;

-- 7. 新しいプライマリキーを設定
ALTER TABLE split_patterns ADD PRIMARY KEY (id);

-- 8. インデックスを作成
DROP INDEX IF EXISTS idx_split_patterns_pattern_id;
CREATE INDEX idx_split_patterns_pattern_id ON split_patterns(user_id, pattern_id);

-- 5. split_patternsテーブルにtransaction_typeカラムを追加
ALTER TABLE split_patterns ADD COLUMN transaction_type VARCHAR(10);

-- 6. 既存データは全て'INCOME'として設定
UPDATE split_patterns SET transaction_type = 'INCOME';

-- 7. transaction_typeをNOT NULL制約に変更
ALTER TABLE split_patterns ALTER COLUMN transaction_type SET NOT NULL;

-- 8. transaction_typeにチェック制約を追加
ALTER TABLE split_patterns 
ADD CONSTRAINT chk_split_patterns_transaction_type 
    CHECK (transaction_type IN ('INCOME', 'EXPENSE'));

-- 9. split_patternsテーブルにsplit_typeカラムを追加
ALTER TABLE split_patterns ADD COLUMN split_type VARCHAR(10);

-- 10. 既存データは全て'RATIO'（割合指定）として設定
UPDATE split_patterns SET split_type = 'RATIO';

-- 11. split_typeをNOT NULL制約に変更
ALTER TABLE split_patterns ALTER COLUMN split_type SET NOT NULL;

-- 12. split_typeにチェック制約を追加
ALTER TABLE split_patterns 
ADD CONSTRAINT chk_split_patterns_split_type 
    CHECK (split_type IN ('RATIO', 'AMOUNT'));

-- 13. split_patternsテーブルにsplit_amountカラムを追加（金額指定時に使用）
ALTER TABLE split_patterns ADD COLUMN split_amount BIGINT;

-- 14. split_amountのチェック制約を追加
ALTER TABLE split_patterns 
ADD CONSTRAINT chk_split_patterns_split_amount 
    CHECK (
        (split_type = 'RATIO' AND split_amount IS NULL AND split_ratio IS NOT NULL) OR
        (split_type = 'AMOUNT' AND split_amount IS NOT NULL AND split_ratio IS NULL)
    );

-- 15. split_ratioをNULL許容に変更（金額指定の場合はNULL）
ALTER TABLE split_patterns ALTER COLUMN split_ratio DROP NOT NULL;

-- ============================================
-- split_transactionsテーブルの変更
-- ============================================

-- 16. split_transactionsテーブルにtransaction_typeカラムを追加
ALTER TABLE split_transactions ADD COLUMN transaction_type VARCHAR(10);

-- 17. 既存データは全て'INCOME'として設定
UPDATE split_transactions SET transaction_type = 'INCOME';

-- 18. transaction_typeをNOT NULL制約に変更
ALTER TABLE split_transactions ALTER COLUMN transaction_type SET NOT NULL;

-- 19. transaction_typeにチェック制約を追加
ALTER TABLE split_transactions 
ADD CONSTRAINT chk_split_transactions_transaction_type 
    CHECK (transaction_type IN ('INCOME', 'EXPENSE'));

-- 完了メッセージ
DO $$ 
BEGIN
    RAISE NOTICE 'カラムの追加が完了しました';
    RAISE NOTICE '  transaction_type: 収入/支出の判別';
    RAISE NOTICE '  split_type: 割合/金額指定の判別';
    RAISE NOTICE '  split_amount: 金額指定時の金額';
    RAISE NOTICE '';
    RAISE NOTICE '【重要な変更】';
    RAISE NOTICE '  - プライマリキーがid（SERIAL）に変更されました';
    RAISE NOTICE '  - 既存のidカラムはpattern_idにリネームされました';
    RAISE NOTICE '  - 既存データのidは1から順番に自動採番されます';
END $$;

