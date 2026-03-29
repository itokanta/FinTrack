-- 登録履歴テーブル（1 CSV = 1 レコード）
CREATE TABLE csv_import_histories (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    import_type VARCHAR(10) NOT NULL,  -- 'BANK' or 'CARD'
    csv_name VARCHAR(255) NOT NULL,
    import_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    record_count INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_csv_import_histories_user_id ON csv_import_histories(user_id);
CREATE INDEX idx_csv_import_histories_import_date ON csv_import_histories(import_date);

ALTER TABLE bank_transactions
ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN csv_import_history_id INTEGER REFERENCES csv_import_histories(id) ON DELETE SET NULL,
ADD COLUMN update_count INTEGER NOT NULL DEFAULT 0;

CREATE INDEX idx_bank_transactions_csv_import_history ON bank_transactions(csv_import_history_id);

-- card_transactions に登録履歴FKを追加
ALTER TABLE card_transactions
ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN csv_import_history_id INTEGER REFERENCES csv_import_histories(id) ON DELETE SET NULL,
ADD COLUMN update_count INTEGER NOT NULL DEFAULT 0;


CREATE INDEX idx_card_transactions_csv_import_history ON card_transactions(csv_import_history_id);

-- アップデート項目の内容を管理するテーブル（1更新 = 1レコード）
CREATE TABLE csv_import_update_histories (
    id SERIAL PRIMARY KEY,
    csv_import_history_id INTEGER NOT NULL,
    bank_transaction_id INTEGER,
    card_transaction_id INTEGER,
    target_field VARCHAR(50) NOT NULL,  -- 'payment_month' or 'is_paid'
    value_before TEXT,
    value_after TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id INTEGER NOT NULL,
    update_count INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (csv_import_history_id) REFERENCES csv_import_histories(id) ON DELETE CASCADE,
    FOREIGN KEY (bank_transaction_id) REFERENCES bank_transactions(id) ON DELETE SET NULL,
    FOREIGN KEY (card_transaction_id) REFERENCES card_transactions(id) ON DELETE SET NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CHECK (
        (bank_transaction_id IS NOT NULL AND card_transaction_id IS NULL) OR
        (bank_transaction_id IS NULL AND card_transaction_id IS NOT NULL)
    )
);

CREATE INDEX idx_csv_import_update_histories_import ON csv_import_update_histories(csv_import_history_id);
CREATE INDEX idx_csv_import_update_histories_card ON csv_import_update_histories(card_transaction_id);