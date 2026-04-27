package com.feijimiao.xianyuassistant.controller.dto;

import lombok.Data;

/**
 * 卡密信息响应DTO
 */
@Data
public class CardSecretRespDTO {
    private Integer id;
    private Long xianyuAccountId;
    private String accountNote; // 账号备注
    private String xyGoodsId;
    private String goodsTitle;
    private String cardContent;
    private Integer isUsed;
    private String orderId;
    private String createTime;
    private String updateTime;
}
