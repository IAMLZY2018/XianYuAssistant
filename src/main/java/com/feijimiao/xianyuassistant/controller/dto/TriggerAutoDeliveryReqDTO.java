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
}
