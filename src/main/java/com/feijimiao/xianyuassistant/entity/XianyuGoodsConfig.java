package com.feijimiao.xianyuassistant.entity;

import lombok.Data;

/**
 * 商品配置实体类
 */
@Data
public class XianyuGoodsConfig {
    
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
     * 自动发货开关：1-开启，0-关闭，默认关闭
     */
    private Integer xianyuAutoDeliveryOn;
    
    /**
     * 自动回复开关：1-开启，0-关闭，默认关闭
     */
    private Integer xianyuAutoReplyOn;
    
    /**
     * 携带上下文开关：1-开启，0-关闭，默认开启，跟随自动回复开关
     */
    private Integer xianyuAutoReplyContextOn;
    
    /**
     * 创建时间
     */
    private String createTime;
    
    /**
     * 更新时间
     */
    private String updateTime;
}
