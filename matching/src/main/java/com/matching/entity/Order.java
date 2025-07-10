package com.matching.entity;

import com.matching.enums.OrderSide;
import com.matching.enums.OrderStatus;
import com.matching.enums.OrderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order implements Serializable {
    private static final AtomicLong ID_GENERATOR = new AtomicLong(0);

    private Long orderId;
    private String symbol;
    private OrderType orderType;
    private OrderSide orderSide;
    private BigDecimal price;
    private BigDecimal quantity;
    private BigDecimal executedQuantity;
    private OrderStatus orderStatus;
    private long timestamp;
    private String clientOrderId;

    public Order(String symbol, OrderType orderType, OrderSide orderSide,
                 BigDecimal price, BigDecimal quantity, String clientOrderId) {
        this.orderId = ID_GENERATOR.incrementAndGet();
        this.symbol = symbol;
        this.orderType = orderType;
        this.orderSide = orderSide;
        this.price = price;
        this.quantity = quantity;
        this.executedQuantity = BigDecimal.ZERO;
        this.orderStatus = OrderStatus.NEW;
        this.timestamp = System.currentTimeMillis();
        this.clientOrderId = clientOrderId;
    }

    public boolean isFullyExecuted() {
        return quantity.compareTo(executedQuantity) == 0;
    }

    public boolean canBeMatched() {
        return orderStatus == OrderStatus.NEW || orderStatus == OrderStatus.PARTIALLY_FILLED;
    }

    public void execute(BigDecimal executedQty) {
        this.executedQuantity = this.executedQuantity.add(executedQty);
        this.orderStatus = this.isFullyExecuted() ? OrderStatus.FILLED : OrderStatus.PARTIALLY_FILLED;
    }
}