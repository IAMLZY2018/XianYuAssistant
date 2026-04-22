package com.feijimiao.xianyuassistant.service;

import com.feijimiao.xianyuassistant.event.chatMessageEvent.ChatMessageData;

/**
 * 自动回复服务接口
 * 
 * @author IAMLZY
 * @date 2026/4/22
 */
public interface AutoReplyService {
    
    /**
     * 执行RAG自动回复
     * 
     * <p>使用AIService的chatByRAG方法生成回复内容并发送</p>
     * 
     * @param messageData 消息数据
     */
    void executeRagAutoReply(ChatMessageData messageData);
    
    /**
     * 检查商品是否开启RAG自动回复
     * 
     * @param accountId 账号ID
     * @param xyGoodsId 商品ID
     * @return 是否开启
     */
    boolean isRagAutoReplyEnabled(Long accountId, String xyGoodsId);
}
