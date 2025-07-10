package com.matching.mc;
import com.matching.entity.Order;
import com.matching.entity.Trade;
import com.matching.enums.OrderSide;
import com.matching.enums.OrderStatus;
import com.matching.enums.OrderType;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

@Slf4j
public class OrderBook {
    private final String symbol;
    private final ConcurrentSkipListMap<BigDecimal, TreeMap<Long, Order>> asks = new ConcurrentSkipListMap<>();
    private final ConcurrentSkipListMap<BigDecimal, TreeMap<Long, Order>> bids = new ConcurrentSkipListMap<>(Comparator.reverseOrder());
    private final ConcurrentHashMap<Long, Order> orderMap = new ConcurrentHashMap<>();
    private final AtomicLong tradeIdGenerator = new AtomicLong(0);
    private final Consumer<Trade> tradeConsumer;

    public OrderBook(String symbol, Consumer<Trade> tradeConsumer) {
        this.symbol = symbol;
        this.tradeConsumer = tradeConsumer;
    }

    public synchronized void addOrder(Order order) {
        if (orderMap.containsKey(order.getOrderId())) {
            log.warn("订单ID已存在，跳过添加: orderId={}, symbol={}",
                    order.getOrderId(), order.getSymbol());
            return;
        }
        log.info("订单添加到订单簿: {}", order);

        orderMap.put(order.getOrderId(), order);

        if (order.getOrderSide() == OrderSide.BUY) {
            bids.computeIfAbsent(order.getPrice(), p -> new TreeMap<>())
                    .put(order.getOrderId(), order);
        } else {
            asks.computeIfAbsent(order.getPrice(), p -> new TreeMap<>())
                    .put(order.getOrderId(), order);
        }
    }

    public synchronized boolean cancelOrder(long orderId) {
        Order order = orderMap.get(orderId);
        if (order == null || !order.canBeMatched()) {
            return false;
        }

        ConcurrentSkipListMap<BigDecimal, TreeMap<Long, Order>> orderMap =
                order.getOrderSide() == OrderSide.BUY ? bids : asks;

        TreeMap<Long, Order> priceLevel = orderMap.get(order.getPrice());
        if (priceLevel != null) {
            priceLevel.remove(orderId);
            if (priceLevel.isEmpty()) {
                orderMap.remove(order.getPrice());
            }
            log.info("订单已取消: {}", order);
        }else {
            log.warn("尝试取消不存在的订单: {}", order);
        }
        order.setOrderStatus(OrderStatus.CANCELED);
        return true;
    }

    public synchronized boolean modifyOrder(long orderId, BigDecimal newPrice, BigDecimal newQuantity) {
        Order order = orderMap.get(orderId);
        if (order == null || !order.canBeMatched()) {
            log.warn("修改订单失败: 订单不存在或已完成，orderId={}", orderId);
            return false;
        }

        log.info("修改订单: 原订单={}, 新价格={}, 新数量={}",
                order, newPrice, newQuantity);

        // 1. 从原价格水平移除订单
        ConcurrentSkipListMap<BigDecimal, TreeMap<Long, Order>> originalOrderMap =
                order.getOrderSide() == OrderSide.BUY ? bids : asks;

        TreeMap<Long, Order> priceLevel = originalOrderMap.get(order.getPrice());
        if (priceLevel != null) {
            priceLevel.remove(orderId);
            if (priceLevel.isEmpty()) {
                originalOrderMap.remove(order.getPrice());
            }
        }

        // 更新订单信息
        if (newPrice != null) {
            order.setPrice(newPrice);
        }
        if (newQuantity != null) {
            order.setQuantity(newQuantity);
        }

        // 从orderMap中移除
        orderMap.remove(order.getOrderId());
        // 添加到新价格水平
        addOrder(order);
        //开始撮合
        matchOrders(order);
        return true;
    }

