package com.feijimiao.xianyuassistant.controller.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * 自动发货配置查询请求DTO
 */
@Data
public class AutoDeliveryConfigQueryReqDTO {
    
    /**
     * 闲鱼账号ID（必选）
     */
    @NotNull(message = "闲鱼账号ID不能为空")
    private Long xianyuAccountId;
    
    /**
     * 闲鱼的商品ID（可选）
     */
    private String xyGoodsId;

    private String skuId;
}