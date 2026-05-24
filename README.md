# FinTrack - 家計簿アプリケーション

Spring Boot で構築された個人向け家計管理 Web アプリケーションです。

## 機能

- **ユーザー認証**: Spring Security によるフォームログイン・ユーザー登録（BCrypt）
- **CSV 取込**: 銀行・カード会社の取引明細 CSV をインポート（複数金融機関対応）
- **CSV 登録履歴**: 取込履歴の参照・再取込時の更新差分管理
- **資産管理**: カテゴリ別の資産残高を管理
- **収支管理**: 収入・支出の一覧表示と詳細確認（銀行・カード統合）
- **収支分割**: 収入・支出を資産カテゴリへ分配（単一カテゴリ / 手動 / パターン）
- **分割パターン管理**: 収入・支出それぞれの分割パターン（割合または金額指定）
- **カテゴリ管理**: 資産カテゴリ・支出カテゴリの CRUD
- **自動カテゴリ付与**: 摘要パターンに基づく自動カテゴリ設定（画面から登録）
- **カード引き落とし照合**: 月別明細と銀行引き落としの照合・差異管理
- **重複排除**: fingerprint による取引の重複チェック

## 技術スタック

- **バックエンド**: Spring Boot 3.5.9, Java 21
- **データベース**: PostgreSQL
- **データアクセス**: Spring JDBC（`NamedParameterJdbcTemplate` + 手書き `RowMapper`）。JPA / Spring Data は使用しない
- **セキュリティ**: Spring Security 6
- **テンプレートエンジン**: Thymeleaf（Layout Dialect）
- **CSV 処理**: Apache Commons CSV
- **ビルドツール**: Maven

## セットアップ

### 1. データベースの準備

PostgreSQL で開発用データベースを作成してください：

```sql
CREATE DATABASE "FinTrack_dev";
```

### 2. データベース接続情報の設定

`src/main/resources/application.properties` を編集し、データベース接続情報を設定してください：

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/FinTrack_dev
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### 3. テーブルの作成（マイグレーション）

`sql/` ディレクトリ内の SQL を**ファイル名の昇順**で実行してください（`01` から `19` まで。`15_check.sql` は環境確認用のため通常は不要）：

```bash
for f in sql/0*.sql sql/1*.sql; do
  case "$(basename "$f")" in
    15_check.sql) continue ;;
    *) psql -U your_username -d FinTrack_dev -f "$f" ;;
  esac
done
```

個別に実行する場合の例：

```bash
psql -U your_username -d FinTrack_dev -f sql/01_create_tables.sql
psql -U your_username -d FinTrack_dev -f sql/02_redesign_assets_and_transactions.sql
# … 03 〜 19 まで同様に実行
```

### 4. アプリケーションの起動

```bash
./mvnw spring-boot:run
```

または

```bash
./mvnw clean package
java -jar target/fintrack-0.0.1-SNAPSHOT.jar
```

### 5. アクセスとログイン

ブラウザで http://localhost:8080 にアクセスしてください。

初回は http://localhost:8080/register からユーザーを登録し、登録したユーザー名・パスワードでログインします。初期シードユーザーはありません。

## ディレクトリ構成

```
src/main/java/com/example/fintrack/
├── config/              # 設定クラス（Security 等）
├── controller/          # コントローラー
├── csv/                 # CSV 処理（パーサー・正規化・フィンガープリント）
│   ├── dto/
│   └── parser/
├── domain/              # ドメインモデル・Enum
│   └── dto/             # 画面・クエリ用 DTO
├── repository/          # DB アクセス（RowMapper は各 Repository 内）
│   └── extractor/       # 複雑な ResultSetExtractor
└── service/             # サービス層

src/main/resources/
├── templates/           # Thymeleaf テンプレート
│   ├── assets/
│   ├── auto-category-rules/
│   ├── card-payments/
│   ├── categories/
│   ├── csv/
│   ├── discrepancy-transactions/
│   ├── split/
│   └── transactions/
├── static/
└── application.properties

sql/                     # DB マイグレーションスクリプト
```

## CSV 取込について

### サポートしている金融機関（`BankType`）

| 定数名 | 表示名 | 用途 |
|---|---|---|
| `RAKUTEN_BANK` | 楽天銀行 | 銀行口座 |
| `MIZUHO_BANK` | みずほ銀行 | 銀行口座（先頭 12 行はメタデータ） |
| `MUFG_BANK` | 三菱 UFJ 銀行 | 銀行口座 |
| `RAKUTEN_CARD` | 楽天カード | クレジットカード |
| `RAKUTEN_DEBIT` | 楽天デビットカード | デビットカード |

銀行・カードとも CSV 取込画面のドロップダウンから選択します（銀行用・カード用で同じ一覧が表示されます。カード取込では `RAKUTEN_CARD` / `RAKUTEN_DEBIT` を選択してください）。

### CSV フォーマット（参考）

各パーサーの Javadoc に詳細があります。概要のみ記載します。

