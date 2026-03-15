# CSV取込削除機能 作業手順書

登録したCSVの内容を削除する機能の実装手順です。**新規追加された項目のみを削除**し、アップデートがあった項目はロールバックする仕様とします。

---

## 前提・設計まとめ

- **登録履歴**: 1 CSV につき 1 レコード。`import_type` で BANK / CARD を区別。
- **取引との関係**: `bank_transactions` / `card_transactions` に `csv_import_history_id`（NULLable）を追加。
- **アップデート履歴**: payment_month / is_paid の変更を 1 更新 1 レコードで記録。値は TEXT で前後を保持。
- **削除時**: 資産の巻き戻し → split_transactions 削除 → card_payment_discrepancies 削除 → 更新のロールバック → 取引削除 → 履歴削除の順で実行。

---

## Phase 1: データベース変更

### 1.1 登録履歴テーブルの作成

**ファイル**: `sql/13_add_csv_import_history.sql`（新規）

```sql
-- 登録履歴テーブル（1 CSV = 1 レコード）
CREATE TABLE csv_import_histories (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    import_type VARCHAR(10) NOT NULL,  -- 'BANK' or 'CARD'
    csv_name VARCHAR(255) NOT NULL,
    import_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    record_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_csv_import_histories_user_id ON csv_import_histories(user_id);
CREATE INDEX idx_csv_import_histories_import_date ON csv_import_histories(import_date);
```

### 1.2 取引テーブルへのリレーション追加

**上記と同じファイルに追記**

```sql
-- bank_transactions に登録履歴FKを追加
ALTER TABLE bank_transactions
ADD COLUMN csv_import_history_id INTEGER REFERENCES csv_import_histories(id) ON DELETE SET NULL;

CREATE INDEX idx_bank_transactions_csv_import_history ON bank_transactions(csv_import_history_id);

-- card_transactions に登録履歴FKを追加
ALTER TABLE card_transactions
ADD COLUMN csv_import_history_id INTEGER REFERENCES csv_import_histories(id) ON DELETE SET NULL;

CREATE INDEX idx_card_transactions_csv_import_history ON card_transactions(csv_import_history_id);
```

### 1.3 アップデート項目管理テーブルの作成

**上記と同じファイルに追記**

```sql
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
```

### 1.4 取込単位の資産差分テーブル（推奨）

削除時に資産を正確に巻き戻すため、取込ごとの資産差分を記録するテーブルを追加する。

**上記と同じファイルに追記**

```sql
-- 取込ごとの資産カテゴリ別差分（削除時の巻き戻し用）
CREATE TABLE csv_import_asset_deltas (
    id SERIAL PRIMARY KEY,
    csv_import_history_id INTEGER NOT NULL,
    asset_category_id INTEGER,
    amount_delta BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (csv_import_history_id) REFERENCES csv_import_histories(id) ON DELETE CASCADE,
    FOREIGN KEY (asset_category_id) REFERENCES asset_categories(id) ON DELETE SET NULL
);

CREATE INDEX idx_csv_import_asset_deltas_import ON csv_import_asset_deltas(csv_import_history_id);
```

### 1.5 マイグレーション実行

```bash
psql -U your_username -d FinTrack_dev -f sql/13_add_csv_import_history.sql
```

---

## Phase 2: ドメイン・リポジトリ層

### 2.1 ドメインクラスの追加

| ファイル | 内容 |
|----------|------|
| `domain/CsvImportHistory.java` | id, userId, importType, csvName, importDate, recordCount, createdAt |
| `domain/CsvImportUpdateHistory.java` | id, csvImportHistoryId, bankTransactionId, cardTransactionId, targetField, valueBefore, valueAfter, createdAt, userId |
| `domain/CsvImportAssetDelta.java` | id, csvImportHistoryId, assetCategoryId, amountDelta, createdAt |

- 既存の `BankTransaction` / `CardTransaction` に `csvImportHistoryId`（Long, nullable）を追加する。

### 2.2 リポジトリの追加・変更

| 作業 | 内容 |
|------|------|
| **新規** | `CsvImportHistoryRepository` … 保存・ID取得・user_id 一覧・id 取得 |
| **新規** | `CsvImportUpdateHistoryRepository` … 保存・csv_import_history_id 指定一覧・削除 |
| **新規** | `CsvImportAssetDeltaRepository` … 保存（複数）・csv_import_history_id 指定一覧 |
| **変更** | `BankTransactionRepository` … INSERT/UPDATE に `csv_import_history_id` を追加、findByCsvImportHistoryId を追加 |
| **変更** | `CardTransactionRepository` … 同上 |

- RowMapper / 既存の SELECT で `csv_import_history_id` を取得・設定するよう修正する。

