package com.feijimiao.xianyuassistant.controller.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * 自动发货配置请求DTO
 */
@Data
public class AutoDeliveryConfigReqDTO {
    
    /**
     * 闲鱼账号ID（必选）
     */
    @NotNull(message = "闲鱼账号ID不能为空")
    private Long xianyuAccountId;
    
    /**
     * 本地闲鱼商品ID
     */
    private Long xianyuGoodsId;
    
    /**
     * 闲鱼的商品ID（必选）
     */
    @NotNull(message = "闲鱼商品ID不能为空")
    private String xyGoodsId;
    
    /**
     * 发货类型（1-文本，2-自定义）
     */
    private Integer type = 1;
    
    /**
     * 自动发货的文本内容
     */
    private String autoDeliveryContent;
    
    /**
     * 自动确认发货开关：0-关闭，1-开启
     */
    private Integer autoConfirmShipment;
}