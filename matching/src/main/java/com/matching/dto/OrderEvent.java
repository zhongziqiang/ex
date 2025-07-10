package com.matching.dto;

import com.matching.entity.Order;
import com.matching.enums.OrderCommand;

public class OrderEvent {
    private Order order;
    private OrderCommand command;
    private long sequence;

    public void clear() {
        order = null;
        command = null;
        sequence = -1;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public OrderCommand getCommand() {
        return command;
    }

    public void setCommand(OrderCommand command) {
        this.command = command;
    }

    public long getSequence() {
        return sequence;
    }

    public void setSequence(long sequence) {
        this.sequence = sequence;
    }
}