# 支出分割機能の実装（最終版）

## 概要

収入分割機能を拡張し、支出も同様に分割できる機能を追加しました。

**重要な設計変更**:
- 支出の分割も`asset_categories`（資産カテゴリ）を使用
- `expense_categories`は従来通り支出の用途分類として使用（分割には使わない）
- プライマリキーを`SERIAL`の単一キーに変更

## データベース変更

### マイグレーション実行手順

```bash
# 1. データベースのバックアップ
pg_dump -U your_username -d fintrack > backup_before_split_feature.sql

# 2. テーブル名の変更
psql -U your_username -d fintrack -f sql/08_rename_to_split_patterns.sql

# 3. カラムの追加と制約の設定（v3を使用）
psql -U your_username -d fintrack -f sql/09_add_transaction_type_and_split_type_v3.sql

# 4. 支出分割パターンID管理用カラムの追加
psql -U your_username -d fintrack -f sql/10_add_expense_split_pattern_id.sql
```

### 主な変更点

#### 1. テーブル名の変更
- `income_split_patterns` → `split_patterns`
- `split_incomes` → `split_transactions`

#### 2. split_patternsテーブル

**プライマリキーの変更**:
- 旧: `PRIMARY KEY (user_id, id, asset_category_id)`
- 新: `PRIMARY KEY (pk_id)` ← SERIAL型の新カラム

**新規カラム**:
- `pk_id` (SERIAL): プライマリキー（自動採番）
- `transaction_type` (VARCHAR(10), NOT NULL): 'INCOME' or 'EXPENSE'
- `split_type` (VARCHAR(10), NOT NULL): 'RATIO'(割合) or 'AMOUNT'(金額)
- `split_amount` (BIGINT): 金額指定時の金額

**変更**:
- `split_ratio`: NULL許容に変更（金額指定時はNULL）
- `asset_category_id`: NOT NULLのまま（収入・支出共通で使用）

#### 3. split_transactionsテーブル

**新規カラム**:
- `transaction_type` (VARCHAR(10), NOT NULL): 'INCOME' or 'EXPENSE'

**変更なし**:
- `asset_category_id`: NOT NULLのまま（収入・支出共通で使用）

#### 4. bank_transactions / card_transactionsテーブル

**新規カラム**:
- `expense_split_pattern_id` (INTEGER): 支出分割パターンID

## 既存データへの影響

### SERIAL型の自動採番

`pk_id SERIAL`を追加すると、既存のレコードには自動的に1から順番に番号が振られます。

**例**: 既存に3レコードある場合

| **pk_id** (新) | id | user_id | asset_category_id | split_ratio | pattern_name |
|----------------|----|---------|--------------------|-------------|--------------|
| **1** | 1 | 2 | 1 | 50.00 | 給料分配 |
| **2** | 1 | 2 | 2 | 30.00 | 給料分配 |
| **3** | 1 | 2 | 3 | 20.00 | 給料分配 |

新しいレコードは`pk_id=4`から始まります。

### 既存データの互換性

- 既存の収入分割データ: 自動的に`transaction_type='INCOME'`, `split_type='RATIO'`として保持
- 既存のアプリケーション動作: `id`カラムは引き続きパターンIDとして機能するため影響なし

## テーブル構造（最終版）

### split_patterns

| カラム名 | 型 | NULL | 説明 |
|---------|-----|------|------|
| **pk_id** | SERIAL | NO | プライマリキー（自動採番） |
| id | INTEGER | NO | パターンID（複数レコードで同じ値） |
| user_id | INTEGER | NO | ユーザーID |
| transaction_type | VARCHAR(10) | NO | 'INCOME' or 'EXPENSE' |
| split_type | VARCHAR(10) | NO | 'RATIO' or 'AMOUNT' |
| asset_category_id | INTEGER | NO | 資産カテゴリID（収入・支出共通） |
| split_ratio | DECIMAL(5,2) | YES | 割合（split_type='RATIO'時） |
| split_amount | BIGINT | YES | 金額（split_type='AMOUNT'時） |
| pattern_name | VARCHAR(100) | YES | パターン名 |
| created_at | TIMESTAMP | NO | 作成日時 |

### split_transactions

| カラム名 | 型 | NULL | 説明 |
|---------|-----|------|------|
| id | SERIAL | NO | プライマリキー |
| transaction_type | VARCHAR(10) | NO | 'INCOME' or 'EXPENSE' |
| asset_category_id | INTEGER | NO | 資産カテゴリID（収入・支出共通） |
| split_pattern_id | INTEGER | YES | 分割パターンID |
| split_method | VARCHAR(20) | NO | 'PATTERN', 'MANUAL', 'SINGLE' |
| amount | BIGINT | NO | 分割金額 |
| bank_transaction_id | INTEGER | YES | 銀行取引ID |
| card_transaction_id | INTEGER | YES | カード取引ID |
| created_at | TIMESTAMP | NO | 作成日時 |
| updated_at | TIMESTAMP | NO | 更新日時 |

## 使い方

### 1. 支出分割パターンの作成

#### 割合指定パターン
- URL: `/split/expense/patterns/new`
- 例: 買い物を「財布60% + クレカ40%」に分割

#### 金額指定パターン
- URL: `/split/expense/patterns/new`
- 例: 固定費を「家賃80,000円 + 光熱費15,000円」に分割

### 2. 支出の分割

#### 手動分割
- 支出詳細画面で各資産カテゴリに金額を入力
- 例: 10,000円の買い物を「現金6,000円 + カード4,000円」に分割

#### パターンで分割
- 登録済みのパターンを選択して自動分割

### 3. 資産カテゴリの用途

- **収入の分割**: 給料を「普通預金」「貯蓄」などに分配
- **支出の分割**: 買い物を「現金」「クレジットカード」などの支払元に分割

## コード変更

### 削除した概念
- ~~`expense_category_id`カラム~~（split_patterns, split_transactionsから削除）
- ~~`ExpenseCategoryRepository`依存~~（SplitService, SplitControllerから削除）

### 主な変更ファイル

1. **ドメインモデル**
   - `SplitPattern.java`: `pkId`追加、`expenseCategoryId`削除
   - `SplitTransaction.java`: `expenseCategoryId`削除

2. **Repository層**
   - `SplitPatternRepository.java`: `RETURNING pk_id`でINSERT
   - `SplitTransactionRepository.java`: `expense_category_id`削除

3. **Service層**
   - `SplitService.java`: `ExpenseCategoryRepository`削除、全て`assetCategoryRepository`使用

4. **Controller層**
   - `SplitController.java`: `ExpenseCategoryRepository`削除
   - `TransactionController.java`: 支出詳細画面で`assetCategoryMap`使用

5. **テンプレート**
   - `expense-detail.html`: `assetCategories`を使用して分割

## 設計の利点

1. **シンプルな設計**
   - 収入も支出も同じ`asset_categories`を使用
   - テーブル構造が統一され、理解しやすい

2. **柔軟な分割**
   - 割合指定: 変動する金額に対応
   - 金額指定: 固定費に対応

3. **拡張性**
   - SERIAL主キーにより、将来的なテーブル構造変更が容易

4. **データ整合性**
   - プライマリキーの重複問題を解決
   - チェック制約により不正な値を防止

## 詳細なテーブル構造図

詳細は `sql/TABLE_STRUCTURE_AFTER_MIGRATION.md` を参照してください。

