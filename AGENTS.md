# AGENTS.md — Fintrack プロジェクト共通ルール

## 対話ルール

**コードの編集は明示的な指示があった場合のみ行う。**
- 質問・相談・レビュー依頼に対しては、チャット上での提案・説明に留める。
- 「編集して」「修正して」「作成して」「実装して」「追加して」「削除して」などの指示があった場合のみ、実際にファイルを編集する。

---

## プロジェクト概要

**Fintrack** は個人向け家計管理 Web アプリケーション。銀行・クレジットカードの CSV を取り込み、収支の分類・資産管理・カード引き落とし照合を行う。

| 項目 | 内容 |
|---|---|
| 言語 | Java 21 |
| フレームワーク | Spring Boot 3.5.9 |
| View | Thymeleaf（サーバーサイドレンダリング） |
| セキュリティ | Spring Security 6（フォームログイン / BCrypt） |
| DB | PostgreSQL（`FinTrack_dev`） |
| データアクセス | `NamedParameterJdbcTemplate`（JPA/ORM は使用しない） |
| CSV | Apache Commons CSV（楽天銀行・楽天カード・楽天デビット・みずほ銀行・三菱UFJ銀行） |

### 主なドメインモデル

| クラス | 役割 |
|---|---|
| `User` | 認証ユーザー |
| `BankTransaction` | 銀行口座の取引（CSV インポート） |
| `CardTransaction` | クレジット / デビットカードの取引（CSV インポート） |
| `Asset` | 資産カテゴリごとの残高 |
| `AssetCategory` / `ExpenseCategory` | ユーザー定義カテゴリ |
| `SplitPatternGroup` / `SplitPattern` | 分割パターンのグループと明細行 |
| `SplitTransaction` | 手動・パターン分割の明細 |
| `AutoCategoryRule` | インポート時の自動カテゴリ割当ルール |
| `CardPaymentDiscrepancy` | カード明細と銀行引き落とし金額の差異 |
| `DiscrepancyTransaction` | 差異解消用の調整取引 |
| `CsvImportHistory` / `CsvImportUpdateHistory` | CSV インポート・更新履歴 |

### パッケージ構成

```
com.example.fintrack/
├── controller/   # HTTP リクエスト受付・レスポンス返却
├── service/      # ビジネスロジック
├── repository/   # DB アクセス（SQL・RowMapper は各 Repository 内に手書き）
│   └── extractor/  # 複雑な ResultSetExtractor（任意）
├── domain/       # ドメインモデル・Enum（dto/ サブパッケージあり）
├── csv/          # CSV パーサー・正規化・フィンガープリント
└── config/       # Spring Security 設定
```

---

## アーキテクチャルール

### レイヤー責務

| レイヤー | 責務 | やってはいけないこと |
|---|---|---|
| **Controller** | リクエスト受け取り・レスポンス（View）返却のみ | ビジネスロジックを持つこと |
| **Service** | ビジネスロジックの集約 | — |
| **Repository** | データアクセス（クエリ・CRUD）のみ | ビジネスロジックを持つこと |

### 呼び出し規則

```
Controller → Service → Repository
```

以下の呼び出しパターンは禁止：

- Controller から Repository を直接呼び出す
- Service から別の Service を呼び出す
- Controller から別の Controller を呼び出す
- Repository から別の Repository を呼び出す

---

## 実装上の注意点

- **JPA は使用しない。** DB アクセスは必ず `NamedParameterJdbcTemplate` と**手書きの `RowMapper`**で実装する。エンティティ行のマッピングは、原則として各 `*Repository` 内の `private static final RowMapper<...> ROW_MAPPER = (rs, rowNum) -> { ... }`（または同等の手書き `RowMapper`）に置く。`repository/mapper` にクラスを切り出す構成は必須ではない（複雑な `ResultSetExtractor` などは `repository/extractor/` 等でよい）。
- **フィンガープリント方式で重複排除。** CSV を再インポートしても既存レコードは重複しない。
- **全データはユーザー単位で分離。** クエリには必ず `userId` でのスコープを含める。
- **分割状態**（`sql/16` 以降。`split_pattern_id` の番兵値 `0` / `-1` は廃止）:
  - `split_method` が `NULL` かつ `split_pattern_id` が `NULL` … 未分配
  - `split_method = 'SINGLE'` … 単一カテゴリ（`asset_category_id` に全額。`split_transactions` 行は作らない）
  - `split_method = 'MANUAL'` … 手動分割（明細は `split_transactions`）
  - `split_method = 'PATTERN'` … パターン分割（`split_pattern_id` が `split_patterns.id` を参照）
