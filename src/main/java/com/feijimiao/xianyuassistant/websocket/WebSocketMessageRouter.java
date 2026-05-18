package com.feijimiao.xianyuassistant.websocket;

import com.feijimiao.xianyuassistant.service.WebSocketService;
import com.feijimiao.xianyuassistant.websocket.handler.*;
import com.feijimiao.xianyuassistant.service.AccountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WebSocket消息路由器
 * 根据lwp路径分发消息到不同的业务处理器
 * 使用模板模式，每种lwp类型对应一个处理器类
 */
@Slf4j
@Component
public class WebSocketMessageRouter {
    
    // 路由表：lwp路径 -> 处理器
    private final Map<String, AbstractLwpHandler> handlerMap = new HashMap<>();
    
    // 初始化标志
    private volatile boolean initialized = false;
    
    // 自动注入所有处理器
    @Autowired(required = false)
    private List<AbstractLwpHandler> handlers;

    @Autowired
    private AccountService accountService;

    @Lazy
    @Autowired
    private WebSocketService webSocketService;
    
    /**
     * 延迟初始化处理器
     */
    private void ensureInitialized() {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    registerHandlers();
                    initialized = true;
                }
            }
        }
    }
    
    /**
     * 注册所有处理器
     */
    private void registerHandlers() {
        if (handlers == null || handlers.isEmpty()) {
            log.warn("未找到任何WebSocket消息处理器");
            return;
        }
        
        for (AbstractLwpHandler handler : handlers) {
            String lwpPath = handler.getLwpPath();
            handlerMap.put(lwpPath, handler);
            log.info("注册WebSocket处理器: {} -> {}", lwpPath, handler.getClass().getSimpleName());
        }
        
        // 同步包消息支持两种路径
        AbstractLwpHandler syncHandler = handlerMap.get("/s/para");
        if (syncHandler != null) {
            handlerMap.put("/s/sync", syncHandler);
            log.info("注册WebSocket处理器: /s/sync -> {}", syncHandler.getClass().getSimpleName());
        }
        
        log.info("WebSocket消息路由器初始化完成，共注册 {} 个处理器", handlerMap.size());
    }
    
    /**
     * 手动注册处理器
     * 
     * @param handler 处理器实例
     */
    public void registerHandler(AbstractLwpHandler handler) {
        String lwpPath = handler.getLwpPath();
        handlerMap.put(lwpPath, handler);
        log.info("手动注册处理器: {} -> {}", lwpPath, handler.getClass().getSimpleName());
    }
    
    /**
     * 路由消息到对应的处理器
     * 
     * @param accountId 账号ID
     * @param messageData 消息数据
     */
    public void route(String accountId, Map<String, Object> messageData) {
        try {
            // 确保已初始化
            ensureInitialized();
            
            // 获取lwp路径
            Object lwpObj = messageData.get("lwp");
            String lwp = lwpObj != null ? lwpObj.toString() : null;
            
            if (lwp == null) {
                // 没有lwp字段，可能是响应消息
                log.debug("【账号{}】收到无lwp字段的消息，可能是响应消息", accountId);
                handleResponseMessage(accountId, messageData);
                return;
            }
            
            // 查找对应的处理器
            AbstractLwpHandler handler = handlerMap.get(lwp);
            
            if (handler != null) {
                log.debug("【账号{}】路由消息: {} -> {}", accountId, lwp, handler.getClass().getSimpleName());
                handler.handle(accountId, messageData);
            } else {
                log.debug("【账号{}】未找到处理器: {}", accountId, lwp);
                handleUnknownMessage(accountId, messageData);
            }
            
        } catch (Exception e) {
            log.error("【账号{}】路由消息失败", accountId, e);
        }
    }
    

    
    /**
     * 处理响应消息（没有lwp字段，但有code字段）
     */
    private void handleResponseMessage(String accountId, Map<String, Object> messageData) {
        Object code = messageData.get("code");
        if (code == null) {
            handleUnknownMessage(accountId, messageData);
            return;
        }
        
        log.debug("【账号{}】处理响应消息: code={}", accountId, code);
        
        // 完成等待中的sendMessage Future
        try {
            Long accountIdLong = Long.parseLong(accountId);
            Object headersObj = messageData.get("headers");
            if (headersObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> headers = (Map<String, Object>) headersObj;
                Object mid = headers.get("mid");
                if (mid != null) {
                    int codeValue = Integer.parseInt(code.toString());
                    ((com.feijimiao.xianyuassistant.service.impl.WebSocketServiceImpl) webSocketService)
                        .completePendingResponse(accountIdLong, mid.toString(), codeValue);
                }
            }
        } catch (Exception e) {
            log.debug("完成pendingResponse失败: {}", e.getMessage());
        }
        
        try {
            int codeValue = Integer.parseInt(code.toString());
            
            if (codeValue == 200) {
                // 检查是否是消息发送响应
                Object bodyObj = messageData.get("body");
                if (bodyObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> body = (Map<String, Object>) bodyObj;
                    
                    // 如果body中包含messageId，说明是消息发送响应
                    if (body.containsKey("messageId")) {
                        log.debug("【账号{}】识别为消息发送响应", accountId);
                        AbstractLwpHandler handler = handlerMap.get("/r/MessageSend/sendByReceiverScope");
                        if (handler != null) {
                            handler.handle(accountId, messageData);
                            return;
                        }
                    }
                }
                
                log.debug("【账号{}】收到成功响应(200)", accountId);
            } else if (codeValue == 401) {
                log.error("【账号{}】Token失效(401)，需要重新获取Token", accountId);
                updateCookieStatusIfPresent(accountId, 2);
            } else if (codeValue == 500) {
                log.error("【账号{}】服务器错误(500)", accountId);
                updateCookieStatusIfPresent(accountId, 2);
            } else {
                log.warn("【账号{}】未知响应码: {}", accountId, code);
            }
        } catch (Exception e) {
            log.warn("【账号{}】解析响应码失败: {}", accountId, code);
        }
    }
    
    /**
     * 处理未知消息
     */
    private void handleUnknownMessage(String accountId, Map<String, Object> messageData) {
        log.debug("【账号{}】收到未知类型消息", accountId);
        
        Object type = messageData.get("type");
        Object lwp = messageData.get("lwp");
        
        if (lwp != null) {
            log.debug("【账号{}】未注册的lwp路径: {}", accountId, lwp);
        }
        if (type != null) {
            log.debug("【账号{}】消息类型: {}", accountId, type);
        }
    }

    private void updateCookieStatusIfPresent(String accountId, int status) {
        try {
            Long id = Long.parseLong(accountId);
            accountService.updateCookieStatus(id, status);
        } catch (NumberFormatException e) {
            log.warn("无法解析accountId: {}", accountId);
        }
    }
    
    /**
     * 移除处理器
     */
    public void unregisterHandler(String lwp) {
        handlerMap.remove(lwp);
        log.info("移除处理器: {}", lwp);
    }
    
    /**
     * 获取所有已注册的路由
     */
    public java.util.Set<String> getRegisteredRoutes() {
        return handlerMap.keySet();
    }
    
    /**
     * 获取处理器
     */
    public AbstractLwpHandler getHandler(String lwp) {
        return handlerMap.get(lwp);
    }
}
