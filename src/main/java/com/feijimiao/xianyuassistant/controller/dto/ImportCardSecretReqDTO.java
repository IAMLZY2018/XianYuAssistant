package com.feijimiao.xianyuassistant.controller.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 导入卡密请求DTO
 */
@Data
public class ImportCardSecretReqDTO {
    @NotNull(message = "账号ID不能为空")
    private Long xianyuAccountId;
    
    @NotBlank(message = "商品ID不能为空")
    private String xyGoodsId;
    
    @NotBlank(message = "卡密内容不能为空")
    private String content; // 换行分割
}
