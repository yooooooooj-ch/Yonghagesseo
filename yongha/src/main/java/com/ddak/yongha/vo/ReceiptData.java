// src/main/java/com/example/ocr/dto/ReceiptData.java
package com.ddak.yongha.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ReceiptData {
    private BigDecimal totalAmount;      // 결제액
    private LocalDateTime  paidAt;        // 결제일시
    private String rawText;              // 원문 텍스트 (디버그용)
}
