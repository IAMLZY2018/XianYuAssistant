package com.feijimiao.xianyuassistant.controller.dto;

import lombok.Data;

@Data
public class SyncProgressRespDTO {
    private String syncId;
    private Long accountId;
    private Integer totalCount;
    private Integer completedCount;
    private Integer successCount;
    private Integer failedCount;
    private Boolean isCompleted;
    private Boolean isRunning;
    private String currentItemId;
    private String message;
    private Long startTime;
    private Long estimatedRemainingTime;
}
