package com.feijimiao.xianyuassistant.service;

import com.feijimiao.xianyuassistant.event.chatMessageEvent.ChatMessageData;

import java.util.List;

/**
 * 自动回复服务接口
 * 
 * @author IAMLZY
 * @date 2026/4/22
 */
public interface AutoReplyService {
    
    /**
     * 执行自动回复（单条消息）
     * 
     * @param messageData 消息数据
     */
    void executeAutoReply(ChatMessageData messageData);
    
    /**
     * 执行自动回复（多条消息，延时期间收集）
     * 
     * @param messageList 触发消息列表
     */
    void executeAutoReply(List<ChatMessageData> messageList);
    
    /**
     * 检查商品是否开启自动回复
     * 
     * @param accountId 账号ID
     * @param xyGoodsId 商品ID
     * @return 是否开启
     */
    boolean isAutoReplyEnabled(Long accountId, String xyGoodsId);
}
