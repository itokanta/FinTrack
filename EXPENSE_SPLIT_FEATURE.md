# 支出分割機能の実装

## 概要

収入分割機能を拡張し、支出も同様に分割できる機能を追加しました。

既存の機能:
1. 収入を資産カテゴリごとに分割
   - 割合指定での分割パターン
   - 金額指定での手動分割

**新機能**:
2. **支出を支出カテゴリごとに分割** ← NEW!
   - 割合指定での分割パターン
   - 金額指定での分割パターン（NEW!）
   - 金額指定での手動分割

## データベース変更

### マイグレーション実行手順

```bash
# 1. データベースのバックアップ
pg_dump -U your_username -d fintrack > backup_before_split_feature.sql

# 2. テーブル名の変更
psql -U your_username -d fintrack -f sql/08_rename_to_split_patterns.sql

# 3. カラムの追加と制約の設定
psql -U your_username -d fintrack -f sql/09_add_transaction_type_and_split_type.sql

# 4. 支出分割パターンID管理用カラムの追加
psql -U your_username -d fintrack -f sql/10_add_expense_split_pattern_id.sql
```

### 変更内容

#### テーブル名の変更
- `income_split_patterns` → `split_patterns` (収支統合)
- `split_incomes` → `split_transactions` (収支統合)

#### split_patternsテーブル
- `transaction_type` (VARCHAR(10), NOT NULL): 'INCOME' または 'EXPENSE'
- `split_type` (VARCHAR(10), NOT NULL): 'RATIO'(割合) または 'AMOUNT'(金額指定)
- `split_amount` (BIGINT): 金額指定時の金額
- `expense_category_id` (INTEGER): 支出分割時のカテゴリID
- `asset_category_id`, `split_ratio`: NULL許容に変更

#### split_transactionsテーブル
- `transaction_type` (VARCHAR(10), NOT NULL): 'INCOME' または 'EXPENSE'
- `expense_category_id` (INTEGER): 支出分割時のカテゴリID
- `asset_category_id`: NULL許容に変更

#### bank_transactions / card_transactionsテーブル
- `expense_split_pattern_id` (INTEGER): 支出分割パターンID

## コード変更

### 新規作成ファイル

#### ドメインモデル
1. **TransactionType.java** - 収支の種類を表すEnum
2. **SplitType.java** - 分割方法の種類を表すEnum
3. **SplitPattern.java** - 収支分割パターンのドメインモデル（旧: IncomeSplitPattern）
4. **SplitTransaction.java** - 分割済み収支のドメインモデル（旧: SplitIncome）

#### Repository層
5. **SplitPatternRepository.java** - 分割パターンのRepository
6. **SplitTransactionRepository.java** - 分割取引のRepository
7. **SplitPatternRowMapper.java** - 分割パターンのRowMapper
8. **SplitTransactionRowMapper.java** - 分割取引のRowMapper

#### Service層
9. **SplitService.java** - 収支分割管理サービス（収入・支出統合版）

#### Controller層
10. **SplitController.java** - 収支分割管理コントローラー

#### テンプレート
11. **split/patterns.html** - 分割パターン一覧画面
12. **split/pattern-form.html** - 分割パターン作成画面
13. **transactions/expense-detail.html** - 支出詳細画面（更新）

### 更新ファイル

1. **BankTransaction.java** / **CardTransaction.java**
   - `expenseSplitPatternId` フィールドを追加

2. **TransactionController.java**
   - `SplitService` を注入
   - `expenseDetail()` メソッドを更新（分割パターンと分割状況を追加）

## 使い方

### 1. 支出分割パターンの作成

#### 割合指定パターン
1. `/split/expense/patterns` にアクセス
2. 「新規パターン作成」をクリック
3. 「割合指定」を選択
4. パターン名と各カテゴリの割合を入力（合計100%）
5. 「登録」をクリック

例:
- パターン名: 「買い物分割」
- 食費: 60%
- 日用品: 30%
- その他: 10%

#### 金額指定パターン
1. `/split/expense/patterns` にアクセス
2. 「新規パターン作成」をクリック
3. 「金額指定」を選択
4. パターン名と各カテゴリの金額を入力
5. 「登録」をクリック

例:
- パターン名: 「定期支払い」
- 家賃: 80,000円
- 光熱費: 15,000円
- 通信費: 10,000円

### 2. 支出の分割

#### 手動分割（金額指定）
1. `/transactions/expenses` で支出を選択
2. 詳細画面で「金額を指定して分割」セクションへ
3. 各支出カテゴリに分割する金額を入力
4. 合計が支出金額と一致すると「分配する」ボタンが有効化
5. 「分配する」をクリック

例: 支出 10,000円
- 食費: 6,000円
- 交通費: 3,000円
- その他: 1,000円

#### パターンで分割
1. `/transactions/expenses` で支出を選択
2. 詳細画面で「分割パターンで分配」セクションへ
3. 登録済みのパターンを選択
4. 「分配する」をクリック

### 3. 収入の分割（既存機能も拡張）

収入分割も同様に、割合指定と金額指定の両方に対応しています。
- 収入分割パターン: `/split/income/patterns`
- 収入詳細: `/transactions/incomes/{id}`

## データ構造の例

### 収入のパターン分割（割合指定）
```sql
# split_patternsテーブル
id: 1
transaction_type: 'INCOME'
split_type: 'RATIO'
asset_category_id: 1
split_ratio: 50.00
pattern_name: '給料分配'

# split_transactionsテーブル
transaction_type: 'INCOME'
asset_category_id: 1
split_pattern_id: 1
split_method: 'PATTERN'
amount: 150000

# bank_transactionsテーブル
income_split_pattern_id: 1
```

### 支出のパターン分割（金額指定）
```sql
# split_patternsテーブル
id: 2
transaction_type: 'EXPENSE'
split_type: 'AMOUNT'
expense_category_id: 1
split_amount: 80000
pattern_name: '固定費'

# split_transactionsテーブル
transaction_type: 'EXPENSE'
expense_category_id: 1
split_pattern_id: 2
split_method: 'PATTERN'
amount: 80000

# bank_transactionsテーブル
expense_split_pattern_id: 2
```

### 支出の手動分割
```sql
# split_transactionsテーブル
transaction_type: 'EXPENSE'
expense_category_id: 1
split_pattern_id: NULL
split_method: 'MANUAL'
amount: 6000

# bank_transactionsテーブル
expense_split_pattern_id: -1
```

## 設計の利点

1. **統合設計**
   - 収入と支出で同じテーブル構造を使用
   - コードの重複を避け、保守性向上

2. **柔軟な分割方法**
   - 割合指定: 給与などの変動する金額に対応
   - 金額指定: 家賃などの固定費に対応
   - 手動分割: 一度きりの特殊な分割に対応

3. **拡張性**
   - 新しい分割方法を簡単に追加可能
   - 将来的な機能拡張に対応しやすい設計

4. **データ整合性**
   - チェック制約により不正な値を防止
   - 収入/支出の判別が明確

## 既存機能との互換性

- 既存の収入分割データは自動的に `transaction_type='INCOME'`, `split_type='RATIO'` として保持されます
- 既存の収入分割機能は引き続き使用可能です
- IncomeSplitServiceも残っているため、既存のコードは動作します（ただし、新規開発はSplitServiceを使用することを推奨）

## 今後の拡張候補

1. 収支分割の一括処理機能
2. 分割パターンのコピー機能
3. 分割履歴の表示機能
4. 分割パターンの編集機能
5. 支出カテゴリ別の残高管理

