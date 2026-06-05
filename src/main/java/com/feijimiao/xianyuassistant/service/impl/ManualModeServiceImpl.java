package com.feijimiao.xianyuassistant.service.impl;

import com.feijimiao.xianyuassistant.config.WebSocketConfig;
import com.feijimiao.xianyuassistant.service.ManualModeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 人工接管模式服务实现
 * 参考Python代码的人工接管管理逻辑
 */
@Slf4j
@Service
public class ManualModeServiceImpl implements ManualModeService {

    @Autowired
    private WebSocketConfig config;
    
    // 人工接管模式的会话集合
    private final Map<String, Long> manualModeConversations = new ConcurrentHashMap<>();
    
    @Override
    public boolean isManualMode(String chatId) {
        if (!manualModeConversations.containsKey(chatId)) {
            return false;
        }
        
        // 检查是否超时
        Long timestamp = manualModeConversations.get(chatId);
        if (timestamp == null) {
            return false;
        }
        
        long currentTime = System.currentTimeMillis() / 1000;
        long timeout = config.getManualModeTimeout();
        
        if (currentTime - timestamp > timeout) {
            // 超时，自动退出人工模式
            exitManualMode(chatId);
            return false;
        }
        
        return true;
    }
    
    @Override
    public String toggleManualMode(String chatId) {
        if (isManualMode(chatId)) {
            exitManualMode(chatId);
            return "auto";
        } else {
            enterManualMode(chatId);
            return "manual";
        }
    }
    
    @Override
    public void enterManualMode(String chatId) {
        long currentTime = System.currentTimeMillis() / 1000;
        manualModeConversations.put(chatId, currentTime);
        log.info("✅ 进入人工接管模式: chatId={}", chatId);
    }
    
    @Override
    public void exitManualMode(String chatId) {
        manualModeConversations.remove(chatId);
        log.info("✅ 退出人工接管模式: chatId={}", chatId);
    }
    
    @Override
    public Map<String, Long> getAllManualModeConversations() {
        return new ConcurrentHashMap<>(manualModeConversations);
    }
    
    @Override
    public int cleanupExpiredManualModes() {
        int count = 0;
        long currentTime = System.currentTimeMillis() / 1000;
        long timeout = config.getManualModeTimeout();
        
        for (Map.Entry<String, Long> entry : manualModeConversations.entrySet()) {
            if (currentTime - entry.getValue() > timeout) {
                exitManualMode(entry.getKey());
                count++;
            }
        }
        
        if (count > 0) {
            log.info("清理超时人工接管模式会话: {}个", count);
        }
        
        return count;
    }
    
    /**
     * 定时清理超时的人工接管模式会话
     * 每5分钟执行一次
     */
    @Scheduled(fixedRate = 300000)
    public void scheduledCleanup() {
        cleanupExpiredManualModes();
    }
}
