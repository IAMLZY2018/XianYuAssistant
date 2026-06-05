package com.feijimiao.xianyuassistant.controller.dto;

import lombok.Data;

@Data
public class KamiItemQueryReqDTO {
    
    private Long kamiConfigId;
    
    private Integer status;
    
    private String keyword;
}
