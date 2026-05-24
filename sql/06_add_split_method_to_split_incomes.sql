-- split_incomes テーブルに分割方法を示すカラムを追加
-- split_method: 'PATTERN' = パターン使用, 'MANUAL' = 手動分割, 'SINGLE' = 単一カテゴリ
--
-- 【重要】transactionsテーブルのincome_split_pattern_idの値の意味:
--   NULL: 未分配
--   >= 1: パターン分割 (income_split_patternsテーブルのid)
--   0: 単一カテゴリ分割
--   -1: 手動分割

-- 1. split_methodカラムを追加（一時的にNULL許容）
ALTER TABLE split_incomes ADD COLUMN split_method VARCHAR(20);

-- 2. 既存データのsplit_methodを設定
-- split_pattern_id が 0 の場合は SINGLE（単一カテゴリ）
-- それ以外は PATTERN（パターン使用）
UPDATE split_incomes 
SET split_method = CASE 
    WHEN split_pattern_id = 0 THEN 'SINGLE'
    ELSE 'PATTERN'
END;

-- 3. split_methodをNOT NULL制約に変更
ALTER TABLE split_incomes ALTER COLUMN split_method SET NOT NULL;

-- 4. split_pattern_idをNULL許容に変更
ALTER TABLE split_incomes ALTER COLUMN split_pattern_id DROP NOT NULL;

-- 5. split_methodにチェック制約を追加
ALTER TABLE split_incomes ADD CONSTRAINT chk_split_method 
    CHECK (split_method IN ('PATTERN', 'MANUAL', 'SINGLE'));

-- 6. 既存のsplit_pattern_idが0のレコードをNULLに変更
UPDATE split_incomes SET split_pattern_id = NULL WHERE split_pattern_id = 0;

