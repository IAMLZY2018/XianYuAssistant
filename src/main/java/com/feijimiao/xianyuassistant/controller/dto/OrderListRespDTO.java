package com.feijimiao.xianyuassistant.controller.dto;

import lombok.Data;
import java.util.List;

/**
 * 订单列表响应DTO（第三方调用）
 */
@Data
public class OrderListRespDTO {

    private List<OrderDTO> records;

    private Long total;

    private Integer pageNum;

    private Integer pageSize;
}
