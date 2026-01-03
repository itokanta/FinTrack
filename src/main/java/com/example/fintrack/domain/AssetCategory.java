package com.example.fintrack.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssetCategory {
    
    private Long id;
    private Long userId;
    private String categoryName;
    private LocalDateTime createdAt;
}
