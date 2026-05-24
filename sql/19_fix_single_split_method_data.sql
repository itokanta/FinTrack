-- ============================================================
-- 単一カテゴリ（SINGLE）の旧データ移行と split_transactions 整理
-- ============================================================
-- 背景:
--   sql/16 以前は単一カテゴリでも split_transactions に明細が残ることがあった。
--   sql/16 以降の現行仕様では SINGLE は親取引の asset_category_id に直接保持し、
--   split_transactions には行を作らない（SplitService.splitSingle 参照）。
--
-- 目的:
--   1. 親取引（bank / card）の asset_category_id が NULL のまま SINGLE になっている行に、
--      対応する split_transactions の asset_category_id をコピーして埋める。
--   2. 移行済みの旧 SINGLE 明細を split_transactions から削除する。
--
-- 前提: sql/16_add_split_type_and_soft_delete_pattern.sql 適用済み
-- 推奨: 実行前に pg_dump でバックアップ、BEGIN 内で実行し結果確認後 COMMIT
-- ============================================================

-- ステップ 1: 銀行取引へカテゴリ ID をコピー
UPDATE bank_transactions AS bt
SET asset_category_id = st.asset_category_id
FROM split_transactions AS st
WHERE bt.id = st.bank_transaction_id
  AND bt.asset_category_id IS NULL
  AND st.split_method = 'SINGLE'
  AND bt.split_method = 'SINGLE';

-- ステップ 2: 旧 SINGLE 明細を削除（現行アプリでは SINGLE は split_transactions に保持しない）
DELETE FROM split_transactions
WHERE split_method = 'SINGLE';

-- ============================================================
-- split_pattern_id の FK 参照先を split_pattern_groups(id) に修正
-- ============================================================
-- 背景:
--   sql/16 で bank/card/split/discrepancy 各テーブルの split_pattern_id に
--   split_patterns(id) への FK が追加された。
--   sql/18 で split_pattern_groups が導入され、アプリは split_pattern_id に
--   グループ ID（split_pattern_groups.id）を保存するようになったが、
--   FK の更新が行われていなかった。
--
-- 目的:
--   split_pattern_id の FK 参照先を split_pattern_groups(id) に統一する。
--
-- 対象テーブル:
--   - bank_transactions      (fk_bt_split_pattern_id)
--   - card_transactions      (fk_ct_split_pattern_id)
--   - split_transactions     (fk_st_split_pattern_id)
--   - discrepancy_transactions (fk_dt_split_pattern_id)
--
-- 前提: sql/18_add_split_pattern_groups.sql 適用済み
-- 推奨: 実行前に pg_dump でバックアップ、BEGIN 内で実行し結果確認後 COMMIT
-- ============================================================


-- ============================================================
-- ステップ 1: 旧 FK 定義（split_patterns.id）で保存されていた値をグループ ID に変換
--   split_pattern_id = split_patterns.id かつ id <> pattern_id の行のみ対象。
--   すでに split_pattern_groups.id が入っている行は変更しない。
--   実行時に各テーブルの更新件数を NOTICE で表示する。
-- ============================================================

DO $$
DECLARE
    updated_count INTEGER;
BEGIN
    UPDATE bank_transactions AS bt
    SET split_pattern_id = sp.pattern_id
    FROM split_patterns AS sp
    WHERE bt.split_pattern_id = sp.id
      AND bt.split_pattern_id <> sp.pattern_id
      AND bt.split_method = 'PATTERN';
    GET DIAGNOSTICS updated_count = ROW_COUNT;
    RAISE NOTICE 'bank_transactions: % 件を更新', updated_count;

    UPDATE card_transactions AS ct
    SET split_pattern_id = sp.pattern_id
    FROM split_patterns AS sp
    WHERE ct.split_pattern_id = sp.id
      AND ct.split_pattern_id <> sp.pattern_id
      AND ct.split_method = 'PATTERN';
    GET DIAGNOSTICS updated_count = ROW_COUNT;
    RAISE NOTICE 'card_transactions: % 件を更新', updated_count;

    UPDATE split_transactions AS st
    SET split_pattern_id = sp.pattern_id
    FROM split_patterns AS sp
    WHERE st.split_pattern_id = sp.id
      AND st.split_pattern_id <> sp.pattern_id
      AND st.split_method = 'PATTERN';
    GET DIAGNOSTICS updated_count = ROW_COUNT;
    RAISE NOTICE 'split_transactions: % 件を更新', updated_count;

    UPDATE discrepancy_transactions AS dt
    SET split_pattern_id = sp.pattern_id
    FROM split_patterns AS sp
    WHERE dt.split_pattern_id = sp.id
      AND dt.split_pattern_id <> sp.pattern_id
      AND dt.split_method = 'PATTERN';
    GET DIAGNOSTICS updated_count = ROW_COUNT;
    RAISE NOTICE 'discrepancy_transactions: % 件を更新', updated_count;
END $$;


-- ============================================================
-- ステップ 2: 旧 FK 制約を削除
-- ============================================================

ALTER TABLE bank_transactions
    DROP CONSTRAINT IF EXISTS fk_bt_split_pattern_id;

ALTER TABLE card_transactions
    DROP CONSTRAINT IF EXISTS fk_ct_split_pattern_id;

ALTER TABLE split_transactions
    DROP CONSTRAINT IF EXISTS fk_st_split_pattern_id;

ALTER TABLE discrepancy_transactions
    DROP CONSTRAINT IF EXISTS fk_dt_split_pattern_id;


-- ============================================================
-- ステップ 3: 新 FK 制約を追加（split_pattern_groups.id を参照）
--   NULL は未分配 / 手動 / 単一カテゴリのため許容。
--   グループは論理削除（deleted_at）のため ON DELETE はデフォルト（NO ACTION）。
-- ============================================================

ALTER TABLE bank_transactions
    ADD CONSTRAINT fk_bt_split_pattern_id
        FOREIGN KEY (split_pattern_id) REFERENCES split_pattern_groups(id);

ALTER TABLE card_transactions
    ADD CONSTRAINT fk_ct_split_pattern_id
        FOREIGN KEY (split_pattern_id) REFERENCES split_pattern_groups(id);

ALTER TABLE split_transactions
    ADD CONSTRAINT fk_st_split_pattern_id
        FOREIGN KEY (split_pattern_id) REFERENCES split_pattern_groups(id);

ALTER TABLE discrepancy_transactions
    ADD CONSTRAINT fk_dt_split_pattern_id
        FOREIGN KEY (split_pattern_id) REFERENCES split_pattern_groups(id);
