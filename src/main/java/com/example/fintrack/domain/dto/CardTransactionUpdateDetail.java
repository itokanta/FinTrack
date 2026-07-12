package com.example.fintrack.domain.dto;

import com.example.fintrack.domain.CardTransaction;
import com.example.fintrack.domain.CsvImportUpdateHistory;
import java.util.List;
import lombok.Data;

/**
 * カード取引と、その CSV 更新履歴をまとめて返す DTO。
 *
 * @author kanta ito
 */
@Data
public class CardTransactionUpdateDetail {
    private CardTransaction cardTransaction;
    private List<CsvImportUpdateHistory> updates;
}
