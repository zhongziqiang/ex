package com.matching.dto;

public class OrderEvent {
    private long orderId;
    private boolean isBuy; // true=买单, false=卖单
    private double price;
    private int quantity;

    public void set(long orderId, boolean isBuy, double price, int quantity) {
        this.orderId = orderId;
        this.isBuy = isBuy;
        this.price = price;
        this.quantity = quantity;
    }

    public long getOrderId() { return orderId; }
    public boolean isBuy() { return isBuy; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
}