---

## Phase 3: CSV取込処理の改修（履歴・差分の記録）

### 3.1 銀行CSV取込（CsvImportService.importBankTransactions）

1. **取込開始時**
   - `CsvImportHistory` を 1 件作成（import_type=BANK, csv_name=ファイル名, record_count=0）。
   - 保存して ID を取得。

2. **各 BankTransaction の INSERT 時**
   - エンティティに `csvImportHistoryId` を設定して保存。
   - 資産を更新する前に、その取引の `(asset_category_id, amount_delta)` をメモし、取込終了後に `csv_import_asset_deltas` にまとめて INSERT（カード引き落としでない場合のみ資産更新＋差分記録）。

3. **カード引き落とし行の処理（markAsPaid 前）**
   - 対象となる `payment_month` の CardTransaction 一覧を取得。
   - 各 CardTransaction について `CsvImportUpdateHistory` を 1 件ずつ作成（csv_import_history_id=上記履歴ID, card_transaction_id, target_field='is_paid', value_before='false', value_after='true'）して保存。
   - その後、従来どおり `markAsPaidByPaymentMonth` を実行。

4. **差異がある場合**
   - CardPaymentDiscrepancy の保存は従来どおり（bank_transaction_id で紐づくため、削除時にこの取込に紐づく bank を消せば discrepancy も削除対象に含められる）。

5. **取込終了時**
   - 登録件数を確定し、`CsvImportHistory` の `record_count` を UPDATE。

### 3.2 カードCSV取込（CsvImportService.importCardTransactions）

1. **取込開始時**
   - `CsvImportHistory` を 1 件作成（import_type=CARD, csv_name=ファイル名, record_count=0）。
   - 保存して ID を取得。

2. **各 CardTransaction の INSERT 時**
   - エンティティに `csvImportHistoryId` を設定して保存。
   - 資産更新分の `(asset_category_id, amount_delta)` をメモし、取込終了後に `csv_import_asset_deltas` に INSERT。

3. **payment_month の更新時（extractTransactionsToUpdate で更新する各件）**
   - 更新前に、対象 CardTransaction の現在の `payment_month` を取得。
   - 更新後の値（CSV 側の payment_month）と合わせて、`CsvImportUpdateHistory` を 1 件作成（card_transaction_id, target_field='payment_month', value_before/value_after）して保存。
   - その後、従来どおり `cardTransactionRepository.save`。

4. **取込終了時**
   - 登録件数（新規 INSERT 数 ＋ 更新数）を確定し、`CsvImportHistory` の `record_count` を UPDATE。

---

## Phase 4: 削除処理の実装

### 4.1 削除サービス（新規 or CsvImportService 内）

**処理順序（必ずこの順で実行）**

1. **対象の CsvImportHistory を取得**（存在・user_id の一致を確認）。
2. **資産の巻き戻し**
   - `csv_import_asset_deltas` で当該 `csv_import_history_id` の一覧を取得。
   - 各レコードの `amount_delta` の符号を反転して `assetService.updateBalance` を実行。
3. **削除対象の取引ID一覧を取得**
   - `bank_transactions` または `card_transactions` の `csv_import_history_id = ?` で取得。
4. **split_transactions の削除**
   - 上記の bank_transaction_id / card_transaction_id に紐づく行を削除。
5. **card_payment_discrepancies の削除**
   - 上記の bank_transaction_id 一覧に含まれる `bank_transaction_id` を持つレコードを削除。
6. **更新のロールバック**
   - `csv_import_update_histories` で当該 `csv_import_history_id` の一覧を取得。
   - 各レコードについて、`card_transaction_id` の CardTransaction の `target_field` に対応するカラムを `value_before` に復元（payment_month は文字列、is_paid は Boolean.valueOf(value_before)）。
7. **取引の削除**
   - 上記で取得した bank_transaction / card_transaction を削除。
8. **履歴の削除**
   - `csv_import_asset_deltas` → `csv_import_update_histories` → `csv_import_histories` の順で削除（または CASCADE に任せる場合は履歴のみ削除）。

- 全体を `@Transactional` で囲み、いずれかで失敗した場合はロールバックされるようにする。

### 4.2 資産差分を記録しない場合のフォールバック

- `csv_import_asset_deltas` を使わない場合は、削除対象の取引一覧とその split_transactions から「現在のカテゴリ別の効き分」を計算し、その逆を `updateBalance` で適用する。
- 取込時点の差分を残さないため、削除時計算が複雑になるため、Phase 1.4 のテーブルを入れることを推奨。

---

## Phase 5: 画面実装

### 5.1 ヘッダーメニューの変更

**ファイル**: `src/main/resources/templates/fragments/header.html`

