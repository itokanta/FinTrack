package com.example.fintrack.domain.dto;

import com.example.fintrack.domain.CardTransaction;
import com.example.fintrack.domain.CsvImportUpdateHistory;
import java.util.List;
import lombok.Data;

@Data
public class CardTransactionUpdateDetail {
    private CardTransaction cardTransaction;
    private List<CsvImportUpdateHistory> updates;
}
