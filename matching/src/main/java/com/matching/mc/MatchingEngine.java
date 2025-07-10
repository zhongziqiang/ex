package com.matching.mc;

import com.matching.entity.Order;
import com.matching.entity.Trade;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
public class MatchingEngine {
    private static final Logger logger = LoggerFactory.getLogger(MatchingEngine.class);
    private final Map<String, OrderBook> orderBooks = new ConcurrentHashMap<>();
    private final Consumer<Trade> tradeConsumer;

    public MatchingEngine(Consumer<Trade> tradeConsumer) {
        this.tradeConsumer = tradeConsumer;
    }

    public void processOrder(Order order) {
        OrderBook orderBook = orderBooks.computeIfAbsent(
                order.getSymbol(),
                symbol -> new OrderBook(symbol, tradeConsumer)
        );

        log.info("开始处理订单: {}", order);

        List<Trade> trades = orderBook.matchOrders(order);
        log.info("订单处理完成，产生 {} 笔成交", trades.size());

        if (!trades.isEmpty()) {
            logger.info("Processed order {}: {} trades generated", order.getOrderId(), trades.size());
        }
    }

    public boolean cancelOrder(String symbol, long orderId) {
        OrderBook orderBook = orderBooks.get(symbol);
        if (orderBook == null) {
            return false;
        }
        return orderBook.cancelOrder(orderId);
    }

    public boolean modifyOrder(String symbol, long orderId, java.math.BigDecimal newPrice, java.math.BigDecimal newQuantity) {
        OrderBook orderBook = orderBooks.get(symbol);
        if (orderBook == null) {
            return false;
        }
        return orderBook.modifyOrder(orderId, newPrice, newQuantity);
    }

    public Map<String, OrderBook> getOrderBooks() {
        return new HashMap<>(orderBooks);
    }
}