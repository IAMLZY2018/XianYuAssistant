package com.feijimiao.xianyuassistant.controller.dto;

import lombok.Data;

/**
 * 自动发货记录DTO (简化版)
 */
@Data
public class AutoDeliveryRecordDTO {
    
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 闲鱼的商品ID
     */
    private String xyGoodsId;
    
    /**
     * 商品标题
     */
    private String goodsTitle;
    
    /**
     * 买家用户名称
     */
    private String buyerUserName;
    
    /**
     * 发货消息内容
     */
    private String content;
    
    /**
     * 发货是否成功: 1-成功, 0-失败
     */
    private Integer state;
    
    /**
     * 订单ID
     */
    private String orderId;
    
    /**
     * 创建时间
     */
    private String createTime;
}
