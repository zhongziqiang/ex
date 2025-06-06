package com.org.kline.config;


import com.org.kline.handler.BinanceWebSocketHandler;
import com.org.kline.handler.MyWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

@EnableWebSocket
@Configuration
public class WebSocketConfig implements WebSocketConfigurer {

    private static final String BINANCE_WS_URL = "wss://stream.testnet.binance.vision/ws"; //TEST

    @Bean
    public WebSocketConnectionManager webSocketConnectionManager() {
        WebSocketConnectionManager manager = new WebSocketConnectionManager(
                new StandardWebSocketClient(),
                binanceWebSocketHandler(),
                BINANCE_WS_URL
        );
        manager.setAutoStartup(true);
        return manager;
    }

    @Bean
    public BinanceWebSocketHandler binanceWebSocketHandler() {
        return new BinanceWebSocketHandler();
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(myWebSocketHandler(), "/ws")
                .addInterceptors(new HttpSessionHandshakeInterceptor())
                .setAllowedOrigins("*"); // 允许所有来源，生产环境应限制

        // 如果需要支持SockJS（浏览器兼容性）
        registry.addHandler(myWebSocketHandler(), "/ws/sockjs")
                .setAllowedOrigins("*")
                .withSockJS();
    }
    @Bean
    public WebSocketHandler myWebSocketHandler() {
        return new MyWebSocketHandler();
    }
}
