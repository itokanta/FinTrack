# 収入の手動分割機能

## 概要

収入を資産カテゴリごとに金額を指定して分割できる機能を追加しました。

従来の方法:
1. 単一の資産カテゴリに全額登録
2. 分割パターン（割合指定）で自動分割

新機能:
3. **手動で金額を指定して分割** ← NEW!

## データベース変更

### マイグレーション実行

```bash
# 1. split_methodカラムの追加
psql -U your_username -d fintrack -f sql/06_add_split_method_to_split_incomes.sql

# 2. 既存の手動分割データの修正（必要な場合のみ）
psql -U your_username -d fintrack -f sql/07_fix_existing_manual_splits.sql
```

**注意**: `07_fix_existing_manual_splits.sql`は、マイグレーション前に手動分割したデータがある場合に必要です。このスクリプトは、`split_method='MANUAL'`のレコードがある取引の`income_split_pattern_id`を`-1`に設定します。

### 変更内容

#### split_incomesテーブル
- `split_method` カラムを追加（NOT NULL, VARCHAR(20)）
  - 'PATTERN': 分割パターンを使用した分割
  - 'MANUAL': 手動での金額指定分割
  - 'SINGLE': 単一の資産カテゴリに全額登録

- `split_pattern_id` カラムをNULL許容に変更
  - PATTERN分割の場合のみ値を持つ
  - MANUALとSINGLEの場合はNULL

## コード変更

### 新規作成ファイル

1. **SplitMethod.java** - 分割方法を表すEnum

### 更新ファイル

1. **SplitIncome.java**
   - `splitMethod` フィールドを追加

2. **SplitIncomeRepository.java**
   - INSERT/UPDATE時に `split_method` を含めるよう変更

3. **SplitIncomeRowMapper.java**
   - `split_method` のマッピングを追加
   - `split_pattern_id` のNULL対応

4. **IncomeSplitService.java**
   - `splitIncomeManual()` メソッドを追加（手動分割処理）
   - 既存メソッドに `splitMethod` の設定を追加

5. **TransactionController.java**
   - `splitIncomeManual()` エンドポイントを追加

6. **income-detail.html**
   - 手動分割フォームを追加
   - リアルタイムで合計金額と残額を表示
   - 金額が一致しない場合は送信ボタンを無効化

## 使い方

### 1. 収入一覧から収入を選択

`/transactions/incomes` で収入を選択し、詳細画面へ

### 2. 手動分割フォームに金額を入力

「金額を指定して分割」セクションで:
1. 各資産カテゴリに分割したい金額を入力
2. 合計金額が収入金額と一致すると送信ボタンが有効化
3. 「分配する」をクリック

### 例

収入: 300,000円

分割例:
- 普通預金: 200,000円
- 貯蓄口座: 50,000円  
- 投資口座: 50,000円

合計: 300,000円 ✓

## データ構造の例

### PATTERN分割の場合
```sql
# split_incomesテーブル
split_pattern_id: 1（パターンID）
split_method: 'PATTERN'

# transactionsテーブル
income_split_pattern_id: 1（パターンID）
```

### MANUAL分割の場合
```sql
# split_incomesテーブル
split_pattern_id: NULL
split_method: 'MANUAL'

# transactionsテーブル
income_split_pattern_id: -1（手動分割を表す特殊値）
```

### SINGLE分割の場合
```sql
# split_incomesテーブル
split_pattern_id: NULL
split_method: 'SINGLE'

# transactionsテーブル
income_split_pattern_id: 0（単一カテゴリ分割を表す特殊値）
```

### 分配ステータスの判定
- `income_split_pattern_id IS NULL` → 未分配
- `income_split_pattern_id IS NOT NULL` → 分配済み
  - `>= 1`: パターン分割
  - `0`: 単一カテゴリ分割
  - `-1`: 手動分割

## 設計の利点

1. **明示的な分割方法**
   - `split_method` カラムでデータの意味が明確

2. **拡張性**
   - 将来的に新しい分割方法を追加しやすい

3. **データ整合性**
   - チェック制約により不正な値を防止
   - 合計金額の一致をバリデーション

4. **ユーザビリティ**
   - リアルタイムフィードバックで入力ミスを防止
   - 3つの分割方法から選択可能

## 注意事項

- 手動分割では、合計金額が収入金額と完全に一致する必要があります
- 各資産カテゴリは自分のユーザーに属するもののみ選択可能
- 一度分割した収入は再分割できません（現在の仕様）

## トラブルシューティング

### 問題: 手動分割したのに収入一覧で「未分配」と表示される

**原因**: マイグレーション前に手動分割したデータは、`income_split_pattern_id`が更新されていない可能性があります。

**解決方法**:
```bash
psql -U your_username -d fintrack -f sql/07_fix_existing_manual_splits.sql
```

このスクリプトは、`split_incomes`テーブルに`split_method='MANUAL'`のレコードがある取引を検索し、該当する取引の`income_split_pattern_id`を`-1`に設定します。

**手動で確認する場合**:
```sql
-- 手動分割されているが、income_split_pattern_idが設定されていない取引を確認
SELECT 
    bt.id,
    bt.description,
    bt.amount,
    bt.income_split_pattern_id,
    si.split_method
FROM bank_transactions bt
INNER JOIN split_incomes si ON si.bank_transaction_id = bt.id
WHERE si.split_method = 'MANUAL'
AND (bt.income_split_pattern_id IS NULL OR bt.income_split_pattern_id = 0);
```

