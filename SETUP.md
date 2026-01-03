# FinTrack セットアップガイド

このガイドでは、FinTrackを初めて起動するまでの手順を説明します。

## 前提条件

以下のソフトウェアがインストールされている必要があります：

- **Java 21** 以上
- **PostgreSQL** 12 以上
- **Maven** 3.6 以上（または同梱の `mvnw` を使用）

## セットアップ手順

### 1. PostgreSQLのインストールと起動

#### macOS (Homebrew)
```bash
brew install postgresql@14
brew services start postgresql@14
```

#### Ubuntu/Debian
```bash
sudo apt update
sudo apt install postgresql postgresql-contrib
sudo systemctl start postgresql
```

#### Windows
PostgreSQLの公式サイトからインストーラーをダウンロードしてインストールしてください。

### 2. データベースの作成

```bash
# PostgreSQLに接続（デフォルトユーザーで接続）
psql -U postgres

# データベースを作成
CREATE DATABASE "FinTrack_dev";

# 接続を終了
\q
```

### 3. テーブルの作成と初期データの投入

```bash
# プロジェクトのルートディレクトリに移動
cd /path/to/fintrack

# テーブル作成
psql -U postgres -d FinTrack_dev -f sql/01_create_tables.sql

# 初期データ投入
psql -U postgres -d FinTrack_dev -f sql/02_insert_initial_data.sql
```

### 4. データベース接続情報の設定

`src/main/resources/application.properties` を開き、必要に応じて接続情報を変更してください：

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/FinTrack_dev
spring.datasource.username=postgres
spring.datasource.password=postgres
```

**注意**: パスワードを設定している場合は、`spring.datasource.password` を変更してください。

### 5. アプリケーションのビルドと起動

#### 方法1: Mavenラッパーを使用（推奨）

```bash
# Mavenラッパーに実行権限を付与（初回のみ、Unix系のみ）
chmod +x mvnw

# アプリケーションを起動
./mvnw spring-boot:run
```

#### 方法2: Mavenを使用

```bash
mvn spring-boot:run
```

#### 方法3: JARファイルを作成して起動

```bash
# ビルド
./mvnw clean package

# 起動
java -jar target/fintrack-0.0.1-SNAPSHOT.jar
```

### 6. アプリケーションにアクセス

ブラウザで以下のURLにアクセスしてください：

```
http://localhost:8080
```

### 7. ログイン

デフォルトのログイン情報：

- **ユーザー名**: `testuser`
- **パスワード**: `password`

## 初期データについて

`sql/02_insert_initial_data.sql` で以下のデータが投入されます：

### ユーザー
- ユーザー名: `testuser`
- パスワード: `password`（BCryptでハッシュ化済み）

### 資産カテゴリ
- 生活費
- 貯金
- 投資
- 予備費

### 支出カテゴリ
- 食費
- 交通費
- 娯楽
- 日用品
- 光熱費
- 通信費
- 医療費
- その他

### 自動カテゴリ付与ルール
- 「スーパー」→ 生活費
- 「コンビニ」→ 生活費
- 「給与」→ 貯金
- 「配当」→ 投資

## サンプルデータの取込

サンプルCSVファイルを使ってテストできます：

1. アプリケーションにログイン
2. サイドバーの「CSV取込」をクリック
3. 以下のファイルを取り込む：
   - `sample_data/rakuten_bank_sample.csv` → 銀行取引（RAKUTEN、ヘッダーあり）
   - `sample_data/credit_card_sample.csv` → カード取引（CREDIT_CARD_GENERIC、ヘッダーなし）

## トラブルシューティング

### データベース接続エラー

```
org.postgresql.util.PSQLException: FATAL: database "FinTrack_dev" does not exist
```

**解決方法**: データベースが作成されていません。手順2を実行してください。

### ポート8080が使用中

```
Web server failed to start. Port 8080 was already in use.
```

**解決方法1**: 他のアプリケーションを停止する

**解決方法2**: ポート番号を変更する

`application.properties` に以下を追加：
```properties
server.port=8081
```

### Java バージョンエラー

```
Unsupported class file major version
```

**解決方法**: Java 21以上をインストールしてください。

```bash
# バージョン確認
java -version

# Java 21以上が表示されればOK
```

### Maven ビルドエラー

```bash
# 依存関係を再取得
./mvnw clean install -U
```

## 次のステップ

セットアップが完了したら、以下のドキュメントを参照してください：

- **使い方ガイド**: `USAGE.md`
- **README**: `README.md`

## 開発環境のセットアップ

### IDE（IntelliJ IDEA / Eclipse）

1. プロジェクトをIDEで開く
2. Maven プロジェクトとして認識させる
3. Lombok プラグインをインストール（IntelliJ IDEAの場合）
4. `FintrackApplication.java` を実行

### ホットリロード

Spring Boot DevToolsが含まれているため、コード変更時に自動的に再起動されます。

### データベースツール

以下のツールを使うとデータベースの確認が便利です：

- **pgAdmin**: PostgreSQLの公式GUIツール
- **DBeaver**: マルチプラットフォームのデータベースツール
- **psql**: コマンドラインツール

```bash
# psqlでデータベースに接続
psql -U postgres -d FinTrack_dev

# テーブル一覧を表示
\dt

# ユーザーを確認
SELECT * FROM users;

# 取引を確認
SELECT * FROM bank_transactions;
```

## 本番環境へのデプロイ

本番環境にデプロイする場合は、以下の点に注意してください：

1. **application.properties の設定**
   - データベース接続情報を本番環境用に変更
   - `spring.jpa.show-sql=false` に設定
   - `logging.level` を適切に設定

2. **セキュリティ**
   - デフォルトユーザーのパスワードを変更
   - HTTPS を有効化
   - CSRF保護を有効化（デフォルトで有効）

3. **パフォーマンス**
   - データベース接続プールの設定
   - インデックスの最適化

4. **バックアップ**
   - 定期的なデータベースバックアップ
   - トランザクションログの保存

## サポート

問題が発生した場合は、以下を確認してください：

1. ログファイル（コンソール出力）
2. データベースの状態
3. アプリケーションの設定ファイル

詳細なログを確認する場合は、`application.properties` で以下を設定：

```properties
logging.level.com.example.fintrack=DEBUG
```

