-- ============================================
-- asset_category_id管理の統一
-- split_pattern_idの値によって管理方法を統一
-- - split_pattern_id = 0: 単一カテゴリ → asset_category_idに設定
-- - split_pattern_id >= 1 or < 0: 分割 → asset_category_idはNULL
-- ============================================

-- 【ステップ1】card_transactionsのsplit_pattern_idをNULLから0に設定
-- 現状のcard_transactionsは全て分割していないデータ
UPDATE card_transactions 
SET split_pattern_id = 0
WHERE split_pattern_id IS NULL;

-- 【ステップ2】split_transactionsのSINGLEデータをbank_transactions/card_transactionsに移行
-- bank_transactionsの場合
UPDATE bank_transactions bt
SET asset_category_id = st.asset_category_id
FROM split_transactions st
WHERE st.bank_transaction_id = bt.id
  AND st.split_method = 'SINGLE'
  AND bt.split_pattern_id = 0;

-- card_transactionsの場合
UPDATE card_transactions ct
SET asset_category_id = st.asset_category_id
FROM split_transactions st
WHERE st.card_transaction_id = ct.id
  AND st.split_method = 'SINGLE'
  AND ct.split_pattern_id = 0;

-- 【ステップ3】SINGLEのsplit_transactionsデータを削除
DELETE FROM split_transactions
WHERE split_method = 'SINGLE';

-- 【ステップ4】分割されている取引(split_pattern_id >= 1 or < 0)のasset_category_idをNULLに設定
-- bank_transactionsの場合
UPDATE bank_transactions
SET asset_category_id = NULL
WHERE split_pattern_id IS NOT NULL 
  AND split_pattern_id != 0;

-- card_transactionsの場合
UPDATE card_transactions
SET asset_category_id = NULL
WHERE split_pattern_id IS NOT NULL 
  AND split_pattern_id != 0;

-- 完了メッセージ
DO $$ 
BEGIN
    RAISE NOTICE 'asset_category_id管理の統一が完了しました';
    RAISE NOTICE '';
    RAISE NOTICE '【変更内容】';
    RAISE NOTICE '1. card_transactionsのsplit_pattern_idをNULLから0に設定';
    RAISE NOTICE '2. SINGLEのsplit_transactionsをbank/card_transactionsのasset_category_idに移行';
    RAISE NOTICE '3. SINGLEのsplit_transactionsを削除';
    RAISE NOTICE '4. 分割取引のasset_category_idをNULLに設定';
    RAISE NOTICE '';
    RAISE NOTICE '【管理ルール】';
    RAISE NOTICE '  split_pattern_id = 0: 単一カテゴリ → asset_category_idで管理';
    RAISE NOTICE '  split_pattern_id >= 1 or < 0: 分割 → split_transactionsで管理';
END $$;

