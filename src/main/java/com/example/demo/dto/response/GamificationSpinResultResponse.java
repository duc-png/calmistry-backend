package com.example.demo.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GamificationSpinResultResponse {
    List<String> symbols;
    Boolean jackpot;
    Integer remainingSpins;
    @Builder.Default
    String voucherCode = null;
    @Builder.Default
    String voucherTitle = null;
}

