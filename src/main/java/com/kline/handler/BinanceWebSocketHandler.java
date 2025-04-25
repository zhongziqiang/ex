package com.kline.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
public class BinanceWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private boolean status = true;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("Connected to Binance WebSocket");

        // 订阅BTC/USDT的1分钟K线
        String subscribeMessage = """
            {
              "method": "SUBSCRIBE",
              "params": [
                    "btcusdt@kline_1m",
                    "ethusdt@kline_1m",
                    "bnbusdt@kline_5m"
              ],
              "id": 1
            }
            """;
        if (status){
            session.sendMessage(new TextMessage(subscribeMessage));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        JsonNode jsonNode = objectMapper.readTree(payload);

        // 处理K线数据
        if (jsonNode.has("k")) {
            JsonNode kline = jsonNode.get("k");
            log.info("Received Kline: {}", kline);
            // 这里可以添加业务处理逻辑，如存储到数据库等
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.warn("Disconnected from Binance WebSocket: {}", status);
        // 可以在这里实现重连逻辑
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket error", exception);
    }
}