#### 楽天銀行（Shift-JIS）

```
取引日,入出金(円),残高(円),入出金内容
20251201,-1625,23986,VISAデビット A0113457
```

※ 摘要に「VISAデビット」を含む行は銀行 CSV から除外されます（デビット CSV で取込）。

#### 楽天カード（UTF-8）

```
"利用日","利用店名・商品名","利用者","支払方法","利用金額",…
"2025/11/30","店舗名","本人","1回払い","2044",…
```

#### 楽天デビット（Shift-JIS）

```
"ご利用日","ご利用先","ご利用金額(円)",…
"20251201","店舗名","1625",…
```

#### みずほ銀行（Shift-JIS）

照会結果 CSV の 13 行目以降がデータ行です（アプリ側で先頭 12 行をスキップ）。

#### 三菱 UFJ 銀行（Shift-JIS）

```
"日付","摘要","摘要内容","支払い金額","預かり金額","差引残高",…
"2025/12/7","振込入金","…",,"192,800","302,982",…
```

### 新しい金融機関の追加方法

1. `BankType` enum に金融機関を追加
2. `CsvParser` を実装したパーサークラスを `csv/parser/` に作成
3. `CsvParserFactory` の `switch` にパーサーを登録

## データベース設計

### 主要テーブル（現行）

- `users`: ユーザー管理
- `bank_transactions` / `card_transactions`: 銀行・カード取引
- `assets` / `asset_categories` / `expense_categories`: 資産・カテゴリ
- `split_pattern_groups` / `split_patterns`: 分割パターン（グループと明細行）
- `split_transactions`: 分割明細（手動・パターン分割時）
- `auto_category_rules`: 自動カテゴリ付与ルール
- `card_payment_discrepancies`: カード引き落とし差異
- `discrepancy_transactions`: 差異解消用の調整取引
- `csv_import_histories` / `csv_import_update_histories`: CSV 取込・更新履歴

初期スキーマは `sql/01_create_tables.sql` を参照し、以降の変更は `sql/02` 〜 `sql/19` で適用されます。

## 使い方

### 基本的な流れ

1. **ユーザー登録** → ログイン
2. **カテゴリ設定**: 資産カテゴリ・支出カテゴリを登録
3. **CSV 取込**: 銀行・カードの明細をインポート
4. **分類・分割**: カテゴリ付与、収入・支出の分割
5. **資産確認**: カテゴリ別残高を確認
6. **カード照合**（任意）: 引き落としと明細の差異を確認

### 1. CSV 取込

1. ナビの **CSV → CSV 取込** を開く
2. 銀行またはカードのセクションで金融機関を選択（例: 楽天銀行 → `RAKUTEN_BANK`）
3. CSV ファイルをアップロード（必要に応じて「ヘッダー行あり」を ON）
4. **取込** をクリック

取込履歴は **CSV → CSV 登録履歴** から確認できます。

### 2. 自動カテゴリ付与

**自動分類ルール** メニューから、摘要パターンと対象カテゴリを登録します（`user_id` 単位で管理）。取込時にマッチした取引へ自動でカテゴリが付与されます。

### 3. 支出・収入のカテゴリ設定

- **支出一覧** / **収入一覧** から詳細画面で個別に設定
- **資産管理** 画面から、カテゴリ未設定の取引を一括設定

### 4. 収入・支出の分割

**収入一覧** または **支出一覧** の詳細画面から、次のいずれかで分配します。

- **全額を 1 つのカテゴリに分配**（`SINGLE`）
- **分割パターンで分配**（`PATTERN`）
- **手動で金額を指定**（`MANUAL`）

分割パターンは **分割管理 → 収入分割パターン** / **支出分割パターン** で作成・編集します（割合または金額指定）。

### 5. カード引き落とし照合

**カード管理** メニューから、月別明細や銀行引き落としとの差異を確認・解消できます。

### 画面構成

| 画面 | 説明 |
|---|---|
| ログイン / ユーザー登録 | 認証 |
| 資産管理 | 総資産・カテゴリ別残高・未分類取引 |
| 支出一覧 / 支出詳細 | 支出の一覧・カテゴリ・分割 |
| 収入一覧 / 収入詳細 | 収入の一覧・分割 |
| CSV 取込 / CSV 登録履歴 | 取込と履歴 |
| 分割管理（収入・支出） | 分割パターン |
| カテゴリ管理 | 資産・支出カテゴリ |
| 自動分類ルール | 自動カテゴリ |
| カード管理 | 月別明細・差異 |

## 注意事項

- 金額は円単位の整数（`BIGINT`）として扱います
- CSV の重複チェックは fingerprint（正規化した日付・金額・摘要等のハッシュ）で行います
- 同一 fingerprint が既に存在する場合、件数の差分のみが新規取込されます
- 分割状態は `split_method` 列で管理します（`NULL` = 未分配、`SINGLE` / `MANUAL` / `PATTERN`）。詳細は `AGENTS.md` を参照

## ライセンス

このプロジェクトはデモ用です。