    public synchronized List<Trade> matchOrders(Order newOrder) {
        List<Trade> trades = new ArrayList<>();
        OrderSide oppositeSide = newOrder.getOrderSide() == OrderSide.BUY ? OrderSide.SELL : OrderSide.BUY;
        ConcurrentSkipListMap<BigDecimal, TreeMap<Long, Order>> oppositeOrders =
                oppositeSide == OrderSide.BUY ? bids : asks;

        BigDecimal remainingQty = newOrder.getQuantity().subtract(newOrder.getExecutedQuantity());

        for (Map.Entry<BigDecimal, TreeMap<Long, Order>> entry : oppositeOrders.entrySet()) {
            BigDecimal price = entry.getKey();

            // 检查价格是否匹配
            if ((newOrder.getOrderSide() == OrderSide.BUY && price.compareTo(newOrder.getPrice()) > 0) ||
                    (newOrder.getOrderSide() == OrderSide.SELL && price.compareTo(newOrder.getPrice()) < 0)) {
                break;
            }

            TreeMap<Long, Order> ordersAtPrice = entry.getValue();
            Iterator<Map.Entry<Long, Order>> iterator = ordersAtPrice.entrySet().iterator();

            while (iterator.hasNext() && remainingQty.compareTo(BigDecimal.ZERO) > 0) {
                Map.Entry<Long, Order> orderEntry = iterator.next();
                Order existingOrder = orderEntry.getValue();

                if (!existingOrder.canBeMatched()) {
                    iterator.remove();
                    continue;
                }

                BigDecimal matchQty =
                        remainingQty.compareTo(existingOrder.getQuantity().subtract(existingOrder.getExecutedQuantity())) < 0 ?
                                remainingQty : existingOrder.getQuantity().subtract(existingOrder.getExecutedQuantity());

                // 创建成交记录
                Trade trade = createTrade(newOrder, existingOrder, price, matchQty);
                trades.add(trade);
                tradeConsumer.accept(trade);
                log.info("成交记录: {}", trade);

                // 更新订单执行量
                newOrder.execute(matchQty);
                existingOrder.execute(matchQty);

                remainingQty = remainingQty.subtract(matchQty);

                // 移除已完全成交的订单
                if (existingOrder.isFullyExecuted()) {
                    iterator.remove();
                    this.orderMap.remove(existingOrder.getOrderId());
                    log.debug("订单完全成交，从订单簿移除: {}", existingOrder);
                }
            }

            // 移除空价格水平
            if (ordersAtPrice.isEmpty()) {
                oppositeOrders.remove(price);
                log.debug("价格水平为空，从订单簿移除: {}", entry.getKey());
            }

            if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
        }

        // 如果新订单还有剩余量且不是市价单，则添加到订单簿
        if (remainingQty.compareTo(BigDecimal.ZERO) > 0 && newOrder.getOrderType() != OrderType.MARKET) {
            addOrder(newOrder);
        } else if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) {
            this.orderMap.remove(newOrder.getOrderId());
        }

        return trades;
    }

    private Trade createTrade(Order buyer, Order seller, BigDecimal price, BigDecimal quantity) {
        Long buyOrderId = buyer.getOrderId();
        Long sellOrderId = seller.getOrderId();
        if (buyer.getOrderSide() == OrderSide.SELL) {
            buyOrderId = seller.getOrderId();
            sellOrderId = buyer.getOrderId();
        }
        return Trade.builder()
                .tradeId(String.valueOf(tradeIdGenerator.incrementAndGet()))
                .symbol(symbol)
                .buyOrderId(buyOrderId)
                .sellOrderId(sellOrderId)
                .price(price)
                .quantity(quantity)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public String getSymbol() {
        return symbol;
    }

    public ConcurrentSkipListMap<BigDecimal, TreeMap<Long, Order>> getAsks() {
        return asks;
    }

    public ConcurrentSkipListMap<BigDecimal, TreeMap<Long, Order>> getBids() {
        return bids;
    }
}
