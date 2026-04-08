package com.feijimiao.xianyuassistant.controller.vo;

import lombok.Data;

/**
 * 订单展示VO
 */
@Data
public class OrderVO {
    /**
     * 订单ID
     */
    private Long id;
    
    /**
     * 闲鱼账号备注
     */
    private String accountRemark;
    
    /**
     * 订单ID
     */
    private String orderId;
    
    /**
     * 商品名称
     */
    private String goodsTitle;
    
    /**
     * 会话ID（用于快速回复）
     */
    private String sId;
    
    /**
     * 创建时间
     */
    private Long createTime;
    
    /**
     * 是否自动发货成功
     */
    private Boolean autoDeliverySuccess;
    
    /**
     * 订单状态
     */
    private Integer orderStatus;
    
    /**
     * 订单状态文本
     */
    private String orderStatusText;
    
    /**
     * 买家用户名
     */
    private String buyerUserName;
    
    /**
     * 商品ID
     */
    private String xyGoodsId;
}
