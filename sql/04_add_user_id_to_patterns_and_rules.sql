-- ============================================
-- income_split_patternsとauto_category_rulesにuser_idを追加
-- ユーザー固有のパターンとルールを管理
-- ============================================

-- 1. income_split_patternsテーブルにuser_idカラムを追加
ALTER TABLE income_split_patterns 
ADD COLUMN user_id INTEGER;

-- 2. 既存データのuser_idを設定（asset_categoriesから取得）
UPDATE income_split_patterns 
SET user_id = (
    SELECT user_id 
    FROM asset_categories 
    WHERE asset_categories.id = income_split_patterns.asset_category_id
);

-- 3. user_idをNOT NULL制約に変更
ALTER TABLE income_split_patterns 
ALTER COLUMN user_id SET NOT NULL;

-- 4. 外部キー制約を追加
ALTER TABLE income_split_patterns 
ADD CONSTRAINT income_split_patterns_user_id_fkey 
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- 5. インデックスを追加
CREATE INDEX idx_income_split_patterns_user_id ON income_split_patterns(user_id);

-- 6. 複合主キーを再定義（user_id, id, asset_category_id）
ALTER TABLE income_split_patterns DROP CONSTRAINT income_split_patterns_pkey;
ALTER TABLE income_split_patterns 
ADD CONSTRAINT income_split_patterns_pkey 
PRIMARY KEY (user_id, id, asset_category_id);

-- ============================================
-- auto_category_rulesテーブルの修正
-- ============================================

-- 7. auto_category_rulesテーブルにuser_idカラムを追加（既にある場合はスキップ）
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'auto_category_rules' AND column_name = 'user_id'
    ) THEN
        ALTER TABLE auto_category_rules ADD COLUMN user_id INTEGER;
    END IF;
END $$;

-- 8. 既存データのuser_idを設定（asset_categoriesから取得）
UPDATE auto_category_rules 
SET user_id = (
    SELECT user_id 
    FROM asset_categories 
    WHERE asset_categories.id = auto_category_rules.asset_category_id
)
WHERE user_id IS NULL;

-- 9. user_idをNOT NULL制約に変更
ALTER TABLE auto_category_rules 
ALTER COLUMN user_id SET NOT NULL;

-- 10. 外部キー制約を追加（存在しない場合）
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'auto_category_rules_user_id_fkey'
    ) THEN
        ALTER TABLE auto_category_rules 
        ADD CONSTRAINT auto_category_rules_user_id_fkey 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
    END IF;
END $$;

-- 11. インデックスを追加
CREATE INDEX IF NOT EXISTS idx_auto_category_rules_user_id ON auto_category_rules(user_id);

-- 12. transaction_typeカラムを追加（収支区分で判別するため）
ALTER TABLE auto_category_rules 
ADD COLUMN IF NOT EXISTS transaction_type VARCHAR(20);

-- 13. 既存データのtransaction_typeをデフォルトでEXPENSEに設定
UPDATE auto_category_rules 
SET transaction_type = 'EXPENSE'
WHERE transaction_type IS NULL;

-- 14. transaction_typeをNOT NULL制約に変更
ALTER TABLE auto_category_rules 
ALTER COLUMN transaction_type SET NOT NULL;

-- 注意事項:
-- - 既存データがある場合、user_idが正しく設定されているか確認してください
-- - income_split_patternsの主キー変更により、既存の外部キー参照がある場合は調整が必要です
