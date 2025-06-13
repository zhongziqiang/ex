package com.matching.dto;

import java.util.Comparator;
import java.util.PriorityQueue;

public class OrderBook {
    private PriorityQueue<OrderEvent> buyOrders = new PriorityQueue<>(
            Comparator.comparingDouble(OrderEvent::getPrice).reversed()
    );

    private PriorityQueue<OrderEvent> sellOrders = new PriorityQueue<>(
            Comparator.comparingDouble(OrderEvent::getPrice)
    );

    public void matchBuyOrder(OrderEvent buyOrder) {
        while (buyOrder.getQuantity() > 0 && !sellOrders.isEmpty() && sellOrders.peek().getPrice() <= buyOrder.getPrice()) {
            OrderEvent sellOrder = sellOrders.peek();

            int tradedQty = Math.min(buyOrder.getQuantity(), sellOrder.getQuantity());
            System.out.printf("成交：买单[%d] 卖单[%d] 数量=%d 价格=%.2f%n",
                    buyOrder.getOrderId(), sellOrder.getOrderId(), tradedQty, sellOrder.getPrice());

            buyOrder = updateOrderQuantity(buyOrder, buyOrder.getQuantity() - tradedQty);
            sellOrder = updateOrderQuantity(sellOrder, sellOrder.getQuantity() - tradedQty);

            if (sellOrder.getQuantity() == 0) {
                sellOrders.poll();
            } else {
                sellOrders.poll();
                sellOrders.offer(sellOrder);
            }
        }

        if (buyOrder.getQuantity() > 0) {
            buyOrders.offer(buyOrder);
            System.out.println("买单剩余入队：" + buyOrder.getOrderId() + " 数量=" + buyOrder.getQuantity());
        }
    }

    public void matchSellOrder(OrderEvent sellOrder) {
        while (sellOrder.getQuantity() > 0 && !buyOrders.isEmpty() && buyOrders.peek().getPrice() >= sellOrder.getPrice()) {
            OrderEvent buyOrder = buyOrders.peek();

            int tradedQty = Math.min(sellOrder.getQuantity(), buyOrder.getQuantity());
            System.out.printf("成交：卖单[%d] 买单[%d] 数量=%d 价格=%.2f%n",
                    sellOrder.getOrderId(), buyOrder.getOrderId(), tradedQty, buyOrder.getPrice());

            sellOrder = updateOrderQuantity(sellOrder, sellOrder.getQuantity() - tradedQty);
            buyOrder = updateOrderQuantity(buyOrder, buyOrder.getQuantity() - tradedQty);

            if (buyOrder.getQuantity() == 0) {
                buyOrders.poll();
            } else {
                buyOrders.poll();
                buyOrders.offer(buyOrder);
            }
        }

        if (sellOrder.getQuantity() > 0) {
            sellOrders.offer(sellOrder);
            System.out.println("卖单剩余入队：" + sellOrder.getOrderId() + " 数量=" + sellOrder.getQuantity());
        }
    }

    private OrderEvent updateOrderQuantity(OrderEvent order, int newQty) {
        OrderEvent newOrder = new OrderEvent();
        newOrder.set(order.getOrderId(), order.isBuy(), order.getPrice(), newQty);
        return newOrder;
    }
}
