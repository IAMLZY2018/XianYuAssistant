package com.feijimiao.xianyuassistant.service;

/**
 * 增强的消息发送服务
 * 参考Python代码的消息发送逻辑
 */
public interface EnhancedMessageSendService {
    
    /**
     * 发送消息（带重试机制）
     * 
     * @param accountId 账号ID
     * @param chatId 会话ID
     * @param toUserId 接收方用户ID
     * @param message 消息内容
     * @return 发送结果
     */
    MessageSendResult sendMessageWithRetry(Long accountId, String chatId, String toUserId, String message);
    
    /**
     * 发送消息（带重试机制和人工延迟）
     * 
     * @param accountId 账号ID
     * @param chatId 会话ID
     * @param toUserId 接收方用户ID
     * @param message 消息内容
     * @param simulateHumanDelay 是否模拟人工延迟
     * @return 发送结果
     */
    MessageSendResult sendMessageWithRetry(Long accountId, String chatId, String toUserId, String message, boolean simulateHumanDelay);
    
    /**
     * 消息发送结果
     */
    enum MessageSendResult {
        /**
         * 发送成功
         */
        SUCCESS,
        
        /**
         * 发送失败
         */
        FAILED,
        
        /**
         * WebSocket未连接
         */
        NOT_CONNECTED,
        
        /**
         * 重试次数耗尽
         */
        RETRY_EXHAUSTED
    }
}
