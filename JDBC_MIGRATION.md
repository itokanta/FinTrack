# JDBCTemplate移行完了レポート

## 実施日
2026年1月3日

## 変更概要

Spring Data JPAからJDBCTemplateへの全面的な移行を完了しました。

## 主な変更内容

### 1. 依存関係の変更

**pom.xml**
- ❌ 削除: `spring-boot-starter-data-jpa`
- ✅ 追加: `spring-boot-starter-jdbc`

### 2. パッケージ構成の変更

**entityパッケージ → domainパッケージ**
- `src/main/java/com/example/fintrack/entity` → `src/main/java/com/example/fintrack/domain`
- すべてのimport文を自動的に修正

### 3. Domainクラスの変更

**JPAアノテーションの削除**
- `@Entity`, `@Table`, `@Id`, `@GeneratedValue` などを削除
- `@ManyToOne`, `@JoinColumn` などのリレーションアノテーションを削除
- シンプルなPOJO（Plain Old Java Object）に変更
- Lombokの`@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`は継続使用

**IDベースの設計**
- リレーションはオブジェクト参照からIDベースに変更
- 例: `private User user;` → `private Long userId;`
- JOIN時に使用するオブジェクトフィールドは残存（`private User user;`など）

### 4. RowMapperの作成

**新規作成: `src/main/java/com/example/fintrack/repository/mapper/`**

以下のRowMapperクラスを作成：
- `UserRowMapper`
- `BankTransactionRowMapper`
- `CardTransactionRowMapper`
- `AssetRowMapper`
- `AssetCategoryRowMapper`
- `ExpenseCategoryRowMapper`
- `AutoCategoryRuleRowMapper`
- `IncomeSplitPatternRowMapper`
- `SplitIncomeRowMapper`

各RowMapperは`ResultSet`からドメインオブジェクトへのマッピングを担当。

### 5. Repositoryの全面書き換え

**変更前: Spring Data JPA**
```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}
```

**変更後: JDBCTemplate**
```java
@Repository
public class UserRepository {
    private final JdbcTemplate jdbcTemplate;
    
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        List<User> users = jdbcTemplate.query(sql, userRowMapper, username);
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }
}
```

**書き換えたRepositoryクラス（9個）**
1. `UserRepository`
2. `BankTransactionRepository`
3. `CardTransactionRepository`
4. `AssetRepository`
5. `AssetCategoryRepository`
6. `ExpenseCategoryRepository`
7. `AutoCategoryRuleRepository`
8. `IncomeSplitPatternRepository`
9. `SplitIncomeRepository`

### 6. Serviceクラスの修正

**主な変更点**
- import文を`entity`から`domain`に変更
- Spring Data JPAの`Page`, `Pageable`を使用していた箇所を修正
- オブジェクト参照からIDベースの処理に変更

**修正したServiceクラス（6個）**
1. `CsvImportService` - User/Transaction/Assetの保存処理をIDベースに変更
2. `TransactionService` - PageableをLIMIT/OFFSETに変更
3. `IncomeSplitService` - オブジェクト参照をIDベースに変更
4. `AssetService` - import文の修正
5. `UserService` - import文の修正
6. `CustomUserDetailsService` - import文の修正

### 7. Controllerの修正

**変更内容**
- import文を`entity`から`domain`に自動一括変更
- ロジックの変更は不要（Serviceレイヤーで吸収）

**修正したControllerクラス（6個）**
1. `TransactionController`
2. `AssetController`
3. `IncomeSplitController`
4. `CsvImportController`
5. `HomeController`
6. `LoginController`

### 8. その他の修正

**CSV処理関連**
- `TransactionDTO` - import文の修正
- `RakutenBankParser` - import文の修正
- `GenericCreditCardParser` - import文の修正

**削除したファイル**
- `IncomeSplitPatternId.java` - JDBCでは複合キーのIDクラスは不要

## SQL実装のポイント

