package com.feijimiao.xianyuassistant.service.bo;

import lombok.Data;

import java.util.List;

/**
 * RAG回复结果
 * 包含AI回复内容和RAG命中的资料详情
 */
@Data
public class RAGReplyResult {
    
    /** AI回复内容 */
    private String replyContent;
    
    /** RAG命中的资料详情列表 */
    private List<RAGHitDetail> hitDetails;
    
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
