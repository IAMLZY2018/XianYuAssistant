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
     * 发货模式：1-自动发货，2-卡密发货，3-自定义发货
     */
    private Integer deliveryMode = 1;

    /**
     * 自动发货的文本内容
     */
    private String autoDeliveryContent;

    /**
     * 卡密发货：绑定的卡密配置ID列表（逗号分隔）
     */
    private String kamiConfigIds;

    /**
     * 卡密发货文案模板，使用{kmKey}占位符替换卡密内容
     */
    private String kamiDeliveryTemplate;

    /**
     * 自动发货图片URL
     */
    private String autoDeliveryImageUrl;
    
    /**
     * 自动确认发货开关：0-关闭，1-开启
     */
    private Integer autoConfirmShipment;
}