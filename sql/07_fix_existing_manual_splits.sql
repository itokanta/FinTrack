-- 既存の手動分割データのincome_split_pattern_idを修正
-- split_incomesテーブルにsplit_method='MANUAL'のレコードがある取引の
-- income_split_pattern_idを-1に設定

-- 銀行取引の修正
UPDATE bank_transactions bt
SET income_split_pattern_id = -1
WHERE EXISTS (
    SELECT 1 
    FROM split_incomes si 
    WHERE si.bank_transaction_id = bt.id 
    AND si.split_method = 'MANUAL'
)
AND (bt.income_split_pattern_id IS NULL OR bt.income_split_pattern_id = 0);

-- カード取引の修正
UPDATE card_transactions ct
SET income_split_pattern_id = -1
WHERE EXISTS (
    SELECT 1 
    FROM split_incomes si 
    WHERE si.card_transaction_id = ct.id 
    AND si.split_method = 'MANUAL'
)
AND (ct.income_split_pattern_id IS NULL OR ct.income_split_pattern_id = 0);

-- 確認用クエリ（実行後に確認してください）
-- SELECT 
--     bt.id,
--     bt.description,
--     bt.amount,
--     bt.income_split_pattern_id,
--     si.split_method
-- FROM bank_transactions bt
-- LEFT JOIN split_incomes si ON si.bank_transaction_id = bt.id
-- WHERE bt.transaction_type = 'INCOME'
-- ORDER BY bt.transaction_date DESC
-- LIMIT 20;

