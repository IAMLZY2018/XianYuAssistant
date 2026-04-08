package com.feijimiao.xianyuassistant.entity;

import lombok.Data;

/**
 * 商品自动发货记录实体类
 * 优化后: 移除重复字段,通过关联xianyu_order表获取订单信息
 */
@Data
public class XianyuGoodsAutoDeliveryRecord {
    
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
     * 消息pnmid，用于防止重复发货
     */
    private String pnmId;
    
    /**
     * 订单ID (关联xianyu_order表)
     */
    private String orderId;
    
    /**
     * 发货消息内容
     */
    private String content;
    
    /**
     * 发货是否成功: 1-成功, 0-失败
     */
    private Integer state;
    
    /**
     * 创建时间
     */
    private String createTime;
    
    // ========== 以下为关联查询字段 (非数据库字段) ==========
    
    /**
     * 买家用户ID (关联查询字段)
     */
    private String buyerUserId;
    
    /**
     * 买家用户名称 (关联查询字段)
     */
    private String buyerUserName;
    
    /**
     * 商品标题 (关联查询字段)
     */
    private String goodsTitle;
    
    /**
     * 订单状态 (关联查询字段, 来自xianyu_order.order_status)
     * 1-待付款, 2-待发货, 3-已发货, 4-已完成, 5-已取消
     */
    private Integer orderStatus;
    
    /**
     * 订单状态文本 (关联查询字段)
     */
    private String orderStatusText;
}
