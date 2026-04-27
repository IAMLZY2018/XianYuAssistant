package com.feijimiao.xianyuassistant.entity.bo;

import lombok.Data;

import java.util.List;

/**
 * 自动回复触发上下文
 * 保存触发本次自动回复的用户消息列表与RAG命中的详细资料列表
 * 以JSON形式存储在 xianyu_goods_auto_reply_record.trigger_context 字段
 */
@Data
public class AutoReplyTriggerContext {
    
    /**
     * 触发本次自动回复的用户消息列表
     * 延时期间用户可能发送多条消息，都收集在此
     */
    private List<TriggerMessage> triggerMessages;
    
    /**
     * RAG命中的详细资料列表
     * 从向量库搜索到的相关文档片段
     */
    private List<RAGHitDetail> ragHitDetails;
    
    /**
     * 携带的上下文消息
     * 开启携带上下文时，包含会话中的历史对话（格式：role: content）
     */
    private String contextMessages;
    
    /**
     * 触发消息
     */
    @Data
    public static class TriggerMessage {
        /** 消息pnmId */
        private String pnmId;
        /** 发送者用户ID */
        private String senderUserId;
        /** 发送者用户名 */
        private String senderUserName;
        /** 消息内容 */
        private String msgContent;
        /** 消息时间戳（毫秒） */
        private Long messageTime;
    }
    
    /**
     * RAG命中资料详情
     */
    @Data
    public static class RAGHitDetail {
        /** 文档ID */
        private String documentId;
        /** 命中的文本内容 */
        private String content;
        /** 相似度得分 */
        private Double score;
    }
}
