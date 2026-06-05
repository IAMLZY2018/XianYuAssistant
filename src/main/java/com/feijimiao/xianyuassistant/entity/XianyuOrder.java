package com.feijimiao.xianyuassistant.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 闲鱼订单实体
 */
@Data
public class XianyuOrder {
    
    private Long id;
    
    // 关联信息
    private Long xianyuAccountId;       // 关联的闲鱼账号ID
    
    // 订单基本信息
    private String orderId;             // 订单ID，如：4502252017189032029
    private String xyGoodsId;           // 闲鱼商品ID
    private String goodsTitle;          // 商品标题
    
    // 交易双方信息
    private String buyerUserId;         // 买家用户ID
    private String buyerUserName;       // 买家用户名
    private String sellerUserId;        // 卖家用户ID（通常是账号自己的unb）
    private String sellerUserName;      // 卖家用户名
    
    // 订单状态信息
    private Integer orderStatus;        // 订单状态：1-待付款，2-待发货，3-已发货，4-已完成，5-已取消
    private String orderStatusText;     // 订单状态文本：等待买家付款、等待卖家发货、交易成功等
    
    // 订单金额信息
    private Long orderAmount;           // 订单金额（单位：分）
    private String orderAmountText;     // 订单金额文本
    
    // 关联消息信息
    private String pnmId;               // 关联的消息pnmid
    private String sId;                 // 关联的会话ID
    private String reminderUrl;         // 消息链接
    
    // 时间信息
    private Long orderCreateTime;       // 订单创建时间戳（毫秒）
    private Long orderPayTime;          // 订单支付时间戳（毫秒）
    private Long orderDeliveryTime;     // 订单发货时间戳（毫秒）
    private Long orderCompleteTime;     // 订单完成时间戳（毫秒）
    private LocalDateTime createTime;   // 记录创建时间
    private LocalDateTime updateTime;   // 记录更新时间
    
    // 扩展信息
    private String completeMsg;         // 完整的消息体JSON（用于调试和后续分析）
}
