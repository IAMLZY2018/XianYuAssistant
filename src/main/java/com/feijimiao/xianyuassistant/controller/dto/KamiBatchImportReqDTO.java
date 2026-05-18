package com.feijimiao.xianyuassistant.controller.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
public class KamiBatchImportReqDTO {

    @NotNull(message = "卡密配置ID不能为空")
    private Long kamiConfigId;

    private String kamiContents;
}
