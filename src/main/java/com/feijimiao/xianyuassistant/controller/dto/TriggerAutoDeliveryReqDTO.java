package com.feijimiao.xianyuassistant.controller.dto;

import lombok.Data;

/**
 * 触发自动发货请求DTO
 */
@Data
public class TriggerAutoDeliveryReqDTO {
    /**
     * 闲鱼账号ID
     */
    private Long xianyuAccountId;

    /**
     * 闲鱼商品ID
     */
    private String xyGoodsId;

    /**
     * 订单ID
     */
    private String orderId;
    
    /**
     * 是否需要模拟人工操作延迟
     * true: 需要延迟（模拟真人，避免被识别为机器人）
     * false: 不需要延迟（手动触发，立即发送）
     * 默认: false（手动触发场景）
     */
    private Boolean needHumanLikeDelay = false;
}
