package com.feijimiao.xianyuassistant.service.bo;

import lombok.Data;

/**
 * 触发自动发货请求BO
 */
@Data
public class TriggerAutoDeliveryReqBO {
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
