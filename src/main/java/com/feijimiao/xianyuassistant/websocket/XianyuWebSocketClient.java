package com.feijimiao.xianyuassistant.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feijimiao.xianyuassistant.utils.AccountDisplayNameUtils;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 闲鱼WebSocket客户端
 * 用于监听闲鱼消息
 * 参考Python代码的WebSocketClient和消息处理机制
 */
@Slf4j
public class XianyuWebSocketClient extends WebSocketClient {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String accountId;
    private final AccountDisplayNameUtils displayNameUtils;
    private boolean isConnected = false;
    
    // 当前用户ID（从Cookie的unb字段获取）
    private String myUserId = null;
    
    // 消息处理并发控制（参考Python的_handle_message_with_semaphore）
    private final Semaphore messageSemaphore = new Semaphore(100); // 最多100个并发消息处理（参考Python）
    private final ExecutorService messageExecutor = Executors.newFixedThreadPool(10);
    
    // 消息处理器
    private WebSocketMessageHandler messageHandler;
    
    // 消息统计
    private long messageCount = 0;
    private long lastMessageTime = 0;
    
    // 会话信息
    private String sessionId = null;  // 保存注册后的sid
    
    // 注册成功回调
    private Runnable onRegistrationSuccess;
    
    // Token失效回调
    private Runnable onTokenExpired;
    
    // 心跳响应回调
    private Runnable onHeartbeatResponse;

    public XianyuWebSocketClient(URI serverUri, Map<String, String> headers, String accountId, AccountDisplayNameUtils displayNameUtils) {
        super(serverUri, headers);
        this.accountId = accountId;
        this.displayNameUtils = displayNameUtils;
    }
    
    /**
     * 获取账号显示名称
     */
    private String getDisplayName() {
        return displayNameUtils != null ? displayNameUtils.getDisplayName(accountId) : "账号" + accountId;
    }
    
    /**
     * 格式化日志前缀
     */
    private String logPrefix() {
        return "【" + getDisplayName() + "】";
    }
    
    /**
     * 设置消息处理器
     */
    public void setMessageHandler(WebSocketMessageHandler handler) {
        this.messageHandler = handler;
    }
    
    /**
     * 设置注册成功回调
     */
    public void setOnRegistrationSuccess(Runnable callback) {
        this.onRegistrationSuccess = callback;
    }
    
    /**
     * 设置当前用户ID
     */
    public void setMyUserId(String userId) {
        this.myUserId = userId;
    }
    
    /**
     * 设置Token失效回调
     */
    public void setOnTokenExpired(Runnable callback) {
        this.onTokenExpired = callback;
    }
    
    /**
     * 设置心跳响应回调
     */
    public void setOnHeartbeatResponse(Runnable callback) {
        this.onHeartbeatResponse = callback;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        isConnected = true;
        String displayName = getDisplayName();
        log.info("【{}】==================== WebSocket连接建立成功 ====================", displayName);
        log.info("【{}】服务器握手状态: {}", displayName, handshakedata.getHttpStatus());
        log.info("【{}】服务器握手消息: {}", displayName, handshakedata.getHttpStatusMessage());
        log.info("【{}】连接已就绪，等待初始化和接收消息...", displayName);
        log.info("【{}】WebSocket连接状态正常，等待服务器消息...", displayName);
        log.info("【{}】准备进入消息接收循环...", displayName);
        log.info("【{}】================================================================", displayName);
    }

    @Override
    public void onMessage(String message) {
        // 使用信号量控制并发处理（参考Python的_handle_message_with_semaphore）
        messageExecutor.submit(() -> handleMessageWithSemaphore(message));
    }
    
