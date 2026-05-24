-- ユーザー管理テーブル
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- カード情報管理テーブル
CREATE TABLE card_transactions (
    id SERIAL PRIMARY KEY,
    description VARCHAR(500) NOT NULL,
    amount BIGINT NOT NULL,
    transaction_type VARCHAR(20) NOT NULL, -- 'INCOME' or 'EXPENSE'
    transaction_date DATE NOT NULL,
    fingerprint VARCHAR(64) NOT NULL,
    user_id INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 重複チェック用のインデックス
CREATE INDEX idx_card_transactions_fingerprint ON card_transactions(fingerprint);
CREATE INDEX idx_card_transactions_user_date ON card_transactions(user_id, transaction_date);

-- 銀行情報管理テーブル
CREATE TABLE bank_transactions (
    id SERIAL PRIMARY KEY,
    description VARCHAR(500) NOT NULL,
    amount BIGINT NOT NULL,
    transaction_type VARCHAR(20) NOT NULL, -- 'INCOME' or 'EXPENSE'
    transaction_date DATE NOT NULL,
    fingerprint VARCHAR(64) NOT NULL,
    user_id INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 重複チェック用のインデックス
CREATE INDEX idx_bank_transactions_fingerprint ON bank_transactions(fingerprint);
CREATE INDEX idx_bank_transactions_user_date ON bank_transactions(user_id, transaction_date);

-- 資産カテゴリテーブル
CREATE TABLE asset_categories (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    category_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE(user_id, category_name)
);

-- 支出カテゴリテーブル
CREATE TABLE expense_categories (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    category_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE(user_id, category_name)
);

-- 資産管理テーブル
CREATE TABLE assets (
    id SERIAL PRIMARY KEY,
    bank_transaction_id INTEGER,
    card_transaction_id INTEGER,
    asset_category_id INTEGER,
    expense_category_id INTEGER,
    amount BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (bank_transaction_id) REFERENCES bank_transactions(id) ON DELETE CASCADE,
    FOREIGN KEY (card_transaction_id) REFERENCES card_transactions(id) ON DELETE CASCADE,
    FOREIGN KEY (asset_category_id) REFERENCES asset_categories(id) ON DELETE SET NULL,
    FOREIGN KEY (expense_category_id) REFERENCES expense_categories(id) ON DELETE SET NULL,
    CHECK (
        (bank_transaction_id IS NOT NULL AND card_transaction_id IS NULL) OR
        (bank_transaction_id IS NULL AND card_transaction_id IS NOT NULL) OR
        (bank_transaction_id IS NULL AND card_transaction_id IS NULL)
    )
);

CREATE INDEX idx_assets_bank_transaction ON assets(bank_transaction_id);
CREATE INDEX idx_assets_card_transaction ON assets(card_transaction_id);
CREATE INDEX idx_assets_asset_category ON assets(asset_category_id);
CREATE INDEX idx_assets_expense_category ON assets(expense_category_id);

-- 自動登録する資産カテゴリを管理するテーブル
CREATE TABLE auto_category_rules (
    id SERIAL PRIMARY KEY,
    asset_category_id INTEGER NOT NULL,
    description_pattern VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (asset_category_id) REFERENCES asset_categories(id) ON DELETE CASCADE
);

CREATE INDEX idx_auto_category_rules_asset_category ON auto_category_rules(asset_category_id);

-- 収入の分割割合を管理するテーブル
CREATE TABLE income_split_patterns (
    id INTEGER NOT NULL,
    asset_category_id INTEGER NOT NULL,
    split_ratio DECIMAL(5, 2) NOT NULL, -- 例: 50.00 for 50%
    pattern_name VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (asset_category_id) REFERENCES asset_categories(id) ON DELETE CASCADE,
    PRIMARY KEY (id, asset_category_id),
    CHECK (split_ratio >= 0 AND split_ratio <= 100)
);

CREATE INDEX idx_income_split_patterns_id ON income_split_patterns(id);

-- 分割済みの収入額を管理するテーブル
CREATE TABLE split_incomes (
    id SERIAL PRIMARY KEY,
    asset_category_id INTEGER NOT NULL,
    split_pattern_id INTEGER NOT NULL,
    amount BIGINT NOT NULL,
    bank_transaction_id INTEGER,
    card_transaction_id INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (asset_category_id) REFERENCES asset_categories(id) ON DELETE CASCADE,
    FOREIGN KEY (bank_transaction_id) REFERENCES bank_transactions(id) ON DELETE CASCADE,
    FOREIGN KEY (card_transaction_id) REFERENCES card_transactions(id) ON DELETE CASCADE,
    CHECK (
        (bank_transaction_id IS NOT NULL AND card_transaction_id IS NULL) OR
        (bank_transaction_id IS NULL AND card_transaction_id IS NOT NULL)
    )
);

CREATE INDEX idx_split_incomes_asset_category ON split_incomes(asset_category_id);
CREATE INDEX idx_split_incomes_split_pattern ON split_incomes(split_pattern_id);
CREATE INDEX idx_split_incomes_bank_transaction ON split_incomes(bank_transaction_id);
CREATE INDEX idx_split_incomes_card_transaction ON split_incomes(card_transaction_id);

