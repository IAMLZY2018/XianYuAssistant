package com.feijimiao.xianyuassistant.controller.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class KamiItemReqDTO {

    @NotNull(message = "卡密配置ID不能为空")
    private Long kamiConfigId;

    @NotBlank(message = "卡密内容不能为空")
    private String kamiContent;
}
