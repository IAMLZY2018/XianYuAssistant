package com.feijimiao.xianyuassistant.controller.dto;

import lombok.Data;

/**
 * 确认发货请求DTO
 */
@Data
public class ConfirmShipmentReqDTO {
    private Long xianyuAccountId;  // 账号ID
    private String orderId;         // 订单ID
}
    