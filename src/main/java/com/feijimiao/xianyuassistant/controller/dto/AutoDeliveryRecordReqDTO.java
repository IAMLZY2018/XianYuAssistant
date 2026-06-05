package com.feijimiao.xianyuassistant.controller.dto;

import lombok.Data;

/**
 * 获取自动发货记录请求DTO
 */
@Data
public class AutoDeliveryRecordReqDTO {
    
    /**
     * 闲鱼账号ID
     */
    private Long xianyuAccountId;
    
    /**
     * 商品ID（可选，不传则查询所有商品）
     */
    private String xyGoodsId;
    
    /**
     * 模糊搜索关键词，匹配商品名称、规格、买家、发货内容
     */
    private String keyword;
    
    /**
     * 页码
     */
    private Integer pageNum = 1;
    
    /**
     * 每页数量
     */
    private Integer pageSize = 20;
}
