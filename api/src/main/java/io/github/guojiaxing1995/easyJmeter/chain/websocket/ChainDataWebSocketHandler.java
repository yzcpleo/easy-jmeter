package io.github.guojiaxing1995.easyJmeter.chain.websocket;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import io.github.guojiaxing1995.easyJmeter.model.ChainLatencyDataDO;
import io.github.guojiaxing1995.easyJmeter.chain.service.ChainDataCollectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 链路数据WebSocket处理器
 * 用于实时推送链路延时数据
 *
 * @author Assistant
 * @version 1.0.0
 */
@Slf4j
@Component
public class ChainDataWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private ChainDataCollectionService chainDataCollectionService;

    // 存储所有连接的会话
    private static final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    // 存储订阅特定链路的会话 (chainId -> Set<sessionId>)
    private static final Map<Long, CopyOnWriteArraySet<String>> chainSubscriptions = new ConcurrentHashMap<>();

    // 存储会话ID与会话的映射 (sessionId -> WebSocketSession)
    private static final Map<String, WebSocketSession> sessionMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        sessionMap.put(session.getId(), session);
        log.info("WebSocket连接建立: sessionId={}", session.getId());

        // 发送连接成功消息
        sendMessage(session, createMessage("connection", "connected", "WebSocket连接成功", null));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        sessionMap.remove(session.getId());

        // 清理链路订阅
        for (Map.Entry<Long, CopyOnWriteArraySet<String>> entry : chainSubscriptions.entrySet()) {
            entry.getValue().remove(session.getId());
        }

        log.info("WebSocket连接关闭: sessionId={}, status={}", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket传输错误: sessionId={}", session.getId(), exception);

        // 清理会话
        sessions.remove(session);
        sessionMap.remove(session.getId());

        // 清理链路订阅
        for (Map.Entry<Long, CopyOnWriteArraySet<String>> entry : chainSubscriptions.entrySet()) {
            entry.getValue().remove(session.getId());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            log.debug("收到WebSocket消息: sessionId={}, message={}", session.getId(), payload);

            // 解析消息
            JSONObject request = JSON.parseObject(payload);
            String type = (String) request.get("type");

            switch (type) {
                case "subscribe":
                    handleSubscribeMessage(session, request);
                    break;
                case "unsubscribe":
                    handleUnsubscribeMessage(session, request);
                    break;
                case "get_status":
                    handleGetStatusMessage(session, request);
                    break;
                case "get_latency_data":
                    handleGetLatencyDataMessage(session, request);
                    break;
                case "ping":
                    handlePingMessage(session);
                    break;
                default:
                    log.warn("未知的消息类型: {}", type);
                    sendMessage(session, createError("未知消息类型: " + type));
            }
        } catch (Exception e) {
            log.error("处理WebSocket消息失败: sessionId={}", session.getId(), e);
            sendMessage(session, createError("消息处理失败: " + e.getMessage()));
        }
    }

    /**
     * 处理订阅消息
     */
    private void handleSubscribeMessage(WebSocketSession session, JSONObject request) {
        Long chainId = getLongValue(request.get("chainId"));
        Long taskId = getLongValue(request.get("taskId"));

        if (chainId != null) {
            // 订阅特定链路
            chainSubscriptions.computeIfAbsent(chainId, k -> new CopyOnWriteArraySet<>()).add(session.getId());

            ChainDataCollectionService.DataCollectionStatus status =
                    chainDataCollectionService.getDataCollectionStatus(taskId, chainId);

            sendMessage(session, createMessage("subscribe", "success",
                    "订阅链路成功: " + chainId, status));
        } else {
            sendMessage(session, createError("链路ID不能为空"));
        }
    }

    /**
     * 处理取消订阅消息
     */
    private void handleUnsubscribeMessage(WebSocketSession session, JSONObject request) {
        Long chainId = getLongValue(request.get("chainId"));

        if (chainId != null) {
            CopyOnWriteArraySet<String> subscribers = chainSubscriptions.get(chainId);
            if (subscribers != null) {
                subscribers.remove(session.getId());
                if (subscribers.isEmpty()) {
                    chainSubscriptions.remove(chainId);
                }
            }

            sendMessage(session, createMessage("unsubscribe", "success",
                    "取消订阅链路成功: " + chainId, null));
        } else {
            sendMessage(session, createError("链路ID不能为空"));
        }
    }

    /**
     * 处理获取状态消息
     */
    private void handleGetStatusMessage(WebSocketSession session, JSONObject request) {
        Long taskId = getLongValue(request.get("taskId"));
        Long chainId = getLongValue(request.get("chainId"));

        if (taskId != null && chainId != null) {
            ChainDataCollectionService.DataCollectionStatus status =
                    chainDataCollectionService.getDataCollectionStatus(taskId, chainId);

            sendMessage(session, createMessage("status", "success",
                    "获取状态成功", status));
        } else {
            sendMessage(session, createError("任务ID和链路ID不能为空"));
        }
    }

    /**
     * 处理获取延时数据消息
     */
    private void handleGetLatencyDataMessage(WebSocketSession session, JSONObject request) {
        String requestId = (String) request.get("requestId");

        if (requestId != null && !requestId.trim().isEmpty()) {
            List<ChainLatencyDataDO> dataList =
                    chainDataCollectionService.correlateRequestData(requestId);

            sendMessage(session, createMessage("latency_data", "success",
                    "获取延时数据成功", dataList));
        } else {
            sendMessage(session, createError("请求ID不能为空"));
        }
    }

    /**
     * 处理心跳消息
     */
    private void handlePingMessage(WebSocketSession session) {
        sendMessage(session, createMessage("pong", "success", "pong", null));
    }

    /**
     * 推送链路延时数据给订阅者
     */
    public void pushLatencyData(Long chainId, ChainLatencyDataDO latencyData) {
        CopyOnWriteArraySet<String> subscribers = chainSubscriptions.get(chainId);
        if (subscribers != null && !subscribers.isEmpty()) {
            String message = createMessage("latency_update", "data",
                    "链路延时数据更新", latencyData);

            for (String sessionId : subscribers) {
                WebSocketSession session = sessionMap.get(sessionId);
                if (session != null && session.isOpen()) {
                    sendMessage(session, message);
                }
            }
        }
    }

    /**
     * 推送收集状态更新给订阅者
     */
    public void pushStatusUpdate(Long chainId, ChainDataCollectionService.DataCollectionStatus status) {
        CopyOnWriteArraySet<String> subscribers = chainSubscriptions.get(chainId);
        if (subscribers != null && !subscribers.isEmpty()) {
            String message = createMessage("status_update", "data",
                    "收集状态更新", status);

            for (String sessionId : subscribers) {
                WebSocketSession session = sessionMap.get(sessionId);
                if (session != null && session.isOpen()) {
                    sendMessage(session, message);
                }
            }
        }
    }

    /**
     * 广播系统消息
     */
    public void broadcastSystemMessage(String message) {
        String wsMessage = createMessage("system", "info", message, null);
        broadcastMessage(wsMessage);
    }

    /**
     * 广播消息给所有连接的会话
     */
    private void broadcastMessage(String message) {
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                sendMessage(session, message);
            }
        }
    }

    /**
     * 发送消息给指定会话
     */
    private void sendMessage(WebSocketSession session, String message) {
        try {
            if (session != null && session.isOpen()) {
                synchronized (session) {
                    session.sendMessage(new TextMessage(message));
                }
            }
        } catch (IOException e) {
            log.error("发送WebSocket消息失败: sessionId={}", session.getId(), e);
        }
    }

    /**
     * 创建WebSocket消息
     */
    private String createMessage(String type, String status, String message, Object data) {
        Map<String, Object> msg = new ConcurrentHashMap<>();
        msg.put("type", type);
        msg.put("status", status);
        msg.put("message", message);
        msg.put("data", data);
        msg.put("timestamp", LocalDateTime.now().toString());
        return JSON.toJSONString(msg);
    }

    /**
     * 创建错误消息
     */
    private String createError(String errorMessage) {
        return createMessage("error", "error", errorMessage, null);
    }

    /**
     * 从对象获取Long值
     */
    private Long getLongValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 获取当前连接数
     */
    public int getConnectionCount() {
        return sessions.size();
    }

    /**
     * 获取特定链路的订阅数
     */
    public int getSubscriptionCount(Long chainId) {
        CopyOnWriteArraySet<String> subscribers = chainSubscriptions.get(chainId);
        return subscribers != null ? subscribers.size() : 0;
    }
}