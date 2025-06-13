package com.matching.handler;

import com.lmax.disruptor.EventHandler;
import com.matching.dto.OrderBook;
import com.matching.dto.OrderEvent;

public class MatchingEventHandler implements EventHandler<OrderEvent> {
    private OrderBook orderBook = new OrderBook();

    @Override
    public void onEvent(OrderEvent event, long sequence, boolean endOfBatch) {
        if (event.isBuy()) {
            orderBook.matchBuyOrder(event);
        } else {
            orderBook.matchSellOrder(event);
        }
    }
}
