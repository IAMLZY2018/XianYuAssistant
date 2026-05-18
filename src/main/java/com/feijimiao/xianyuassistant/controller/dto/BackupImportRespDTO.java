package com.feijimiao.xianyuassistant.controller.dto;

import lombok.Data;

import java.util.List;

@Data
public class BackupImportRespDTO {
    private int totalCount;
    private int successCount;
    private List<String> failedModules;
}
