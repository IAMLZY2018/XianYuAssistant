package com.feijimiao.xianyuassistant.controller.dto;

import lombok.Data;

import java.util.List;

@Data
public class RefreshItemsRespDTO {
    private Boolean success;
    private Integer totalCount;
    private Integer successCount;
    private List<String> updatedItemIds;
    private String message;
    private String syncId;
}
