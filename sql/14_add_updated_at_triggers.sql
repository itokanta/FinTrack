-- BEFORE UPDATE トリガー関数（全テーブル共通）
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- users
CREATE OR REPLACE TRIGGER trg_users_updated_at
BEFORE UPDATE ON users
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- assets
CREATE OR REPLACE TRIGGER trg_assets_updated_at
BEFORE UPDATE ON assets
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- split_transactions
CREATE OR REPLACE TRIGGER trg_split_transactions_updated_at
BEFORE UPDATE ON split_transactions
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- card_transactions
CREATE OR REPLACE TRIGGER trg_card_transactions_updated_at
BEFORE UPDATE ON card_transactions
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- bank_transactions
CREATE OR REPLACE TRIGGER trg_bank_transactions_updated_at
BEFORE UPDATE ON bank_transactions
FOR EACH ROW EXECUTE FUNCTION set_updated_at();
