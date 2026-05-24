-- ============================================================
-- split_patterns の論理削除対応 と split_method カラム追加
-- ============================================================
-- 目的:
--   1. split_patterns を物理削除から論理削除に変更する。
--      削除済みパターンで分類された取引の履歴表示を維持するため。
--   2. bank_transactions / card_transactions / discrepancy_transactions に
--      split_method カラムを追加し、分類状態（SINGLE / PATTERN / MANUAL）を
--      split_pattern_id の番兵値（0 / 正の値 / -1）に依存せず管理できるようにする。
--   3. 既存データの split_pattern_id 番兵値を split_method に移行し、
--      番兵値を使わない正規の状態に変換する。
--   4. split_pattern_id に FK 制約を追加し、参照整合性を担保する。
-- ============================================================


-- ============================================================
-- ステップ 1: split_patterns に論理削除カラムを追加
--   NULL = 有効、値あり = 削除日時
-- ============================================================
ALTER TABLE split_patterns
ADD COLUMN deleted_at TIMESTAMP DEFAULT NULL;


-- ============================================================
-- ステップ 2: 各取引テーブルに split_method カラムを追加
--   NULL = 未分類、SINGLE = 単一カテゴリ、PATTERN = パターン分割、MANUAL = 手動分割
-- ============================================================
ALTER TABLE bank_transactions
ADD COLUMN split_method TEXT DEFAULT NULL;

ALTER TABLE card_transactions
ADD COLUMN split_method TEXT DEFAULT NULL;

ALTER TABLE discrepancy_transactions
ADD COLUMN split_method TEXT DEFAULT NULL;


-- ============================================================
-- ステップ 3: 既存データの番兵値を split_method に変換
--
--   旧: split_pattern_id = -1（手動分割）→ split_method = 'MANUAL'、split_pattern_id を NULL に
--   旧: split_pattern_id =  0（単一カテゴリ）→ split_method = 'SINGLE'、split_pattern_id を NULL に
--   旧: split_pattern_id = 正の値（パターン）→ split_method = 'PATTERN'、split_pattern_id は保持
--
--   実行順序は重要: -1 と 0 を先に NULL にしてから IS NOT NULL で残りを PATTERN にする
-- ============================================================

-- bank_transactions
UPDATE bank_transactions SET split_pattern_id = NULL, split_method = 'MANUAL' WHERE split_pattern_id = -1;
UPDATE bank_transactions SET split_pattern_id = NULL, split_method = 'SINGLE' WHERE split_pattern_id = 0;
UPDATE bank_transactions SET split_method = 'PATTERN' WHERE split_pattern_id IS NOT NULL;

-- card_transactions
UPDATE card_transactions SET split_pattern_id = NULL, split_method = 'MANUAL' WHERE split_pattern_id = -1;
UPDATE card_transactions SET split_pattern_id = NULL, split_method = 'SINGLE' WHERE split_pattern_id = 0;
UPDATE card_transactions SET split_method = 'PATTERN' WHERE split_pattern_id IS NOT NULL;

-- discrepancy_transactions
UPDATE discrepancy_transactions SET split_pattern_id = NULL, split_method = 'MANUAL' WHERE split_pattern_id = -1;
UPDATE discrepancy_transactions SET split_pattern_id = NULL, split_method = 'SINGLE' WHERE split_pattern_id = 0;
UPDATE discrepancy_transactions SET split_method = 'PATTERN' WHERE split_pattern_id IS NOT NULL;


-- ============================================================
-- ステップ 4: split_pattern_id に FK 制約を追加
--   ステップ 3 で番兵値（-1 / 0）を NULL に変換済みのため、
--   以降は正の値（実パターン ID）か NULL のみが格納される
-- ============================================================
ALTER TABLE bank_transactions
ADD CONSTRAINT fk_bt_split_pattern_id
FOREIGN KEY (split_pattern_id) REFERENCES split_patterns(id);

ALTER TABLE card_transactions
ADD CONSTRAINT fk_ct_split_pattern_id
FOREIGN KEY (split_pattern_id) REFERENCES split_patterns(id);

ALTER TABLE split_transactions
ADD CONSTRAINT fk_st_split_pattern_id
FOREIGN KEY (split_pattern_id) REFERENCES split_patterns(id);

ALTER TABLE discrepancy_transactions
ADD CONSTRAINT fk_dt_split_pattern_id
FOREIGN KEY (split_pattern_id) REFERENCES split_patterns(id);
