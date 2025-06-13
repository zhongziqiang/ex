package com.matching.factory;

import com.lmax.disruptor.EventFactory;
import com.matching.dto.OrderEvent;

public class OrderEventFactory implements EventFactory<OrderEvent> {
    @Override
    public OrderEvent newInstance() {
        return new OrderEvent();
    }
}
