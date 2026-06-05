package com.feijimiao.xianyuassistant.controller.dto;

import lombok.Data;

/**
 * 订单列表请求DTO（第三方调用）
 */
@Data
public class OrderListReqDTO {

    private Long xianyuAccountId;

    private String xyGoodsId;

    /**
     * 订单状态（2=待发货, 1=已发货, -1=发货失败）
     * 不传则查询全部
     */
    private Integer orderStatus;

    /**
     * 模糊搜索关键词，匹配商品名称、规格、买家、发货内容
     */
    private String keyword;

    private Integer pageNum = 1;

    private Integer pageSize = 20;
}
