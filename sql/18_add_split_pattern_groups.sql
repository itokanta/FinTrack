-- ============================================================
-- split_pattern_groups 追加と split_patterns の正規化
--   - パターン名をグループテーブルへ移動
--   - split_patterns.pattern_id → split_pattern_groups.id の FK
--
-- 前提: sql/16_add_split_type_and_soft_delete_pattern.sql 適用済み
--       （split_patterns.deleted_at が存在すること）
-- ============================================================

-- ステップ 1: グループテーブル作成
CREATE TABLE split_pattern_groups (
  id SERIAL PRIMARY KEY,
  pattern_name TEXT NOT NULL,
  user_id INTEGER NOT NULL,
  transaction_type TEXT NOT NULL,
  split_type TEXT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP DEFAULT NULL,
  CONSTRAINT split_pattern_groups_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT chk_split_pattern_groups_transaction_type
    CHECK (transaction_type IN ('INCOME', 'EXPENSE')),
  CONSTRAINT chk_split_pattern_groups_split_type
    CHECK (split_type IN ('RATIO', 'AMOUNT'))
);

CREATE INDEX idx_split_pattern_groups_user_id
    ON split_pattern_groups(user_id);
CREATE INDEX idx_split_pattern_groups_user_transaction_type
    ON split_pattern_groups(user_id, transaction_type);

-- ステップ 2: 既存データを移行（pattern_id を groups.id として流用）
INSERT INTO split_pattern_groups (
    id, pattern_name, user_id, transaction_type, split_type, deleted_at, created_at
)
SELECT
    sp.pattern_id,
    MAX(sp.pattern_name) AS pattern_name,
    sp.user_id,
    sp.transaction_type,
    sp.split_type,
    MAX(sp.deleted_at) AS deleted_at,
    MIN(sp.created_at) AS created_at
FROM split_patterns sp
GROUP BY sp.pattern_id, sp.user_id, sp.transaction_type, sp.split_type;

-- SERIAL の採番を現データの最大 id に合わせる
SELECT setval(
    pg_get_serial_sequence('split_pattern_groups', 'id'),
    COALESCE((SELECT MAX(id) FROM split_pattern_groups), 1)
);

-- ステップ 3: 参照整合性の確認（FK 追加前に必ず実行）
-- SELECT sp.pattern_id
-- FROM split_patterns sp
-- LEFT JOIN split_pattern_groups g ON g.id = sp.pattern_id
-- WHERE g.id IS NULL;

-- ステップ 4: FK 制約を追加
ALTER TABLE split_patterns
ADD CONSTRAINT fk_split_patterns_pattern_id
    FOREIGN KEY (pattern_id) REFERENCES split_pattern_groups(id);

-- ステップ 5: split_patterns 側の CHECK 制約を削除（split_type 等を DROP するため）
ALTER TABLE split_patterns DROP CONSTRAINT IF EXISTS chk_split_patterns_transaction_type;
ALTER TABLE split_patterns DROP CONSTRAINT IF EXISTS chk_split_patterns_split_type;
ALTER TABLE split_patterns DROP CONSTRAINT IF EXISTS chk_split_patterns_split_amount;

-- ステップ 6: グループへ移した重複カラムを削除
ALTER TABLE split_patterns
    DROP COLUMN pattern_name,
    DROP COLUMN transaction_type,
    DROP COLUMN split_type,
    DROP COLUMN deleted_at;
