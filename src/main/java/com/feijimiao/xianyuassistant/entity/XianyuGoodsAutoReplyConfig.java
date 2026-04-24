package com.feijimiao.xianyuassistant.entity;

import lombok.Data;

/**
 * 商品自动回复配置实体类
 */
@Data
public class XianyuGoodsAutoReplyConfig {
    
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
     * 关键词（支持多个，用逗号分隔）
     */
    private String keyword;
    
    /**
     * 回复内容
     */
    private String replyContent;
    
    /**
     * 匹配类型（1-包含，2-完全匹配，3-正则）
     */
    private Integer matchType;
    
    /**
     * 回复延时秒数（默认15秒）
     */
    private Integer ragDelaySeconds;
    
    /**
     * 创建时间
     */
    private String createTime;
    
    /**
     * 更新时间
     */
    private String updateTime;
}
