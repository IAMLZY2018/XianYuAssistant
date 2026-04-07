package com.feijimiao.xianyuassistant.service.impl;

import com.feijimiao.xianyuassistant.config.WebSocketConfig;
import com.feijimiao.xianyuassistant.service.EnhancedMessageSendService;
import com.feijimiao.xianyuassistant.service.WebSocketService;
import com.feijimiao.xianyuassistant.websocket.XianyuWebSocketClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 增强的消息发送服务实现
 * 参考Python代码的消息发送逻辑
 * 
 * <p>增强功能：</p>
 * <ul>
 *   <li>消息发送重试机制</li>
 *   <li>模拟人工输入延迟</li>
 *   <li>消息发送统计</li>
 * </ul>
 */
@Slf4j
@Service
public class EnhancedMessageSendServiceImpl implements EnhancedMessageSendService {

    @Autowired
    private WebSocketService webSocketService;
    
    @Autowired
    private WebSocketConfig config;
    
    // 消息发送统计
    private final Map<Long, Long> messageSendCounts = new ConcurrentHashMap<>();
    private final Map<Long, Long> messageSendSuccessCounts = new ConcurrentHashMap<>();
    private final Map<Long, Long> messageSendFailCounts = new ConcurrentHashMap<>();
    
    // 随机数生成器
    private final Random random = new Random();

    @Override
    public MessageSendResult sendMessageWithRetry(Long accountId, String chatId, String toUserId, String message) {
        return sendMessageWithRetry(accountId, chatId, toUserId, message, config.isSimulateHumanTyping());
    }
    
    @Override
    public MessageSendResult sendMessageWithRetry(Long accountId, String chatId, String toUserId, String message, boolean simulateHumanDelay) {
        // 更新发送统计
        messageSendCounts.merge(accountId, 1L, Long::sum);
        
        // 检查WebSocket连接
        if (!webSocketService.isConnected(accountId)) {
            log.error("【账号{}】WebSocket未连接，无法发送消息", accountId);
            messageSendFailCounts.merge(accountId, 1L, Long::sum);
            return MessageSendResult.NOT_CONNECTED;
        }
        
        // 模拟人工延迟（参考Python的simulate_human_typing）
        if (simulateHumanDelay) {
            simulateHumanTypingDelay(message);
        }
        
        // 重试发送
        int maxAttempts = config.getMessageRetryAttempts();
        long retryDelay = config.getMessageRetryDelay();
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.info("【账号{}】发送消息 (尝试 {}/{}): chatId={}, toUserId={}, message={}", 
                        accountId, attempt, maxAttempts, chatId, toUserId, message);
                
                // 发送消息
                boolean success = webSocketService.sendMessage(accountId, chatId, toUserId, message);
                
                if (success) {
                    // 更新成功统计
                    messageSendSuccessCounts.merge(accountId, 1L, Long::sum);
                    log.info("【账号{}】✅ 消息发送成功: chatId={}, toUserId={}", accountId, chatId, toUserId);
                    return MessageSendResult.SUCCESS;
                } else {
                    log.warn("【账号{}】消息发送失败 (尝试 {}/{})", accountId, attempt, maxAttempts);
                    
                    // 如果不是最后一次尝试，等待后重试
                    if (attempt < maxAttempts) {
                        Thread.sleep(retryDelay);
                    }
                }
            } catch (InterruptedException e) {
                log.error("【账号{}】消息发送被中断", accountId, e);
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("【账号{}】消息发送异常 (尝试 {}/{})", accountId, attempt, maxAttempts, e);
                
                // 如果不是最后一次尝试，等待后重试
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        // 更新失败统计
        messageSendFailCounts.merge(accountId, 1L, Long::sum);
        log.error("【账号{}】❌ 消息发送失败，重试次数耗尽: chatId={}, toUserId={}", accountId, chatId, toUserId);
        return MessageSendResult.RETRY_EXHAUSTED;
    }
    
    /**
     * 模拟人工输入延迟
     * 参考Python的simulate_human_typing逻辑
     * 
     * @param message 消息内容
     */
    private void simulateHumanTypingDelay(String message) {
        try {
            // 基础延迟 0-1秒
            double baseDelay = random.nextDouble();
            
            // 每字 0.1-0.3秒
            double typingDelay = message.length() * (0.1 + random.nextDouble() * 0.2);
            
            // 总延迟
            double totalDelay = baseDelay + typingDelay;
            
            // 设置最大延迟上限，防止过长回复等待太久（参考Python的10秒上限）
            totalDelay = Math.min(totalDelay, 10.0);
            
            log.debug("模拟人工输入延迟: {}秒 (消息长度: {})", String.format("%.2f", totalDelay), message.length());
            
            Thread.sleep((long) (totalDelay * 1000));
            
        } catch (InterruptedException e) {
            log.warn("人工延迟被中断", e);
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 获取消息发送统计
     * 
     * @param accountId 账号ID
     * @return 统计信息数组 [总数, 成功数, 失败数]
     */
    public long[] getMessageSendStats(Long accountId) {
        return new long[] {
            messageSendCounts.getOrDefault(accountId, 0L),
            messageSendSuccessCounts.getOrDefault(accountId, 0L),
            messageSendFailCounts.getOrDefault(accountId, 0L)
        };
    }
    
    /**
     * 重置消息发送统计
     * 
     * @param accountId 账号ID
     */
    public void resetMessageSendStats(Long accountId) {
        messageSendCounts.remove(accountId);
        messageSendSuccessCounts.remove(accountId);
        messageSendFailCounts.remove(accountId);
    }
}
