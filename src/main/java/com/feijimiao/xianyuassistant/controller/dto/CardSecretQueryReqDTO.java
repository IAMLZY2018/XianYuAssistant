package com.feijimiao.xianyuassistant.controller.dto;

import lombok.Data;

/**
 * 卡密查询请求DTO
 */
@Data
public class CardSecretQueryReqDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private Long xianyuAccountId;
    private String xyGoodsId;
    private Integer isUsed;
    private String cardContent;
}
