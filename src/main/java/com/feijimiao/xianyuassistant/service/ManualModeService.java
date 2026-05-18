package com.feijimiao.xianyuassistant.service;

import java.util.Map;

/**
 * 人工接管模式服务
 * 参考Python代码的人工接管管理逻辑
 */
public interface ManualModeService {
    
    /**
     * 检查特定会话是否处于人工接管模式
     * 
     * @param chatId 会话ID
     * @return true=人工模式，false=自动模式
     */
    boolean isManualMode(String chatId);
    
    /**
     * 切换人工接管模式
     * 
     * @param chatId 会话ID
     * @return 切换后的模式："manual" 或 "auto"
     */
    String toggleManualMode(String chatId);
    
    /**
     * 进入人工接管模式
     * 
     * @param chatId 会话ID
     */
    void enterManualMode(String chatId);
    
    /**
     * 退出人工接管模式
     * 
     * @param chatId 会话ID
     */
    void exitManualMode(String chatId);
    
    /**
     * 获取所有人工接管模式的会话
     * 
     * @return 会话ID和时间戳的映射
     */
    Map<String, Long> getAllManualModeConversations();
    
    /**
     * 清理超时的人工接管模式会话
     * 
     * @return 清理的会话数量
     */
    int cleanupExpiredManualModes();
}
