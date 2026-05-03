-- ============================================
-- discrepancy_transactions に bank_transaction_id を追加
-- ============================================
-- 概要:
--   カード引き落としの銀行側取引への直接参照を持たせる。
--   split_transactions は差異額を分割した場合のみ行ができるため、
--   銀行取引との対応は本カラムで表す。
--
-- 既存行:
--   bank_transaction_id は NULL のまま（バックフィルしない）
--
-- 新規行:
--   アプリケーション側で必ずセットする想定（DB は NULL 許容）
-- ============================================

ALTER TABLE discrepancy_transactions
ADD COLUMN bank_transaction_id INTEGER;

ALTER TABLE discrepancy_transactions
ADD CONSTRAINT fk_discrepancy_transaction_bank_transaction
    FOREIGN KEY (bank_transaction_id) REFERENCES bank_transactions(id) ON DELETE SET NULL;

CREATE INDEX idx_discrepancy_transactions_bank_transaction
    ON discrepancy_transactions(bank_transaction_id);
