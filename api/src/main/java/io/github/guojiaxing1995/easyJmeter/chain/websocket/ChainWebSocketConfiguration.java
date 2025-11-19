package io.github.guojiaxing1995.easyJmeter.chain.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 链路数据WebSocket配置
 *
 * @author Assistant
 * @version 1.0.0
 */
@Configuration
@EnableWebSocket
public class ChainWebSocketConfiguration implements WebSocketConfigurer {

    private final ChainDataWebSocketHandler chainDataWebSocketHandler;

    public ChainWebSocketConfiguration(ChainDataWebSocketHandler chainDataWebSocketHandler) {
        this.chainDataWebSocketHandler = chainDataWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chainDataWebSocketHandler, "/ws/chain/data")
                .setAllowedOrigins("*") // 允许所有来源，生产环境应该限制
                .withSockJS(); // 启用SockJS支持
    }
}