#!/usr/bin/env bash
# コマンドの失敗、未定義の変数、パイプ途中の失敗で、スクリプトを止める
set -euo pipefail

# 第1引数を、ダウンロードする jar の S3 の URI として受け取る。空ならメッセージを出して終了する
URI="${1:?s3 uri required}"

# 実行ログの出力先
LOG=/var/log/fintrack/deploy.log
# これ以降の標準出力と標準エラーを、画面（SSM が回収する側）とログファイルの両方へ流す
exec > >(tee -a "${LOG}") 2>&1
# 開始の時刻と対象の jar をログに残す
echo "===== $(date --iso-8601=seconds) start ${URI} ====="

# アプリのディレクトリ
APP_DIR=/opt/fintrack
# 本番で動かす jar のパス
JAR="${APP_DIR}/fintrack.jar"
# 差し替え前の jar を残すパス
PREV="${APP_DIR}/fintrack.jar.prev"
# ダウンロード先の一時ファイル
TMP=/tmp/fintrack.jar.new

DB_APP_VALUE="$(grep -E '^DB_APP=' /etc/fintrack/fintrack.env | cut -d= -f2- || true)"
: "${DB_APP_VALUE:?DB_APP is not set in /etc/fintrack/fintrack.env}"
DB_NAME="${DB_APP_VALUE##*/}"
DB_NAME="${DB_NAME%%\?*}"
# バックアップの置き場（root だけが読める）
BACKUP_DIR=/var/backups/fintrack-deploy
# このデプロイのバックアップのファイル名（時刻付き）
BACKUP="${BACKUP_DIR}/$(date +%Y%m%d-%H%M%S).dump"

# DB の復元に失敗したときだけ 1 にして、アプリを止めたままにする
KEEP_STOPPED=0

# 終了時に呼ばれる処理。どこで失敗して終わっても、アプリを止めたままにしない
cleanup() {
  # 止めたままにする指定がなく、サービスが動いていなければ、起動を試みる
  if [[ "${KEEP_STOPPED}" -eq 0 ]] && ! systemctl is-active --quiet fintrack; then
    systemctl start fintrack || true
  fi
}
# スクリプトが終わるとき（成功でも失敗でも）、上の cleanup を呼ぶ
trap cleanup EXIT

# 失敗時の戻し処理。定義しただけでは実行しない
rollback() {
  # 戻し処理の開始をログに残す
  echo "===== $(date --iso-8601=seconds) rollback ====="
  # アプリを止める（すでに止まっていても失敗にしない）
  systemctl stop fintrack || true
  # バックアップを標準入力で渡して、DB を復元する
  # --single-transaction は、途中で失敗したら復元前の状態のままにする
  # --clean --if-exists は、既存のオブジェクトを消してから作り直す
  # postgres ユーザーが root 専用のディレクトリを読めないので、ファイルは root 側から渡す
  if ! runuser -u postgres -- \
      pg_restore --single-transaction --clean --if-exists -d "${DB_NAME}" < "${BACKUP}"; then
    # 復元に失敗したら、中途半端な状態でアプリを起動せず、手作業での復旧を求めて終わる
    echo "!!! database restore failed; manual recovery required (backup: ${BACKUP})" >&2
    KEEP_STOPPED=1
    exit 2
  fi
  # 旧 jar の控えがあれば、本番のパスへ戻す
  if [[ -f "${PREV}" ]]; then
    # コピーと同時に、所有者 fintrack、グループ fintrack、権限 644 を付ける
    install -o fintrack -g fintrack -m 644 "${PREV}" "${JAR}"
  fi
  # 戻した状態で起動する
  systemctl start fintrack || true
}

# 1. 新しい jar を S3 から /tmp へ取る。権限は EC2 のインスタンスロールが持つ
aws s3 cp "${URI}" "${TMP}"

# 2. アプリを止めてからバックアップする。止めることで、バックアップ後に書かれるデータをなくす
systemctl stop fintrack
# PostgreSQL のカスタム形式でダンプし、ファイルへ書く
if ! runuser -u postgres -- pg_dump -Fc "${DB_NAME}" > "${BACKUP}"; then
  # バックアップに失敗したら、今の版のまま終わる。アプリは cleanup が起動し直す
  echo "backup failed; keeping the current version" >&2
  # 中途半端なファイルを消す
  rm -f "${BACKUP}"
  exit 1
fi
# 取れたバックアップのパスをログに残す
echo "backup: ${BACKUP}"

# 3. 今の jar があれば、属性を保ったまま控えへコピーする
if [[ -f "${JAR}" ]]; then
  cp -a "${JAR}" "${PREV}"
fi
# 新しい jar を本番のパスへ置き、所有者と権限を付ける
install -o fintrack -g fintrack -m 644 "${TMP}" "${JAR}"
# 一時ファイルを消す
rm -f "${TMP}"

# 4. 起動する。このときアプリが Flyway を実行し、未適用の V21 以降を DB へ適用する
if ! systemctl start fintrack; then
  # 起動の指示そのものが失敗したら、DB と jar を戻して失敗で終わる
  echo "===== $(date --iso-8601=seconds) start failed ====="
  rollback
  exit 1
fi

# 5. 起動確認。2秒おきに最大60回（約120秒）、ログイン画面へ接続する
for _ in $(seq 1 60); do
  # -f は HTTP のエラーを失敗にし、-sS は失敗時のエラーだけ表示し、-o は本文を捨てる
  if curl -fsS -o /dev/null "http://127.0.0.1:8080/login"; then
    # 応答が取れたら成功
    echo "===== $(date --iso-8601=seconds) success ====="
    # 古いバックアップを消す（新しい順に10個を残す）。失敗してもデプロイの成功は変えない
    ls -1t "${BACKUP_DIR}"/*.dump | tail -n +11 | xargs -r rm -f -- || true
    exit 0
  fi
  # 応答が無ければ2秒待つ
  sleep 2
done

# 約120秒待っても応答が無ければ、DB と jar を戻して失敗で終わる
echo "===== $(date --iso-8601=seconds) health check failed ====="
rollback
exit 1