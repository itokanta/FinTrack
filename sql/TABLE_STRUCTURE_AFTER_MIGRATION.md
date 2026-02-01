# マイグレーション後のテーブル構造

## split_patterns テーブル

分割パターンを管理するテーブル（収入・支出統合版）

### カラム構成

| カラム名 | 型 | NULL | デフォルト | 説明 |
|---------|-----|------|-----------|------|
| **pk_id** | SERIAL | NO | 自動採番 | **プライマリキー（新規追加）** |
| id | INTEGER | NO | - | パターンID（同じ値で複数レコード） |
| user_id | INTEGER | NO | - | ユーザーID |
| transaction_type | VARCHAR(10) | NO | - | 'INCOME'(収入) or 'EXPENSE'(支出) **[新規]** |
| split_type | VARCHAR(10) | NO | - | 'RATIO'(割合) or 'AMOUNT'(金額) **[新規]** |
| asset_category_id | INTEGER | NO | - | 資産カテゴリID（収入・支出共通） |
| split_ratio | DECIMAL(5,2) | YES | - | 割合（split_type='RATIO'時のみ） |
| split_amount | BIGINT | YES | - | 金額（split_type='AMOUNT'時のみ） **[新規]** |
| pattern_name | VARCHAR(100) | YES | - | パターン名 |
| created_at | TIMESTAMP | NO | CURRENT_TIMESTAMP | 作成日時 |

### 制約

- **PRIMARY KEY**: `pk_id`
- **FOREIGN KEY**: `user_id` → `users(id)`
- **FOREIGN KEY**: `asset_category_id` → `asset_categories(id)`
- **CHECK**: `transaction_type IN ('INCOME', 'EXPENSE')`
- **CHECK**: `split_type IN ('RATIO', 'AMOUNT')`
- **CHECK**: 
  - `split_type='RATIO'` の場合: `split_amount IS NULL` かつ `split_ratio IS NOT NULL`
  - `split_type='AMOUNT'` の場合: `split_amount IS NOT NULL` かつ `split_ratio IS NULL`

### インデックス

- `idx_split_patterns_pattern_id` ON `(user_id, id)` - パターン検索用
- `idx_split_patterns_user_id` ON `(user_id)` - ユーザー別検索用
- `idx_split_patterns_id` ON `(id)` - パターンID検索用

### データ例

#### 収入の割合指定パターン（例: パターンID=1）

| pk_id | id | user_id | transaction_type | split_type | asset_category_id | split_ratio | split_amount | pattern_name |
|-------|----|---------|--------------------|------------|-------------------|-------------|--------------|--------------|
| 1 | 1 | 2 | INCOME | RATIO | 1 | 50.00 | NULL | 給料分配 |
| 2 | 1 | 2 | INCOME | RATIO | 2 | 30.00 | NULL | 給料分配 |
| 3 | 1 | 2 | INCOME | RATIO | 3 | 20.00 | NULL | 給料分配 |

#### 支出の金額指定パターン（例: パターンID=2）

| pk_id | id | user_id | transaction_type | split_type | asset_category_id | split_ratio | split_amount | pattern_name |
|-------|----|---------|--------------------|------------|-------------------|-------------|--------------|--------------|
| 4 | 2 | 2 | EXPENSE | AMOUNT | 4 | NULL | 80000 | 固定費 |
| 5 | 2 | 2 | EXPENSE | AMOUNT | 5 | NULL | 15000 | 固定費 |
| 6 | 2 | 2 | EXPENSE | AMOUNT | 6 | NULL | 10000 | 固定費 |

---

## split_transactions テーブル

分割済みの収支額を管理するテーブル（収入・支出統合版）

### カラム構成

| カラム名 | 型 | NULL | デフォルト | 説明 |
|---------|-----|------|-----------|------|
| id | SERIAL | NO | 自動採番 | プライマリキー |
| transaction_type | VARCHAR(10) | NO | - | 'INCOME'(収入) or 'EXPENSE'(支出) **[新規]** |
| asset_category_id | INTEGER | NO | - | 資産カテゴリID（収入・支出共通） |
| split_pattern_id | INTEGER | YES | - | 分割パターンID（手動分割時はNULL） |
| split_method | VARCHAR(20) | NO | - | 'PATTERN', 'MANUAL', 'SINGLE' |
| amount | BIGINT | NO | - | 分割金額 |
| bank_transaction_id | INTEGER | YES | - | 銀行取引ID |
| card_transaction_id | INTEGER | YES | - | カード取引ID |
| created_at | TIMESTAMP | NO | CURRENT_TIMESTAMP | 作成日時 |
| updated_at | TIMESTAMP | NO | CURRENT_TIMESTAMP | 更新日時 |

### 制約

