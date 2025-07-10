package com.matching.service;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.matching.dto.OrderEvent;
import com.matching.entity.Order;
import com.matching.enums.OrderCommand;
import com.matching.entity.Trade;
import com.matching.factory.OrderEventFactory;
import com.matching.handler.OrderEventHandler;
import com.matching.mc.MatchingEngine;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Service
public class ExchangeService {
    private static final Logger logger = LoggerFactory.getLogger(ExchangeService.class);
    private static final int RING_BUFFER_SIZE = 1024 * 1024; // 1M events

    private Disruptor<OrderEvent> disruptor;
    private RingBuffer<OrderEvent> ringBuffer;
    private ExecutorService executor;
    private MatchingEngine matchingEngine;

    @PostConstruct
    public void init() {
        // 初始化线程池
        executor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r);
            thread.setName("disruptor-exchange-thread");
            thread.setDaemon(true);
            return thread;
        });

        // 初始化事件工厂
        OrderEventFactory factory = new OrderEventFactory();
        ThreadFactory threadFactory = Executors.defaultThreadFactory();

        // 初始化Disruptor
        disruptor = new Disruptor<>(
                factory,
                RING_BUFFER_SIZE,
                threadFactory,
                ProducerType.SINGLE,
                new BlockingWaitStrategy()
        );

        // 初始化撮合引擎
        matchingEngine = new MatchingEngine(this::handleTrade);

        // 设置事件处理器
        disruptor.handleEventsWith(new OrderEventHandler(matchingEngine));

        // 启动Disruptor
        ringBuffer = disruptor.start();

        logger.info("ExchangeService initialized and disruptor started");
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down ExchangeService");
        if (disruptor != null) {
            disruptor.shutdown();
        }
        if (executor != null) {
            executor.shutdown();
        }
        logger.info("ExchangeService shutdown complete");
    }

    public void submitOrder(Order order) {
        long sequence = ringBuffer.next();
        try {
            OrderEvent event = ringBuffer.get(sequence);
            event.setOrder(order);
            event.setCommand(OrderCommand.NEW_ORDER);
            event.setSequence(sequence);
        } finally {
            ringBuffer.publish(sequence);
        }
    }

    public void cancelOrder(Order order) {
        long sequence = ringBuffer.next();
        try {
            OrderEvent event = ringBuffer.get(sequence);
            event.setOrder(order);
            event.setCommand(OrderCommand.CANCEL_ORDER);
            event.setSequence(sequence);
        } finally {
            ringBuffer.publish(sequence);
        }
    }

    public void modifyOrder(Order order) {
        long sequence = ringBuffer.next();
        try {
            OrderEvent event = ringBuffer.get(sequence);
            event.setOrder(order);
            event.setCommand(OrderCommand.MODIFY_ORDER);
            event.setSequence(sequence);
        } finally {
            ringBuffer.publish(sequence);
        }
    }

    private void handleTrade(Trade trade) {
        // 处理成交结果，如记录日志、发送通知等
        logger.info("Trade executed: {}", trade);
        // 这里可以添加更多处理逻辑，如发送成交信息到客户端
    }

    public MatchingEngine getMatchingEngine() {
        return matchingEngine;
    }
}
