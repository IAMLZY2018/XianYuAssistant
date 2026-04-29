package com.feijimiao.xianyuassistant.controller.dto;

import lombok.Data;

/**
 * 更新自动回复配置请求DTO
 * @author IAMLZY
 * @date 2026/4/22
 */
@Data
public class UpdateRagAutoReplyConfigReqDTO {
    private Long xianyuAccountId;  
    private String xyGoodsId;
    /** 回复延时秒数 */
    private Integer ragDelaySeconds;
}
