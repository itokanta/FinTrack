-- 取引区分（銀行/カード）とカード種別（デビット/クレジット）を追加

-- bank_transactions テーブルに transaction_source カラムを追加
-- BANK: 銀行取引
ALTER TABLE bank_transactions ADD COLUMN transaction_source VARCHAR(10);

-- 既存データは全て BANK として設定
UPDATE bank_transactions SET transaction_source = 'BANK';

-- NOT NULL 制約を追加
ALTER TABLE bank_transactions ALTER COLUMN transaction_source SET NOT NULL;

-- インデックスを追加
CREATE INDEX idx_bank_transactions_source ON bank_transactions (transaction_source);


-- card_transactions テーブルに transaction_source と card_type カラムを追加
-- transaction_source: CARD（カード取引）
-- card_type: DEBIT（デビットカード）, CREDIT（クレジットカード）
ALTER TABLE card_transactions ADD COLUMN transaction_source VARCHAR(10);
ALTER TABLE card_transactions ADD COLUMN card_type VARCHAR(10);

-- 既存データは全て CARD, CREDIT として設定（楽天カードはクレジットカード）
UPDATE card_transactions SET transaction_source = 'CARD', card_type = 'CREDIT';

-- NOT NULL 制約を追加
ALTER TABLE card_transactions ALTER COLUMN transaction_source SET NOT NULL;
ALTER TABLE card_transactions ALTER COLUMN card_type SET NOT NULL;

-- インデックスを追加
CREATE INDEX idx_card_transactions_source ON card_transactions (transaction_source);
CREATE INDEX idx_card_transactions_card_type ON card_transactions (card_type);

