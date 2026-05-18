package com.feijimiao.xianyuassistant.controller.dto;

import lombok.Data;

/**
 * 订单查询请求DTO
 */
@Data
public class OrderQueryReqDTO {
    /**
     * 闲鱼账号ID（可选）
     */
    private Long xianyuAccountId;
    
    /**
     * 商品ID（可选，用于按商品过滤）
     */
    private String xyGoodsId;
    
    /**
     * 订单状态（可选）
     */
    private Integer orderStatus;
    
    /**
     * 页码（默认1）
     */
    private Integer pageNum = 1;
    
    /**
     * 每页数量（默认20）
     */
    private Integer pageSize = 20;
}