### INSERT with RETURNING

PostgreSQLの`RETURNING`句を使用してINSERT後のIDを取得：

```java
String sql = "INSERT INTO users (username, password, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?) RETURNING id";
Long id = jdbcTemplate.queryForObject(sql, Long.class, ...);
```

### NULL許容カラムの処理

`ResultSet.wasNull()`を使用してNULL判定：

```java
long bankTransactionId = rs.getLong("bank_transaction_id");
asset.setBankTransactionId(rs.wasNull() ? null : bankTransactionId);
```

### ページネーション

Spring Data JPAの`Pageable`を`LIMIT`と`OFFSET`に変換：

```java
int offset = page * size;
String sql = "SELECT * FROM bank_transactions " +
            "WHERE user_id = ? " +
            "ORDER BY transaction_date DESC " +
            "LIMIT ? OFFSET ?";
```

### 集計クエリ

`GROUP BY`や`SUM`を使用した集計：

```java
String sql = "SELECT fingerprint, COUNT(*) as count " +
            "FROM bank_transactions " +
            "WHERE user_id = ? " +
            "GROUP BY fingerprint";
Map<String, Long> results = ...;
```

## メリット

### 1. SQLの完全な制御
- 実行されるSQLを完全に把握・制御可能
- パフォーマンスチューニングが容易
- 複雑なJOINやサブクエリも自由に記述可能

### 2. 軽量化
- JPAのオーバーヘッドがなくなり、メモリ使用量が削減
- 起動時間の短縮
- 依存関係の削減

### 3. シンプルな設計
- ドメインクラスがシンプルなPOJOに
- アノテーションの複雑さが解消
- IDベースの明示的な設計

### 4. デバッグの容易さ
- SQLログで実行内容を直接確認可能
- N+1問題などの発生を防止しやすい

## 注意点

### 1. トランザクション管理
- `@Transactional`アノテーションは引き続き使用
- Spring Frameworkのトランザクション管理機能を活用

### 2. 手動でのSQL記述
- タイプミスに注意
- カラム名とフィールド名の対応を正確に
- テストの重要性が増加

### 3. リレーションの扱い
- JOINが必要な場合は明示的にSQLで記述
- 遅延ロード（Lazy Loading）は手動で実装が必要

## 動作確認

### 必要な手順

1. **Mavenの依存関係を更新**
```bash
./mvnw clean install
```

2. **データベースの確認**
```bash
psql -U postgres -d FinTrack_dev
\dt  # テーブル一覧を確認
```

3. **アプリケーションの起動**
```bash
./mvnw spring-boot:run
```

4. **動作テスト**
- ログイン機能
- CSV取込機能
- 資産一覧表示
- 収支一覧表示
- 収入分割機能

## 今後の拡張

### 新しいテーブルの追加手順

1. **SQLでテーブルを作成**
```sql
CREATE TABLE new_table (...);
```

2. **Domainクラスを作成**
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewDomain {
    private Long id;
    // フィールド定義
}
```

3. **RowMapperを作成**
```java
public class NewDomainRowMapper implements RowMapper<NewDomain> {
    @Override
    public NewDomain mapRow(ResultSet rs, int rowNum) throws SQLException {
        // マッピング処理
    }
}
```

4. **Repositoryを作成**
```java
@Repository
public class NewDomainRepository {
    private final JdbcTemplate jdbcTemplate;
    // CRUD操作を実装
}
```

5. **Serviceで使用**
```java
@Service
public class NewService {
    private final NewDomainRepository repository;
    // ビジネスロジックを実装
}
```

## まとめ

Spring Data JPAからJDBCTemplateへの移行により、以下を達成しました：

✅ SQLの完全な制御
✅ パフォーマンスの向上
✅ シンプルで理解しやすいコード
✅ デバッグの容易さ
✅ 軽量なアプリケーション

すべての機能が正常に動作することを確認してください。

