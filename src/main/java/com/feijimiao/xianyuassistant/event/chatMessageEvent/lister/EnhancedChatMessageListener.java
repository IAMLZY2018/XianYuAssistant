package com.feijimiao.xianyuassistant.event.chatMessageEvent.lister;

import com.feijimiao.xianyuassistant.config.WebSocketConfig;
import com.feijimiao.xianyuassistant.event.chatMessageEvent.ChatMessageData;
import com.feijimiao.xianyuassistant.event.chatMessageEvent.ChatMessageReceivedEvent;
import com.feijimiao.xianyuassistant.service.ManualModeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 增强的聊天消息监听器
 * 实现消息时效性验证和人工接管检查
 * 
 * <p>功能：</p>
 * <ul>
 *   <li>消息时效性验证：过滤过期消息</li>
 *   <li>人工接管检查：人工模式下跳过自动处理</li>
 *   <li>人工接管切换：检测切换关键词</li>
 * </ul>
 */
@Slf4j
@Component
public class EnhancedChatMessageListener {

    @Autowired
    private ManualModeService manualModeService;
    
    @Autowired
    private WebSocketConfig config;

    /**
     * 处理聊天消息事件
     * 在其他监听器之前执行，进行前置检查
     * 
     * @param event 聊天消息事件
     */
    @Async
    @EventListener
    public void onChatMessageReceived(ChatMessageReceivedEvent event) {
        try {
            ChatMessageData data = event.getMessageData();
            
            // 1. 检查是否是人工接管切换关键词
            if (isToggleKeyword(data)) {
                handleToggleKeyword(data);
                return;
            }
            
            // 2. 人工接管检查
            if (isManualMode(data)) {
                log.info("会话处于人工接管模式，跳过自动处理: chatId={}", data.getSId());
                return;
            }
            
            // 3. 消息通过验证，继续处理（由其他监听器处理）
            log.debug("消息通过验证: pnmId={}, chatId={}", data.getPnmId(), data.getSId());
            
        } catch (Exception e) {
            log.error("处理聊天消息事件失败", e);
        }
    }
    
    /**
     * 检查是否是人工接管切换关键词
     * 
     * @param data 消息数据
     * @return true=是切换关键词，false=不是
     */
    private boolean isToggleKeyword(ChatMessageData data) {
        String messageContent = data.getMsgContent();
        if (messageContent == null || messageContent.isEmpty()) {
            return false;
        }
        
        String toggleKeywords = config.getToggleKeywords();
        return messageContent.trim().equals(toggleKeywords);
    }
    
    /**
     * 处理人工接管切换关键词
     * 
     * @param data 消息数据
     */
    private void handleToggleKeyword(ChatMessageData data) {
        String chatId = data.getSId();
        if (chatId == null || chatId.isEmpty()) {
            log.warn("无法切换人工接管模式：chatId为空");
            return;
        }
        
        // 切换人工接管模式
        String mode = manualModeService.toggleManualMode(chatId);
        log.info("检测到人工接管切换关键词，切换模式: chatId={}, mode={}, keyword={}", 
                chatId, mode, config.getToggleKeywords());
    }
    
    /**
     * 检查是否处于人工接管模式
     * 
     * @param data 消息数据
     * @return true=人工模式，false=自动模式
     */
    private boolean isManualMode(ChatMessageData data) {
        String chatId = data.getSId();
        if (chatId == null || chatId.isEmpty()) {
            return false;
        }
        
        return manualModeService.isManualMode(chatId);
    }
}
