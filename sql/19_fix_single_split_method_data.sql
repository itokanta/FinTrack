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
