package com.feijimiao.xianyuassistant.entity;

import lombok.Data;

/**
 * 商品自动回复记录实体类
 */
@Data
public class XianyuGoodsAutoReplyRecord {
    
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 闲鱼账号ID
     */
    private Long xianyuAccountId;
    
    /**
     * 本地闲鱼商品ID
     */
    private Long xianyuGoodsId;
    
    /**
     * 闲鱼的商品ID
     */
    private String xyGoodsId;
    
    /**
     * 会话ID（用于延时任务去重）
     */
    private String sId;
    
    /**
     * 触发回复的消息pnmId
     */
    private String pnmId;
    
    /**
     * 买家用户ID
     */
    private String buyerUserId;
    
    /**
     * 买家用户名
     */
    private String buyerUserName;
    
    /**
     * 买家消息内容
     */
    private String buyerMessage;
    
    /**
     * 回复消息内容
     */
    private String replyContent;
    
    /**
     * 回复类型：1-关键词匹配，2-RAG智能回复
     */
    private Integer replyType;
    
    /**
     * 匹配的关键词
     */
    private String matchedKeyword;
    
    /**
     * 触发上下文JSON（包含触发消息列表和RAG命中资料列表）
     */
    private String triggerContext;
    
    /**
     * 状态：0-待回复，1-成功，-1-失败
     */
    private Integer state;
    
    /**
     * 创建时间
     */
    private String createTime;
}
