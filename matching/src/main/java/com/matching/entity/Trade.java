package com.matching.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Trade implements Serializable {
    private String tradeId;
    private String symbol;
    private Long buyOrderId;
    private Long sellOrderId;
    private BigDecimal price;
    private BigDecimal quantity;
    private long timestamp;
}