    /**
     * 带信号量的消息处理包装器
     * 参考Python的_handle_message_with_semaphore方法
     */
    private void handleMessageWithSemaphore(String message) {
        try {
            // 获取信号量许可
            messageSemaphore.acquire();
            
            try {
                // 实际处理消息
                handleMessage(message);
            } catch (Exception e) {
                // 确保即使处理失败也记录错误
                log.error("【账号{}】消息处理异常", accountId, e);
            } finally {
                // 释放信号量许可
                messageSemaphore.release();
            }
            
        } catch (InterruptedException e) {
            log.error("【账号{}】消息处理被中断", accountId, e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("【账号{}】信号量处理异常", accountId, e);
        }
    }
    
    /**
     * 消息处理核心逻辑
     * 参考Python的handle_message方法
     */
    private void handleMessage(String message) {
        try {
            messageCount++;
            lastMessageTime = System.currentTimeMillis();
            
            if (message == null || message.isEmpty()) {
                log.warn("【账号{}】收到空消息", accountId);
                return;
            }

            // 尝试解析JSON
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> messageData = objectMapper.readValue(message, Map.class);
                
                // 识别消息类型（仅用于调试）
                Object lwpType = messageData.get("lwp");
                Object codeType = messageData.get("code");
                
                // 只记录重要的消息类型
                if (lwpType != null && !"/!".equals(lwpType.toString())) {
                    log.debug("【账号{}】收到消息: lwp={}", accountId, lwpType);
                }
                
                // 检查消息类型和解密（参考Python的handle_message）
                Object lwp = messageData.get("lwp");
                
                // 处理同步包消息 /s/para 和 /s/sync（用户消息）
                if (("/s/para".equals(lwp) || "/s/sync".equals(lwp)) && messageData.containsKey("body")) {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> body = (Map<String, Object>) messageData.get("body");
                        
                        if (body != null && body.containsKey("syncPushPackage")) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> syncPushPackage = (Map<String, Object>) body.get("syncPushPackage");
                            
                            if (syncPushPackage != null && syncPushPackage.containsKey("data")) {
                                @SuppressWarnings("unchecked")
                                java.util.List<Object> dataList = (java.util.List<Object>) syncPushPackage.get("data");
                                
                                if (dataList != null && !dataList.isEmpty()) {
                                    // 处理所有 data 项（可能有多条消息）
                                    for (int i = 0; i < dataList.size(); i++) {
                                        @SuppressWarnings("unchecked")
                                        Map<String, Object> syncData = (Map<String, Object>) dataList.get(i);
                                        
                                        if (syncData != null && syncData.containsKey("data")) {
                                            String encryptedData = syncData.get("data").toString();
                                            
                                            // 解密数据
                                            String decryptedData = com.feijimiao.xianyuassistant.utils.MessageDecryptUtils.decrypt(encryptedData);
                                            
                                            if (decryptedData != null) {
                                                // 将解密后的数据放回
                                                syncData.put("decryptedData", decryptedData);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.warn("【账号{}】解密同步包消息失败: {}", accountId, e.getMessage());
                    }
                }
                
                // 通用body解密（兼容其他消息类型）
                if (messageData.containsKey("body")) {
                    Object body = messageData.get("body");
                    if (body instanceof String) {
                        String bodyStr = (String) body;
                        String decryptedBody = com.feijimiao.xianyuassistant.utils.MessageDecryptUtils.tryDecrypt(bodyStr);
                        if (decryptedBody != null && !decryptedBody.equals(bodyStr)) {
                            messageData.put("decryptedBody", decryptedBody);
                        }
                    }
                }

                // 发送ACK确认消息（参考Python的handle_message方法）
                sendAckMessage(messageData);
                
                // 检查是否是心跳响应（参考Python的handle_heartbeat_response）
                // Python中心跳响应的判断是 code == 200
                Object code = messageData.get("code");
                
                // 检查是否是401错误（Token失效）
                if (code != null && (code.equals(401) || "401".equals(code.toString()))) {
                    log.error("【账号{}】❌ Token失效(401)，需要重新获取Token并重连", accountId);
                    
                    // 触发Token失效回调
                    if (onTokenExpired != null) {
                        try {
                            log.info("【账号{}】触发Token失效回调，准备重新获取Token...", accountId);
                            onTokenExpired.run();
                        } catch (Exception e) {
                            log.error("【账号{}】Token失效回调执行失败", accountId, e);
                        }
                    } else {
                        log.warn("【账号{}】未设置Token失效回调，无法自动重连", accountId);
                    }
                    return; // 不再继续处理
                }
                
                // 检查是否是心跳响应（参考Python的handle_heartbeat_response）
                // 心跳响应的特征：code=200 且 headers中有mid
                if (code != null && (code.equals(200) || "200".equals(code.toString()))) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> headers = (Map<String, Object>) messageData.get("headers");
                    if (headers != null && headers.containsKey("mid")) {
                        // 这是心跳响应
                        handleHeartbeatResponse();
                    }
                    
                    // 检查是否是注册响应，保存sid
                    if (headers != null && headers.containsKey("sid")) {
                        sessionId = headers.get("sid").toString();
                        log.info("【账号{}】已保存会话ID: {}", accountId, sessionId);
                    }
                    if (headers != null && headers.containsKey("reg-sid")) {
                        log.info("【账号{}】✅ 注册成功，reg-sid: {}", accountId, headers.get("reg-sid"));
                        
                        // 触发注册成功回调（保存Token）
                        if (onRegistrationSuccess != null) {
                            try {
                                log.info("【账号{}】触发注册成功回调，准备保存Token...", accountId);
                                onRegistrationSuccess.run();
                            } catch (Exception e) {
                                log.error("【账号{}】注册成功回调执行失败", accountId, e);
                            }
                        }
                    }
                }
                
                // 调用消息处理器
                if (messageHandler != null) {
                    messageHandler.handleMessage(accountId, messageData);
                }

            } catch (Exception e) {
                log.warn("【账号{}】消息解析失败: {}", accountId, e.getMessage());
            }

        } catch (Exception e) {
            log.error("【账号{}】消息处理失败", accountId, e);
            if (messageHandler != null) {
                messageHandler.handleError(accountId, e);
            }
        }
    }
    
    /**
     * 判断消息方向（发送还是接收）
     * 
     * @param decryptedData 解密后的JSON数据
     * @param accountId 当前账号ID
     * @return "【发】" 或 "【收】"
     */
    private String determineMessageDirection(String decryptedData, String accountId) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(decryptedData, Map.class);
            
            // 检查是否是已读回执（字段2=2）
            Object type = data.get("2");
            if (type != null && "2".equals(type.toString())) {
                return "【读】"; // 已读回执
            }
            
            // 检查是否是聊天消息（有字段1且是Map）
            Object field1 = data.get("1");
            if (field1 instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> messageInfo = (Map<String, Object>) field1;
                
                // 获取发送者（字段1.1.1）
                Object senderObj = messageInfo.get("1");
                if (senderObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> senderInfo = (Map<String, Object>) senderObj;
                    String sender = (String) senderInfo.get("1");
                    
                    // 获取接收者（字段1.2）
                    String receiver = (String) messageInfo.get("2");
                    
                    // 判断方向：如果接收者包含当前账号ID，说明是收到的消息
                    if (receiver != null && receiver.contains(accountId)) {
                        return "【收】";
                    } else if (sender != null && sender.contains(accountId)) {
                        return "【发】";
                    }
                }
            }
            
            return "【?】"; // 未知类型
            
        } catch (Exception e) {
            return "【?】";
        }
    }
    
    /**
     * 发送ACK确认消息
     * 参考Python的handle_message方法中的ACK发送逻辑
     * 
     * @param messageData 收到的消息数据
     */
    private void sendAckMessage(Map<String, Object> messageData) {
        try {
            // 检查消息是否包含headers
            if (!messageData.containsKey("headers")) {
                return;
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> headers = (Map<String, Object>) messageData.get("headers");
            
            // 构建ACK消息
            Map<String, Object> ack = new HashMap<>();
            ack.put("code", 200);
            
            Map<String, Object> ackHeaders = new HashMap<>();
            // 复制mid
            if (headers.containsKey("mid")) {
                ackHeaders.put("mid", headers.get("mid"));
            } else {
                // 生成mid
                String mid = com.feijimiao.xianyuassistant.utils.XianyuDeviceUtils.generateMid();
                ackHeaders.put("mid", mid);
            }
            
            // 复制sid
            if (headers.containsKey("sid")) {
                ackHeaders.put("sid", headers.get("sid"));
            } else {
                ackHeaders.put("sid", "");
            }
            
            // 复制其他可选字段
            if (headers.containsKey("app-key")) {
                ackHeaders.put("app-key", headers.get("app-key"));
            }
            if (headers.containsKey("ua")) {
                ackHeaders.put("ua", headers.get("ua"));
            }
            if (headers.containsKey("dt")) {
                ackHeaders.put("dt", headers.get("dt"));
            }
            
            ack.put("headers", ackHeaders);
            
            // 发送ACK
            String ackJson = objectMapper.writeValueAsString(ack);
            send(ackJson);
            
        } catch (Exception e) {
            log.error("【账号{}】发送ACK失败: {}", accountId, e.getMessage(), e);
            // ACK发送失败不影响消息处理，只记录日志
        }
    }
    
    /**
     * 处理心跳响应
     * 参考Python的handle_heartbeat_response方法
     */
    private void handleHeartbeatResponse() {
        log.debug("【账号{}】收到心跳响应", accountId);
        
        // 触发心跳响应回调（更新心跳响应时间）
        if (onHeartbeatResponse != null) {
            try {
                onHeartbeatResponse.run();
            } catch (Exception e) {
                log.error("【账号{}】心跳响应回调执行失败", accountId, e);
            }
        }
        
        // 调用消息处理器
        if (messageHandler != null) {
            messageHandler.handleHeartbeat(accountId);
        }
    }



    @Override
    public void onClose(int code, String reason, boolean remote) {
        isConnected = false;
        String closeType = remote ? "服务器" : "客户端";
        log.info("{}WebSocket连接关闭 - 关闭方: {}, 代码: {}, 原因: {}", 
                logPrefix(), closeType, code, reason);
        
        // 关闭消息处理线程池
        if (messageExecutor != null && !messageExecutor.isShutdown()) {
            messageExecutor.shutdown();
            log.debug("{}消息处理线程池已关闭", logPrefix());
        }
    }

    @Override
    public void onError(Exception ex) {
        log.error("{}WebSocket发生错误", logPrefix(), ex);
        if (messageHandler != null) {
            messageHandler.handleError(accountId, ex);
        }
    }

    /**
     * 发送心跳消息
     * 参考Python的send_heartbeat方法
     */
    public void sendHeartbeat() {
        if (isConnected) {
            try {
                // 生成心跳消息（参考Python格式）
                String mid = com.feijimiao.xianyuassistant.utils.XianyuDeviceUtils.generateMid();
                String heartbeat = String.format("{\"lwp\":\"/!\",\"headers\":{\"mid\":\"%s\"}}", mid);
                send(heartbeat);
            } catch (Exception e) {
                log.error("【账号{}】发送心跳失败", accountId, e);
            }
        }
    }

    /**
     * 检查连接状态
     * 使用底层的连接状态，而不是标志位
     */
    public boolean isConnected() {
        // 使用底层WebSocket的连接状态
        return !isClosed() && isOpen();
    }
    
    /**
     * 发送消息
     * 参考Python的send_msg方法
     * 
     * @param cid 会话ID（可能带或不带@goofish后缀）
     * @param toId 接收方用户ID（可能带或不带@goofish后缀）
     * @param text 消息文本内容
     */
    public void sendMessage(String cid, String toId, String text) {
        if (!isConnected) {
            log.error("【账号{}】WebSocket未连接，无法发送消息", accountId);
            return;
        }
        
        try {
            // 移除可能存在的@goofish后缀，确保统一处理
            String cleanCid = cid.replace("@goofish", "");
            String cleanToId = toId.replace("@goofish", "");
            
            log.info("【账号{}】准备发送消息: cleanCid={}, cleanToId={}, text={}", 
                    accountId, cleanCid, cleanToId, text);
            
            // 构造消息内容
            Map<String, Object> textContent = new HashMap<>();
            textContent.put("contentType", 1);
            Map<String, String> textData = new HashMap<>();
            textData.put("text", text);
            textContent.put("text", textData);
            
            // Base64编码消息内容
            String textJson = objectMapper.writeValueAsString(textContent);
            String textBase64 = java.util.Base64.getEncoder().encodeToString(textJson.getBytes("UTF-8"));
            
            // 构造消息体
            Map<String, Object> messageBody = new HashMap<>();
            messageBody.put("uuid", generateUuid());
            messageBody.put("cid", cleanCid + "@goofish");
            messageBody.put("conversationType", 1);
            
            // 消息内容
            Map<String, Object> content = new HashMap<>();
            content.put("contentType", 101);
            Map<String, Object> custom = new HashMap<>();
            custom.put("type", 1);
            custom.put("data", textBase64);
            content.put("custom", custom);
            messageBody.put("content", content);
            
            messageBody.put("redPointPolicy", 0);
            
            // 扩展信息
            Map<String, String> extension = new HashMap<>();
            extension.put("extJson", "{}");
            messageBody.put("extension", extension);
            
            // 上下文信息
            Map<String, String> ctx = new HashMap<>();
            ctx.put("appVersion", "1.0");
            ctx.put("platform", "web");
            messageBody.put("ctx", ctx);
            
            messageBody.put("mtags", new HashMap<>());
            messageBody.put("msgReadStatusSetting", 1);
            
            // 接收者列表（参考Python: actualReceivers包含接收方和发送方）
            Map<String, Object> receivers = new HashMap<>();
            java.util.List<String> actualReceivers = new java.util.ArrayList<>();
            actualReceivers.add(cleanToId + "@goofish");
            // 使用myUserId而不是accountId
            String senderUserId = myUserId != null ? myUserId : accountId;
            actualReceivers.add(senderUserId + "@goofish");
            receivers.put("actualReceivers", actualReceivers);
            
            log.info("【账号{}】消息接收者列表: {}", accountId, actualReceivers);
            
            // 构造完整消息
            Map<String, Object> message = new HashMap<>();
            message.put("lwp", "/r/MessageSend/sendByReceiverScope");
            
            Map<String, String> headers = new HashMap<>();
            headers.put("mid", generateMid());
            if (sessionId != null) {
                headers.put("sid", sessionId);
            }
            message.put("headers", headers);
            
            java.util.List<Object> body = new java.util.ArrayList<>();
            body.add(messageBody);
            body.add(receivers);
            message.put("body", body);
            
            // 发送消息
            String messageJson = objectMapper.writeValueAsString(message);
            log.debug("【账号{}】发送消息JSON: {}", accountId, messageJson);
            send(messageJson);
            log.info("【账号{}】✅ 消息已发送到WebSocket", accountId);
            
        } catch (Exception e) {
            log.error("【账号{}】❌ 发送消息失败: cid={}, toId={}", accountId, cid, toId, e);
        }
    }
    
    /**
     * 生成消息ID (mid)
     * 格式: 随机数(0-999) + 时间戳(毫秒) + " 0"
     * 参考Python的generate_mid方法
     */
    private String generateMid() {
        return com.feijimiao.xianyuassistant.utils.XianyuDeviceUtils.generateMid();
    }
    
    /**
     * 生成UUID
     * 格式: -时间戳1
     * 参考Python的generate_uuid方法
     */
    private String generateUuid() {
        return com.feijimiao.xianyuassistant.utils.XianyuDeviceUtils.generateUuid();
    }
}
