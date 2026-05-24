-- ============================================
-- カード支払い月管理機能の追加（改訂版）
-- ============================================

-- 【ステップ1】card_transactionsに支払い月カラムを追加
ALTER TABLE card_transactions 
ADD COLUMN payment_month VARCHAR(7); -- YYYY-MM形式

-- 支払い月カラムにインデックスを追加（検索高速化）
CREATE INDEX idx_card_transactions_payment_month ON card_transactions(payment_month);
CREATE INDEX idx_card_transactions_user_payment_month ON card_transactions(user_id, payment_month);

-- 【ステップ2】card_transactionsに引き落とし済みフラグを追加
ALTER TABLE card_transactions 
ADD COLUMN is_paid BOOLEAN; -- NULL許容（デビットカードはNULL）

-- 引き落とし済みフラグにインデックスを追加
CREATE INDEX idx_card_transactions_is_paid ON card_transactions(is_paid);
CREATE INDEX idx_card_transactions_card_type_is_paid ON card_transactions(card_type, is_paid);

-- (ステップ3は削除)

-- 【ステップ4】bank_transactionsにカード引き落としフラグを追加
ALTER TABLE bank_transactions 
ADD COLUMN is_card_payment BOOLEAN DEFAULT FALSE;

-- カード引き落としフラグにインデックスを追加
CREATE INDEX idx_bank_transactions_is_card_payment ON bank_transactions(is_card_payment);

-- 【ステップ5】既存のカード引き落としデータにフラグを設定
UPDATE bank_transactions 
SET is_card_payment = TRUE
WHERE description LIKE '%ラクテンカ－ドサ－ビス%';

-- 【ステップ6】カード差異管理テーブルを作成
CREATE TABLE card_payment_discrepancies (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    payment_month VARCHAR(7) NOT NULL, -- YYYY-MM形式
    bank_transaction_id INTEGER NOT NULL, -- 銀行引き落としのトランザクションID
    expected_amount BIGINT NOT NULL, -- 家計簿上の前月カード利用額
    actual_amount BIGINT NOT NULL, -- 実際の引き落とし額
    difference_amount BIGINT NOT NULL, -- 差異金額（actual - expected）
    is_resolved BOOLEAN DEFAULT FALSE, -- 解決済みフラグ
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (bank_transaction_id) REFERENCES bank_transactions(id) ON DELETE CASCADE
);

-- カード差異管理テーブルのインデックス
CREATE INDEX idx_card_payment_discrepancies_user_id ON card_payment_discrepancies(user_id);
CREATE INDEX idx_card_payment_discrepancies_payment_month ON card_payment_discrepancies(payment_month);
CREATE INDEX idx_card_payment_discrepancies_is_resolved ON card_payment_discrepancies(is_resolved);
CREATE INDEX idx_card_payment_discrepancies_user_month ON card_payment_discrepancies(user_id, payment_month);

-- 完了メッセージ
DO $$ 
BEGIN
    RAISE NOTICE 'カード支払い月管理機能の追加が完了しました';
    RAISE NOTICE '';
    RAISE NOTICE '【追加内容】';
    RAISE NOTICE '1. card_transactions.payment_month カラム追加（支払い月）';
    RAISE NOTICE '2. card_transactions.is_paid カラム追加（引き落とし済みフラグ）';
    RAISE NOTICE '3. bank_transactions.is_card_payment カラム追加（カード引き落としフラグ）';
    RAISE NOTICE '4. card_payment_discrepancies テーブル追加（カード差異管理）';
    RAISE NOTICE '';
    RAISE NOTICE '【既存データの設定】';
    RAISE NOTICE '- クレジット 2025年1月以降: is_paid = FALSE（未払い）';
    RAISE NOTICE '- クレジット 2024年12月以前: is_paid = TRUE（支払い済み）';
    RAISE NOTICE '- デビット: is_paid = NULL（管理対象外）';
END $$;