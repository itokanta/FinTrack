-- ============================================================
-- split_transactions に user_id を追加
-- ============================================================
-- 背景:
--   split_incomes（現 split_transactions）は sql/01 作成時から user_id を持たない。
--   一方、アプリケーション（SplitTransactionRepository）は SELECT で
--   user_id = :userId を前提に書かれており、スキーマとコードが不一致。
--   split_patterns / auto_category_rules 等と同様、ユーザー単位の分離を
--   テーブル自身でも担保する。
--
-- 目的:
--   1. user_id カラムを追加し、既存行を親取引から backfill する
--   2. NOT NULL・外部キー制約を付与する
--   3. Repository の SELECT（user_id + 親 ID）向けの複合インデックスを作成する
--   4. 親 ID 単独の旧インデックス（sql/01 → sql/08 でリネーム、sql/12）を削除する
--
-- 親取引との対応（split_transactions_source_check より、いずれか1つのみ）:
--   bank_transaction_id      → bank_transactions.user_id
--   card_transaction_id      → card_transactions.user_id
--   discrepancy_transaction_id → discrepancy_transactions.user_id
--
-- 前提: sql/12（discrepancy_transaction_id）、sql/16 適用済み
-- 注意:
--   - NOT NULL 適用前に user_id IS NULL の行が 0 件であること
--     （孤児行があると ALTER COLUMN ... SET NOT NULL で失敗する）
--   - 本 SQL 適用後、アプリ側で INSERT に user_id を渡す修正が必要
--     （SplitTransactionRepository.insert）
-- ============================================================


-- ============================================================
-- ステップ 1: user_id カラムを追加（一旦 NULL 許容）
-- ============================================================
ALTER TABLE split_transactions
ADD COLUMN user_id INTEGER;


-- ============================================================
-- ステップ 2: 既存行を親取引から backfill
--   各行は bank / card / discrepancy のいずれか1つの FK のみを持つため、
--   3 本の UPDATE は互いに干渉しない
-- ============================================================

-- 銀行取引に紐づく明細
UPDATE split_transactions AS st
SET user_id = bt.user_id
FROM bank_transactions AS bt
WHERE st.bank_transaction_id = bt.id;

-- カード取引に紐づく明細
UPDATE split_transactions AS st
SET user_id = ct.user_id
FROM card_transactions AS ct
WHERE st.card_transaction_id = ct.id;

-- 差異取引に紐づく明細
UPDATE split_transactions AS st
SET user_id = dt.user_id
FROM discrepancy_transactions AS dt
WHERE st.discrepancy_transaction_id = dt.id;


-- ============================================================
-- ステップ 3: NOT NULL 制約を付与
--   実行前の確認例: SELECT COUNT(*) FROM split_transactions WHERE user_id IS NULL;
-- ============================================================
ALTER TABLE split_transactions
ALTER COLUMN user_id SET NOT NULL;


-- ============================================================
-- ステップ 4: users への外部キー制約
-- ============================================================
ALTER TABLE split_transactions
ADD CONSTRAINT fk_split_transactions_user
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;


-- ============================================================
-- ステップ 5: インデックス作成
--   Repository は常に user_id と親 ID を併用して検索する
-- ============================================================
CREATE INDEX idx_split_transactions_user_id
    ON split_transactions(user_id);
CREATE INDEX idx_split_transactions_user_bank
    ON split_transactions(user_id, bank_transaction_id);
CREATE INDEX idx_split_transactions_user_card
    ON split_transactions(user_id, card_transaction_id);
CREATE INDEX idx_split_transactions_user_discrepancy
    ON split_transactions(user_id, discrepancy_transaction_id);


-- ============================================================
-- ステップ 6: 冗長な旧インデックスを削除
--   親 ID 単独のインデックスはステップ 5 の複合インデックスで代替
--   削除対象:
--     idx_split_transactions_bank_transaction        (旧 idx_split_incomes_bank_transaction)
--     idx_split_transactions_card_transaction        (旧 idx_split_incomes_card_transaction)
--     idx_split_transactions_discrepancy_transaction (sql/12)
-- ============================================================
DROP INDEX IF EXISTS idx_split_transactions_bank_transaction;
DROP INDEX IF EXISTS idx_split_transactions_card_transaction;
DROP INDEX IF EXISTS idx_split_transactions_discrepancy_transaction;
