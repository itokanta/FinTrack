-- ============================================
-- 分割済み支出の残高を修正
-- 分割した支出の残高が未分類に残っているため、
-- 正しいカテゴリに残高を移動する
-- ============================================

-- 【概要】
-- 支出を分割した場合、本来は以下の処理が必要:
-- 1. 未分類から支出金額を戻す（プラス）
-- 2. 各分割先カテゴリから金額を引く（マイナス）
--
-- しかし、既存データではこの処理が行われていないため、
-- split_transactionsを元に残高を修正する

-- 一時テーブルを作成して、修正が必要な分割取引を特定
CREATE TEMP TABLE expense_splits_to_fix AS
SELECT 
    st.asset_category_id,
    st.amount,
    COALESCE(bt.user_id, ct.user_id) as user_id
FROM split_transactions st
LEFT JOIN bank_transactions bt ON st.bank_transaction_id = bt.id
LEFT JOIN card_transactions ct ON st.card_transaction_id = ct.id
WHERE st.transaction_type = 'EXPENSE';

-- デバッグ: 修正対象を表示
DO $$ 
DECLARE
    total_records INTEGER;
BEGIN
    SELECT COUNT(*) INTO total_records FROM expense_splits_to_fix;
    RAISE NOTICE '修正対象の分割支出レコード数: %', total_records;
END $$;

-- 各カテゴリの残高から分割金額を引く
-- 一時テーブルをグループ化して、カテゴリごとの合計を計算
WITH category_adjustments AS (
    SELECT 
        user_id,
        asset_category_id,
        SUM(amount) as total_amount
    FROM expense_splits_to_fix
    GROUP BY user_id, asset_category_id
)
UPDATE assets a
SET amount = a.amount - ca.total_amount
FROM category_adjustments ca
WHERE a.user_id = ca.user_id
  AND a.asset_category_id = ca.asset_category_id;

-- 未分類（asset_category_id = NULL）の残高を戻す
-- 各ユーザーごとに、分割支出の合計金額を未分類に加算
WITH user_total_adjustments AS (
    SELECT 
        user_id,
        SUM(amount) as total_amount
    FROM expense_splits_to_fix
    GROUP BY user_id
)
UPDATE assets a
SET amount = a.amount + uta.total_amount
FROM user_total_adjustments uta
WHERE a.user_id = uta.user_id
  AND a.asset_category_id IS NULL;

-- 一時テーブルを削除
DROP TABLE expense_splits_to_fix;

-- 完了メッセージ
DO $$ 
BEGIN
    RAISE NOTICE '分割済み支出の残高修正が完了しました';
    RAISE NOTICE '';
    RAISE NOTICE '【修正内容】';
    RAISE NOTICE '1. 分割先カテゴリから金額を減算';
    RAISE NOTICE '2. 未分類カテゴリに金額を戻す（加算）';
END $$;

