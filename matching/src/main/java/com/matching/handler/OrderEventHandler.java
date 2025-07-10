package com.matching.handler;

import com.lmax.disruptor.EventHandler;
import com.matching.dto.OrderEvent;
import com.matching.entity.Order;
import com.matching.enums.OrderCommand;
import com.matching.mc.MatchingEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrderEventHandler implements EventHandler<OrderEvent> {
    private static final Logger logger = LoggerFactory.getLogger(OrderEventHandler.class);
    private final MatchingEngine matchingEngine;

    public OrderEventHandler(MatchingEngine matchingEngine) {
        this.matchingEngine = matchingEngine;
    }

    @Override
    public void onEvent(OrderEvent event, long sequence, boolean endOfBatch) throws Exception {
        try {
            Order order = event.getOrder();
            OrderCommand command = event.getCommand();

            if (order == null || command == null) {
                return;
            }

            switch (command) {
                case NEW_ORDER:
                    matchingEngine.processOrder(order);
                    break;
                case CANCEL_ORDER:
                    matchingEngine.cancelOrder(order.getSymbol(), order.getOrderId());
                    break;
                case MODIFY_ORDER:
                    matchingEngine.modifyOrder(
                            order.getSymbol(),
                            order.getOrderId(),
                            order.getPrice(),
                            order.getQuantity()
                    );
                    break;
                default:
                    logger.warn("Unknown command: {}", command);
            }
        } finally {
            event.clear();
        }
    }
}