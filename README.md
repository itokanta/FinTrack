# FinTrack - 家計簿アプリケーション

Spring Bootで構築された家計簿管理アプリケーションです。

## 機能

- **ユーザー認証**: Spring Securityによるログイン機能
- **CSV取込**: 銀行・カード会社の取引明細CSVをインポート
- **資産管理**: カテゴリ別の資産残高を管理
- **収支管理**: 収入・支出の一覧表示と詳細確認
- **収入分割**: 収入を複数の資産カテゴリに分配
- **自動カテゴリ付与**: 摘要に基づく自動カテゴリ設定
- **重複排除**: fingerprintによる取引の重複チェック

## 技術スタック

- **バックエンド**: Spring Boot 3.5.9, Java 21
- **データベース**: PostgreSQL
- **ORM**: Spring Data JPA
- **セキュリティ**: Spring Security
- **テンプレートエンジン**: Thymeleaf
- **CSV処理**: Apache Commons CSV
- **ビルドツール**: Maven

## セットアップ

### 1. データベースの準備

PostgreSQLで開発用データベースを作成してください：

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

### 3. テーブルの作成

`sql/` ディレクトリ内のSQLファイルを順番に実行してください：

```bash
psql -U your_username -d FinTrack_dev -f sql/01_create_tables.sql
psql -U your_username -d FinTrack_dev -f sql/02_insert_initial_data.sql
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

### 5. アクセス

ブラウザで http://localhost:8080 にアクセスしてください。

デフォルトのログイン情報：
- ユーザー名: `testuser`
- パスワード: `password`

## ディレクトリ構成

```
src/main/java/com/example/fintrack/
├── config/              # 設定クラス（Security等）
├── controller/          # コントローラー
├── csv/                 # CSV処理関連
│   ├── dto/            # DTO
│   └── parser/         # 金融機関別パーサー
├── entity/             # エンティティ
├── repository/         # リポジトリ
└── service/            # サービス層

src/main/resources/
├── templates/          # Thymeleafテンプレート
│   ├── assets/        # 資産関連画面
│   ├── csv/           # CSV取込画面
│   ├── income-split/  # 収入分割画面
│   └── transactions/  # 取引一覧・詳細画面
└── application.properties

sql/                    # SQLスクリプト
```

## CSV取込について

### サポートしている金融機関

- 楽天銀行 (RAKUTEN)
- 汎用クレジットカード (CREDIT_CARD_GENERIC)

### CSVフォーマット

#### 楽天銀行
```
取引日,入出金(円),残高(円),摘要
2024/01/01,10000,100000,給与
```

#### 汎用クレジットカード
```
日付,摘要,金額,区分
2024/01/01,スーパー,3000,支出
```

### 新しい金融機関の追加方法

1. `BankType` enumに金融機関を追加
2. `CsvParser` インターフェースを実装した新しいパーサークラスを作成
3. `CsvParserFactory` に新しいパーサーを登録

## データベース設計

### 主要テーブル

- `users`: ユーザー管理
- `bank_transactions`: 銀行取引
- `card_transactions`: カード取引
- `assets`: 資産管理
- `asset_categories`: 資産カテゴリ
- `expense_categories`: 支出カテゴリ
- `auto_category_rules`: 自動カテゴリ付与ルール
- `income_split_patterns`: 収入分割パターン
- `split_incomes`: 分割済み収入

詳細は `sql/01_create_tables.sql` を参照してください。

## 使い方

### 基本的な流れ

1. **CSV取込**: 銀行・カードの取引明細をインポート
2. **カテゴリ設定**: 資産カテゴリ・支出カテゴリを設定
3. **収入分割**: 収入を各資産カテゴリに分配
4. **資産確認**: カテゴリ別の残高を確認

### 1. CSV取込

#### 銀行取引の取込
1. 「CSV取込」メニューから取込画面を開く
2. 「銀行取引CSV取込」セクションで金融機関を選択（例：RAKUTEN）
3. CSVファイルをアップロード
4. ヘッダー行がある場合はチェックボックスをON
5. 「取込」ボタンをクリック

#### カード取引の取込
1. 「CSV取込」メニューから取込画面を開く
2. 「カード取引CSV取込」セクションでカード会社を選択（例：CREDIT_CARD_GENERIC）
3. CSVファイルをアップロード
4. ヘッダー行がある場合はチェックボックスをON
5. 「取込」ボタンをクリック

**サンプルCSVファイル**: `sample_data/` ディレクトリにサンプルファイルがあります

### 2. 自動カテゴリ付与の設定

取込時に自動的にカテゴリを付与するには、`auto_category_rules` テーブルにルールを登録します：

```sql
-- 例：「給与」を含む取引を「生活費」カテゴリに自動分類
INSERT INTO auto_category_rules (asset_category_id, description_pattern) 
VALUES (1, '給与');
```

### 3. 支出のカテゴリ設定

#### 個別に設定
1. 「支出一覧」から支出を選択
2. 詳細画面で「資産カテゴリ」「支出カテゴリ」を選択
3. 「更新」ボタンをクリック

#### 一括設定
1. 「資産」メニューを開く
2. カテゴリ未設定の項目がある場合、「カテゴリを設定する」をクリック
3. 設定したい項目にチェックを入れる
4. カテゴリを選択して「設定」をクリック

### 4. 収入の分割

#### 全額を1つのカテゴリに分配
1. 「収入一覧」から収入を選択
2. 詳細画面の「全額を1つのカテゴリに分配」セクションでカテゴリを選択
3. 「分配する」ボタンをクリック

#### 分割パターンで分配
1. 「収入一覧」から収入を選択
2. 詳細画面の「分割パターンで分配」セクションでパターンを選択
3. 「分配する」ボタンをクリック

**分割済みの収入**: 詳細画面で分配内容が表示されます

### 5. 分割パターンの作成

1. 「収入分割管理」メニューを開く
2. 「新規パターン作成」をクリック
3. パターン名を入力（例：「給与分配パターン」）
4. 各資産カテゴリの割合を入力（合計100%になるように）
   - 例：生活費 60%、貯金 30%、投資 10%
5. 「登録」をクリック

### 6. 資産の確認

1. 「資産」メニューを開く
2. 総資産とカテゴリ別残高が表示されます
3. カテゴリ未設定の項目がある場合は警告が表示されます

### 画面構成

- **ログイン**: ユーザー認証
- **資産**: 総資産とカテゴリ別残高の表示
- **支出一覧**: 月別の支出一覧（銀行+カード）
- **支出詳細**: 支出の詳細とカテゴリ変更
- **収入一覧**: 月別の収入一覧（銀行+カード）
- **収入詳細**: 収入の詳細と分配設定
- **CSV取込**: 銀行・カードのCSV取込
- **収入分割管理**: 分割パターンの管理

## 注意事項

- 金額は円単位で整数値として扱われます
- CSVの重複チェックはfingerprint（日付+金額+摘要+入出金区分のハッシュ値）で行われます
- 同じfingerprintの取引が複数ある場合、件数の差分のみがインポートされます

## ライセンス

このプロジェクトはデモ用です。

