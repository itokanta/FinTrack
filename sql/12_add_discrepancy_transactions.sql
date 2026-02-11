-- ============================================
-- カード差異取引テーブルの追加
-- ============================================
-- 概要:
--   カード引き落とし金額と家計簿上のカード利用額に差異があった場合、
--   その差異額を取引データとして記録するテーブルを追加します。
-- 
-- 機能:
--   1. 差異額を取引データとして登録
--   2. card_payment_discrepanciesテーブルと紐づけ
--   3. 資産カテゴリ・支出カテゴリ・分割パターンに対応
-- ============================================

-- 【ステップ1】差異取引テーブルの作成
CREATE TABLE discrepancy_transactions (
    id SERIAL PRIMARY KEY,
    description VARCHAR(500) NOT NULL,
    amount BIGINT NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    transaction_date DATE NOT NULL,
    user_id INTEGER NOT NULL,
    asset_category_id INTEGER,
    expense_category_id INTEGER,
    split_pattern_id INTEGER,
    card_payment_discrepancy_id INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- 外部キー制約
    CONSTRAINT fk_discrepancy_transaction_user 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_discrepancy_transaction_asset_category 
        FOREIGN KEY (asset_category_id) REFERENCES asset_categories(id) ON DELETE SET NULL,
    CONSTRAINT fk_discrepancy_transaction_expense_category 
        FOREIGN KEY (expense_category_id) REFERENCES expense_categories(id) ON DELETE SET NULL,
    CONSTRAINT fk_discrepancy_transaction_discrepancy 
        FOREIGN KEY (card_payment_discrepancy_id) REFERENCES card_payment_discrepancies(id) ON DELETE CASCADE,
    
    -- CHECK制約
    CONSTRAINT chk_discrepancy_transaction_type 
        CHECK (transaction_type IN ('INCOME', 'EXPENSE'))
);

-- 【ステップ2】インデックスの作成
CREATE INDEX idx_discrepancy_transactions_user 
    ON discrepancy_transactions(user_id);

CREATE INDEX idx_discrepancy_transactions_discrepancy 
    ON discrepancy_transactions(card_payment_discrepancy_id);

CREATE INDEX idx_discrepancy_transactions_date 
    ON discrepancy_transactions(transaction_date);

CREATE INDEX idx_discrepancy_transactions_asset_category 
    ON discrepancy_transactions(asset_category_id);

CREATE INDEX idx_discrepancy_transactions_expense_category 
    ON discrepancy_transactions(expense_category_id);

-- 【ステップ3】split_transactionsテーブルにdiscrepancy_transaction_idカラムを追加
ALTER TABLE split_transactions 
ADD COLUMN discrepancy_transaction_id INTEGER;

-- 外部キー制約を追加
ALTER TABLE split_transactions
ADD CONSTRAINT fk_split_transactions_discrepancy_transaction
    FOREIGN KEY (discrepancy_transaction_id) REFERENCES discrepancy_transactions(id) ON DELETE CASCADE;

-- インデックスを作成
CREATE INDEX idx_split_transactions_discrepancy_transaction 
    ON split_transactions(discrepancy_transaction_id);

-- CHECK制約を修正（bank, card, discrepancyのいずれか1つのみ）
ALTER TABLE split_transactions DROP CONSTRAINT IF EXISTS split_incomes_check;
ALTER TABLE split_transactions DROP CONSTRAINT IF EXISTS split_transactions_check;
ALTER TABLE split_transactions 
ADD CONSTRAINT split_transactions_source_check 
    CHECK (
        (bank_transaction_id IS NOT NULL AND card_transaction_id IS NULL AND discrepancy_transaction_id IS NULL) OR
        (bank_transaction_id IS NULL AND card_transaction_id IS NOT NULL AND discrepancy_transaction_id IS NULL) OR
        (bank_transaction_id IS NULL AND card_transaction_id IS NULL AND discrepancy_transaction_id IS NOT NULL)
    );

-- 【ステップ4】テーブルコメントの追加
COMMENT ON TABLE discrepancy_transactions IS 'カード引き落とし差異を取引データとして記録するテーブル';
COMMENT ON COLUMN discrepancy_transactions.id IS 'プライマリキー（自動採番）';
COMMENT ON COLUMN discrepancy_transactions.description IS '差異の概要・説明（ユーザーが入力）';
COMMENT ON COLUMN discrepancy_transactions.amount IS '差異金額（絶対値）';
COMMENT ON COLUMN discrepancy_transactions.transaction_type IS '収支区分（INCOME/EXPENSE）';
COMMENT ON COLUMN discrepancy_transactions.transaction_date IS '差異登録日（通常は解決時の日付）';
COMMENT ON COLUMN discrepancy_transactions.user_id IS 'ユーザーID';
COMMENT ON COLUMN discrepancy_transactions.asset_category_id IS '資産カテゴリID（単一カテゴリの場合のみ使用、手動分割の場合はNULL）';
COMMENT ON COLUMN discrepancy_transactions.expense_category_id IS '支出カテゴリID（支出の場合のみ使用）';
COMMENT ON COLUMN discrepancy_transactions.split_pattern_id IS '分割パターンID（NULL=未分配, -1=手動分割, 0=単一カテゴリ, >=1=パターン分割）';
COMMENT ON COLUMN discrepancy_transactions.card_payment_discrepancy_id IS '元となった差異レコードのID（card_payment_discrepancies）';
COMMENT ON COLUMN discrepancy_transactions.created_at IS 'レコード作成日時';
COMMENT ON COLUMN split_transactions.discrepancy_transaction_id IS '差異取引ID（差異解決時に使用）';

-- 完了メッセージ
DO $$ 
BEGIN
    RAISE NOTICE '============================================';
    RAISE NOTICE 'カード差異取引テーブルの追加が完了しました';
    RAISE NOTICE '============================================';
    RAISE NOTICE '';
    RAISE NOTICE '【追加内容】';
    RAISE NOTICE '1. discrepancy_transactions テーブル作成';
    RAISE NOTICE '   - カード差異を取引データとして記録';
    RAISE NOTICE '   - card_payment_discrepancies と紐づけ';
    RAISE NOTICE '   - 資産/支出カテゴリ、分割パターンに対応';
    RAISE NOTICE '2. split_transactions に discrepancy_transaction_id カラム追加';
    RAISE NOTICE '   - 手動分割時の詳細を split_transactions に記録';
    RAISE NOTICE '';
    RAISE NOTICE '【split_pattern_idの値の意味】';
    RAISE NOTICE '  NULL: 未分配';
    RAISE NOTICE '  -1: 手動分割（split_transactionsに詳細あり）';
    RAISE NOTICE '  0: 単一カテゴリ分割';
    RAISE NOTICE '  >= 1: パターン分割（split_transactionsに詳細あり）';
    RAISE NOTICE '';
    RAISE NOTICE '============================================';
END $$;
