package com.example.fintrack.domain;

/**
 * CSV取込の更新履歴で記録する対象フィールド。
 * csv_import_update_histories.target_field と対応する。
 */
public enum CsvImportUpdateField {

    /**
     * 支払い月（CardTransaction.payment_month）
     */
    PAYMENT_MONTH,

    /**
     * 引き落とし済みフラグ（CardTransaction.is_paid）
     */
    IS_PAID;

}
