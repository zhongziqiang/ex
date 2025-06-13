package com.matching.test;

import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.matching.dto.OrderEvent;
import com.matching.factory.OrderEventFactory;
import com.matching.handler.MatchingEventHandler;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class MatchingEngine {
    private Disruptor<OrderEvent> disruptor;
    private com.lmax.disruptor.RingBuffer<OrderEvent> ringBuffer;

    public MatchingEngine() {
        ThreadFactory threadFactory = Executors.defaultThreadFactory();

        disruptor = new Disruptor<>(
                new OrderEventFactory(),
                1024,
                threadFactory,
                ProducerType.SINGLE,
                new BlockingWaitStrategy()
        );

        disruptor.handleEventsWith(new MatchingEventHandler());
        disruptor.start();

        ringBuffer = disruptor.getRingBuffer();
    }

    public void onNewOrder(long orderId, boolean isBuy, double price, int quantity) {
        long seq = ringBuffer.next();
        try {
            OrderEvent event = ringBuffer.get(seq);
            event.set(orderId, isBuy, price, quantity);
        } finally {
            ringBuffer.publish(seq);
        }
    }

    public void shutdown() {
        disruptor.shutdown();
    }

    public static void main(String[] args) throws InterruptedException {
        MatchingEngine engine = new MatchingEngine();

        engine.onNewOrder(1L, true, 100.0, 10);  // 买单
        engine.onNewOrder(2L, false, 99.0, 5);   // 卖单，触发撮合
        engine.onNewOrder(3L, false, 101.0, 10); // 卖单，入队
        engine.onNewOrder(4L, true, 102.0, 7);   // 买单，触发撮合
        engine.onNewOrder(5L, false, 100.0, 3);  // 卖单，触发撮合

        Thread.sleep(1000);

        engine.shutdown();
    }
}
