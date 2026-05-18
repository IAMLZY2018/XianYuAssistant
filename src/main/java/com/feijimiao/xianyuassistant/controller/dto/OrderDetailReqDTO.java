package com.feijimiao.xianyuassistant.controller.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
public class OrderDetailReqDTO {

    @NotNull(message = "闲鱼账号ID不能为空")
    private Long xianyuAccountId;

    @NotNull(message = "订单ID不能为空")
    private String orderId;

    private Boolean fromServer;
}
