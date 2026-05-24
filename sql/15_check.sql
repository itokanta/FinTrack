-- 以下を実行してconfdeltypeの値を確認
-- cならばcascadeが効いているが、それ以外なら効いていないので、設定を変更する
SELECT conname, confdeltype
FROM pg_constraint
WHERE conrelid = 'card_payment_discrepancies'::regclass
  AND contype = 'f'
  AND conname LIKE '%bank_transaction%';

-- 設定変更sql
ALTER TABLE card_payment_discrepancies
DROP CONSTRAINT card_payment_discrepancies_bank_transaction_id_fkey;

ALTER TABLE card_payment_discrepancies
ADD CONSTRAINT card_payment_discrepancies_bank_transaction_id_fkey
FOREIGN KEY (bank_transaction_id) REFERENCES bank_transactions(id) ON DELETE CASCADE;


