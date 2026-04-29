package com.feijimiao.xianyuassistant.controller.dto;

import lombok.Data;

/**
 * 自动回复配置请求DTO
 * @author IAMLZY
 * @date 2026/4/22
 */
@Data
public class RagAutoReplyConfigReqDTO {
    private Long xianyuAccountId;
    private String xyGoodsId;
}
