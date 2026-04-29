package com.feijimiao.xianyuassistant.controller.dto;

import com.feijimiao.xianyuassistant.entity.XianyuGoodsInfo;
import lombok.Data;

/**
 * 商品信息（包含配置）DTO
 */
@Data
public class ItemWithConfigDTO {
    
    /**
     * 商品基本信息
     */
    private XianyuGoodsInfo item;
    
    /**
     * 自动发货开关：1-开启，0-关闭
     */
    private Integer xianyuAutoDeliveryOn;
    
    /**
     * 自动回复开关：1-开启，0-关闭
     */
    private Integer xianyuAutoReplyOn;
    
    /**
     * 携带上下文开关：1-开启，0-关闭
     */
    private Integer xianyuAutoReplyContextOn;
    
    /**
     * 自动发货类型（1-文本，2-自定义）
     */
    private Integer autoDeliveryType;
    
    /**
     * 自动发货内容
     */
    private String autoDeliveryContent;
}