- **PRIMARY KEY**: `id`
- **FOREIGN KEY**: `asset_category_id` → `asset_categories(id)`
- **FOREIGN KEY**: `bank_transaction_id` → `bank_transactions(id)`
- **FOREIGN KEY**: `card_transaction_id` → `card_transactions(id)`
- **CHECK**: `transaction_type IN ('INCOME', 'EXPENSE')`
- **CHECK**: `split_method IN ('PATTERN', 'MANUAL', 'SINGLE')`
- **CHECK**: 銀行取引IDとカード取引IDは排他的（どちらか一方のみ）

### インデックス

- `idx_split_transactions_asset_category` ON `(asset_category_id)`
- `idx_split_transactions_split_pattern` ON `(split_pattern_id)`
- `idx_split_transactions_bank_transaction` ON `(bank_transaction_id)`
- `idx_split_transactions_card_transaction` ON `(card_transaction_id)`

### データ例

#### 収入のパターン分割

| id | transaction_type | asset_category_id | split_pattern_id | split_method | amount | bank_transaction_id |
|----|------------------|-------------------|------------------|--------------|--------|---------------------|
| 1 | INCOME | 1 | 1 | PATTERN | 150000 | 100 |
| 2 | INCOME | 2 | 1 | PATTERN | 90000 | 100 |
| 3 | INCOME | 3 | 1 | PATTERN | 60000 | 100 |

#### 支出の手動分割

| id | transaction_type | asset_category_id | split_pattern_id | split_method | amount | bank_transaction_id |
|----|------------------|-------------------|------------------|--------------|--------|---------------------|
| 4 | EXPENSE | 4 | NULL | MANUAL | 5000 | 101 |
| 5 | EXPENSE | 5 | NULL | MANUAL | 3000 | 101 |
| 6 | EXPENSE | 6 | NULL | MANUAL | 2000 | 101 |

---

## bank_transactions / card_transactions テーブル

取引テーブル（既存テーブルに新規カラム追加）

### 新規追加カラム

| カラム名 | 型 | NULL | デフォルト | 説明 |
|---------|-----|------|-----------|------|
| expense_split_pattern_id | INTEGER | YES | NULL | 支出分割パターンID **[新規追加]** |

### 既存カラム（主要なもの）

| カラム名 | 型 | NULL | 説明 |
|---------|-----|------|------|
| id | SERIAL | NO | プライマリキー |
| user_id | INTEGER | NO | ユーザーID |
| description | VARCHAR(500) | NO | 摘要 |
| amount | BIGINT | NO | 金額 |
| transaction_type | VARCHAR(20) | NO | 'INCOME' or 'EXPENSE' |
| transaction_date | DATE | NO | 取引日 |
| asset_category_id | INTEGER | YES | 資産カテゴリID |
| expense_category_id | INTEGER | YES | 支出カテゴリID |
| income_split_pattern_id | INTEGER | YES | 収入分割パターンID（既存） |
| expense_split_pattern_id | INTEGER | YES | 支出分割パターンID（新規） |

### パターンID の意味

| 値 | 意味 |
|----|------|
| NULL | 未分配 |
| >= 1 | パターン分割（split_patternsテーブルのid） |
| 0 | 単一カテゴリ分割 |
| -1 | 手動分割 |

---

## SERIAL型の自動採番について

### 既存データへの影響

`pk_id SERIAL`を追加すると、既存のレコードには**自動的に1から順番に番号が振られます**。

例えば、既存に3レコードある場合:

**マイグレーション前:**
| id | user_id | asset_category_id | split_ratio | pattern_name |
|----|---------|-------------------|-------------|--------------|
| 1 | 2 | 1 | 50.00 | 給料分配 |
| 1 | 2 | 2 | 30.00 | 給料分配 |
| 1 | 2 | 3 | 20.00 | 給料分配 |

**マイグレーション後:**
| **pk_id** | id | user_id | asset_category_id | split_ratio | pattern_name | transaction_type | split_type |
|-----------|----|---------|--------------------|-------------|--------------|------------------|------------|
| **1** | 1 | 2 | 1 | 50.00 | 給料分配 | INCOME | RATIO |
| **2** | 1 | 2 | 2 | 30.00 | 給料分配 | INCOME | RATIO |
| **3** | 1 | 2 | 3 | 20.00 | 給料分配 | INCOME | RATIO |

新しいレコードを追加すると、`pk_id`は4, 5, 6...と続きます。

---

## まとめ

### 主な変更点

1. **プライマリキーの変更**
   - 旧: `(user_id, id, asset_category_id)` の複合キー
   - 新: `pk_id` の単一キー
   - `id`はパターンIDとして引き続き使用（複数レコードで同じ値を持つ）

2. **収支の統合**
   - `transaction_type`カラムで収入/支出を判別
   - 収入も支出も`asset_categories`を使用
   - `expense_categories`は使用しない

3. **分割方法の拡張**
   - `split_type`カラムで割合/金額を判別
   - 割合指定: `split_ratio`使用
   - 金額指定: `split_amount`使用

4. **データの互換性**
   - 既存の収入分割データは自動的に`INCOME`+`RATIO`として保持
   - 既存のアプリケーション動作に影響なし

