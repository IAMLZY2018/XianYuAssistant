package com.feijimiao.xianyuassistant.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feijimiao.xianyuassistant.utils.AccountDisplayNameUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * WebSocket初始化器
 * 参考Python代码的init方法
 * 负责在WebSocket连接建立后发送必要的注册和同步消息
 */
@Slf4j
@Component
public class WebSocketInitializer {

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    private AccountDisplayNameUtils displayNameUtils;
    
    /**
     * 生成消息ID
     * 参考Python的generate_mid方法
     * 格式: 随机数(0-999) + 时间戳(毫秒) + " 0"
     */
    private String generateMid() {
        return com.feijimiao.xianyuassistant.utils.XianyuDeviceUtils.generateMid();
    }
    
    /**
     * 获取账号显示名称
     */
    private String getDisplayName(String accountId) {
        return displayNameUtils.getDisplayName(accountId);
    }
    
    /**
     * 格式化日志前缀
     */
    private String logPrefix(String accountId) {
        return "【" + getDisplayName(accountId) + "】";
    }
    
    /**
     * 发送注册消息
     * 参考Python的init方法中的/reg消息
     * 
     * @param client WebSocket客户端
     * @param token accessToken
     * @param deviceId 设备ID
     * @param accountId 账号ID
     */
    public void sendRegistrationMessage(XianyuWebSocketClient client, String token, String deviceId, String accountId) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("lwp", "/reg");
            
            Map<String, Object> headers = new HashMap<>();
            headers.put("cache-header", "app-key token ua wv");
            headers.put("app-key", "444e9908a51d1cb236a27862abc769c9");
            headers.put("token", token);
            headers.put("ua", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36 DingTalk(2.1.5) OS(Windows/10) Browser(Chrome/133.0.0.0) DingWeb/2.1.5 IMPaaS DingWeb/2.1.5");
            headers.put("dt", "j");
            headers.put("wv", "im:3,au:3,sy:6");
            headers.put("sync", "0,0;0;0;");
            headers.put("did", deviceId);
            headers.put("mid", generateMid());
            
            message.put("headers", headers);
            
            String jsonMessage = objectMapper.writeValueAsString(message);
            client.send(jsonMessage);
            
            log.info("{}已发送注册消息", logPrefix(accountId));
            log.info("{}注册消息内容: {}", logPrefix(accountId), jsonMessage);
            
        } catch (Exception e) {
            log.error("{}发送注册消息失败", logPrefix(accountId), e);
        }
    }
    
    /**
     * 发送同步状态消息
     * 参考Python的init方法中的/r/SyncStatus/ackDiff消息
     * 
     * @param client WebSocket客户端
     * @param accountId 账号ID
     */
    public void sendSyncStatusMessage(XianyuWebSocketClient client, String accountId) {
        try {
            long currentTime = System.currentTimeMillis();
            
            Map<String, Object> message = new HashMap<>();
            message.put("lwp", "/r/SyncStatus/ackDiff");
            
            Map<String, Object> headers = new HashMap<>();
            headers.put("mid", generateMid());
            message.put("headers", headers);
            
            Map<String, Object> bodyItem = new HashMap<>();
            bodyItem.put("pipeline", "sync");
            bodyItem.put("tooLong2Tag", "PNM,1");
            bodyItem.put("channel", "sync");
            bodyItem.put("topic", "sync");
            bodyItem.put("highPts", 0);
            // 重要：pts必须设置为当前时间戳*1000（参考Python实现）
            // Python: pts = current_time * 1000 (毫秒时间戳 * 1000)
            bodyItem.put("pts", currentTime * 1000);
            bodyItem.put("seq", 0);
            bodyItem.put("timestamp", currentTime);
            
            message.put("body", new Object[]{bodyItem});
            
            String jsonMessage = objectMapper.writeValueAsString(message);
            client.send(jsonMessage);
            
            log.info("{}已发送同步状态消息", logPrefix(accountId));
            log.info("{}同步状态消息内容: {}", logPrefix(accountId), jsonMessage);
            log.info("{}pts设置为{} (currentTime * 1000)", logPrefix(accountId), currentTime * 1000);
            
        } catch (Exception e) {
            log.error("{}发送同步状态消息失败", logPrefix(accountId), e);
        }
    }
    
    /**
     * 完整的初始化流程
     * 
     * @param client WebSocket客户端
     * @param token accessToken
     * @param deviceId 设备ID
     * @param accountId 账号ID
     */
    public void initialize(XianyuWebSocketClient client, String token, String deviceId, String accountId) {
        log.info("{}开始WebSocket初始化流程...", logPrefix(accountId));
        log.info("{}设备ID: {}", logPrefix(accountId), deviceId);
        log.info("{}Token长度: {}", logPrefix(accountId), token != null ? token.length() : 0);
        
        // 1. 发送注册消息
        sendRegistrationMessage(client, token, deviceId, accountId);
        
        // 2. 等待1秒（参考Python代码）
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 3. 发送同步状态消息
        sendSyncStatusMessage(client, accountId);
        
        log.info("{}WebSocket初始化流程完成", logPrefix(accountId));
    }
}
