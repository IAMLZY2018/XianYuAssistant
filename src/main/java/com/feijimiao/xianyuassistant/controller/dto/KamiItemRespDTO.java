package com.feijimiao.xianyuassistant.controller.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KamiItemRespDTO {

    private Long id;

    private Long kamiConfigId;

    private String kamiContent;

    private Integer status;

    private String orderId;

    private LocalDateTime usedTime;

    private Integer sortOrder;

    private LocalDateTime createTime;
}
