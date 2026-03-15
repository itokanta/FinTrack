package com.example.fintrack.domain;

/**
 * CSV取込の更新履歴で記録する対象フィールド。
 * csv_import_update_histories.target_field と対応する。
 */
public enum CsvImportUpdateField {

    /**
     * 支払い月（CardTransaction.payment_month）
     */
    PAYMENT_MONTH("payment_month"),

    /**
     * 引き落とし済みフラグ（CardTransaction.is_paid）
     */
    IS_PAID("is_paid");

    private final String dbValue;

    CsvImportUpdateField(String dbValue) {
        this.dbValue = dbValue;
    }

    /**
     * DBの target_field カラムに保存する文字列を返す。
     */
    public String getDbValue() {
        return dbValue;
    }

    /**
     * DBから取得した target_field の文字列から enum に変換する。
     *
     * @param dbValue DBの値（null の場合は null を返す）
     * @return 対応する enum、未定義の値の場合は IllegalArgumentException
     */
    public static CsvImportUpdateField fromDbValue(String dbValue) {
        if (dbValue == null) {
            return null;
        }
        for (CsvImportUpdateField f : values()) {
            if (f.dbValue.equals(dbValue)) {
                return f;
            }
        }
        throw new IllegalArgumentException("Unknown target_field: " + dbValue);
    }
}