- 現在の「CSV取込」単体リンクを、**プルダウン**に変更する。
- プルダウン内のメニュー:
  - **CSV取込** → `/csv/import`（既存）
  - **CSV取込履歴** → `/csv/import/history`（新規）

（既存の「収支管理」「分割管理」などの dropdown と同じパターンで実装。）

### 5.2 CSV取込履歴一覧

- **URL**: `/csv/import/history`（GET）
- **コントローラー**: 認証ユーザーの `csv_import_histories` を登録日の降順で取得し、テンプレートに渡す。
- **テンプレート**: テーブル形式。列は **csv名** / **登録日** / **登録件数** の 3 列と、各行に **詳細** ボタン。
- **詳細ボタン**: `/csv/import/history/{id}` へリンク。

### 5.3 取込詳細（取引一覧）

- **URL**: `/csv/import/history/{id}`（GET）
- **コントローラー**:
  - `id` で `CsvImportHistory` を取得。存在しない、または他ユーザーなら 404。
  - `import_type` に応じて、`bank_transactions` または `card_transactions` の `csv_import_history_id = id` で一覧取得（削除前のデータなので、履歴に紐づく取引が表示される）。
- **テンプレート**: 取引一覧（日付・摘要・金額・種別など必要な項目）を表示。
- **削除ボタン**: 画面内に 1 つ配置。クリックで確認ポップアップ（モーダル or confirm）を表示し、OK なら削除実行。

### 5.4 削除実行

- **URL**: `/csv/import/history/{id}/delete`（POST）など、削除専用の POST エンドポイントを用意。
- **コントローラー**: 認証ユーザーと履歴の user_id を照合し、問題なければ Phase 4 の削除サービスを呼び出す。成功時は `/csv/import/history` へリダイレクトし、フラッシュメッセージで「削除しました」を表示。
- 確認ポップアップは「このCSV取込で登録・更新したデータをすべて取り消します。よろしいですか？」のような文言を推奨。

---

## Phase 6: 結合確認・テスト

- [ ] 銀行CSV取込 → 履歴一覧に 1 件出る。詳細で該当 bank_transactions が表示される。
- [ ] カードCSV取込 → 履歴一覧に 1 件出る。詳細で該当 card_transactions が表示される。
- [ ] 詳細画面から削除実行 → 該当取引・split・discrepancy が削除され、資産が取込前の状態に戻る。
- [ ] カードの payment_month のみ更新した取込を削除 → 該当 CardTransaction の payment_month が value_before に戻り、取引自体は残る。
- [ ] 銀行CSVで is_paid を true にした取込を削除 → 該当 CardTransaction の is_paid が false に戻り、紐づく BankTransaction と discrepancy が削除される。
- [ ] 他ユーザーの履歴 ID で詳細・削除にアクセスしても 404 または 403 になること。

---

## ファイル一覧（追加・変更予定）

| 種別 | パス |
|------|------|
| SQL | `sql/13_add_csv_import_history.sql` |
| Domain | `domain/CsvImportHistory.java`, `CsvImportUpdateHistory.java`, `CsvImportAssetDelta.java` |
| Domain 変更 | `BankTransaction.java`, `CardTransaction.java`（csvImportHistoryId 追加） |
| Repository | `CsvImportHistoryRepository.java`, `CsvImportUpdateHistoryRepository.java`, `CsvImportAssetDeltaRepository.java` |
| Repository 変更 | `BankTransactionRepository.java`, `CardTransactionRepository.java`（および RowMapper） |
| Service 変更 | `CsvImportService.java`（取込時の履歴・差分・更新履歴の記録） |
| Service 新規 | `CsvImportDeleteService.java`（または CsvImportService にメソッド追加） |
| Controller 新規/変更 | `CsvController.java`（履歴一覧・詳細・削除 POST） |
| テンプレート | `fragments/header.html`（CSV プルダウン化）, `csv/import-history.html`, `csv/import-history-detail.html` |

---

## メモ（アップデート複数回の扱い）

- 2 回以上アップデートが入った場合:
  - **最新の取込を削除**: その取込に紐づく `csv_import_update_histories` の `value_before` に戻す（1 つ前の状態になる）。
  - **1 回目の取込を削除**: その取込に紐づく更新履歴の `value_before` が「初回登録の値」なので、それに戻せばよい。
- 更新履歴は「1 更新 1 レコード」で時系列で並ぶため、同じ card_transaction_id に複数レコードが並んでも、削除時は「その csv_import_history_id に紐づく行だけ」をロールバック対象にすればよい。

---

以上で、CSV取込削除機能の作業手順は一通り揃っています。Phase 1 から順に実施し、各 Phase 完了時に動作確認することを推奨します。
